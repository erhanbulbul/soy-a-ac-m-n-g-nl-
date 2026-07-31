package com.example.emergency

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Afet Durumunda Çalışan Arka Plan (Foreground) Servisi
 * 
 * Bu servis:
 * 1. Cihaz uykudayken veya ekran kapalıyken bile işlemcinin uyumasını engeller (WakeLock).
 * 2. Donanımsal Ses ve Hoparlör Ayarları: Hoparlör ve Mikrofonu son seste açar.
 * 3. BLE Advertising (S.O.S Sinyali): Eşleşmesiz BLE yayın yapar.
 * 4. Google Nearby Connections API (P2P_STAR Topolojisi): Otomatik tünel açar ve bağlantı taleplerini kullanıcı onayı olmadan kabul eder.
 */
class EmergencySosForegroundService : Service() {

    companion object {
        const val ACTION_START_EMERGENCY = "ACTION_START_EMERGENCY"
        const val ACTION_STOP_EMERGENCY = "ACTION_STOP_EMERGENCY"
        const val CHANNEL_ID = "EMERGENCY_SOS_SERVICE_CHANNEL"
        const val SERVICE_ID = "com.example.emergency.MESH_SOS"
        val SOS_UUID: UUID = UUID.fromString("0000FEAA-0000-1000-8000-00805F9B34FB")
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var bluetoothAdvertiser: BluetoothLeAdvertiser? = null
    private var audioManager: AudioManager? = null

    private var isAdvertisingNearby = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_EMERGENCY -> startEmergencyMode()
            ACTION_STOP_EMERGENCY -> stopEmergencyMode()
            else -> startEmergencyMode()
        }
        return START_STICKY
    }

    private fun startEmergencyMode() {
        Log.w("EmergencyService", "AFET DURUMU SERVİSİ BAŞLATILDI!")

        // 1. Foreground Notification başlat
        val notification = buildForegroundNotification()
        startForeground(1001, notification)

        // 2. CPU WakeLock Al (Ekran kapansa bile uyumasın)
        acquireWakeLock()

        // 3. Donanımsal Hoparlör ve Ses Ayarlarını Yap (Son Seste Aç)
        configureAudioHardware()

        // 4. BLE Advertising Yayınını Başlat
        startBleAdvertising()

        // 5. Google Nearby Connections API Advertising (Otomatik Kabul)
        startNearbyAdvertising()
    }

    private fun stopEmergencyMode() {
        Log.i("EmergencyService", "Afet durumu servisi durduruluyor...")
        stopBleAdvertising()
        stopNearbyAdvertising()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Donanımsal Hoparlör ve Mikrofonu Son Seste Açar
     */
    private fun configureAudioHardware() {
        try {
            audioManager?.let { am ->
                am.mode = AudioManager.MODE_IN_COMMUNICATION
                am.isSpeakerphoneOn = true

                // Tüm ses akışlarını maksimum seviyeye getir
                val maxMusic = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val maxVoice = am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                val maxAlarm = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val maxRing = am.getStreamMaxVolume(AudioManager.STREAM_RING)

                am.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0)
                am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVoice, 0)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarm, 0)
                am.setStreamVolume(AudioManager.STREAM_RING, maxRing, 0)

                Log.d("EmergencyService", "Hoparlör ve Mikrofon son seste aktif edildi.")
            }
        } catch (e: Exception) {
            Log.e("EmergencyService", "Audio ayarlanırken hata: ${e.message}")
        }
    }

    /**
     * BLE (Bluetooth Low Energy) S.O.S Sinyali Yayınlar
     */
    @SuppressLint("MissingPermission")
    private fun startBleAdvertising() {
        try {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter = bluetoothManager?.adapter

            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                Log.e("EmergencyService", "Bluetooth kapalı veya desteklenmiyor!")
                return
            }

            bluetoothAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
            if (bluetoothAdvertiser == null) {
                Log.e("EmergencyService", "Cihaz BLE Advertising desteğine sahip değil.")
                return
            }

            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .setTimeout(0)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(ParcelUuid(SOS_UUID))
                .build()

            bluetoothAdvertiser?.startAdvertising(settings, data, advertiseCallback)
            Log.d("EmergencyService", "BLE SOS Beacon yayını başlatıldı.")
        } catch (e: SecurityException) {
            Log.e("EmergencyService", "BLE İzin hatası: ${e.message}")
        } catch (e: Exception) {
            Log.e("EmergencyService", "BLE Yayın hatası: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopBleAdvertising() {
        try {
            bluetoothAdvertiser?.stopAdvertising(advertiseCallback)
            Log.d("EmergencyService", "BLE SOS Beacon yayını durduruldu.")
        } catch (e: Exception) {
            Log.e("EmergencyService", "BLE Durdurma hatası: ${e.message}")
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d("EmergencyService", "BLE Advertising BAŞARILI!")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e("EmergencyService", "BLE Advertising BAŞARISIZ: ErrorCode=$errorCode")
        }
    }

    /**
     * Google Nearby Connections API - P2P_STAR Topolojisinde Otomatik Kabul Modu
     */
    private fun startNearbyAdvertising() {
        try {
            val advertisingOptions = AdvertisingOptions.Builder()
                .setStrategy(Strategy.P2P_STAR)
                .build()

            val deviceName = Build.MODEL ?: "SOS_STATION"

            Nearby.getConnectionsClient(this).startAdvertising(
                deviceName,
                SERVICE_ID,
                connectionLifecycleCallback,
                advertisingOptions
            ).addOnSuccessListener {
                isAdvertisingNearby = true
                Log.d("EmergencyService", "Nearby Connections Advertising Başlatıldı ($deviceName)")
            }.addOnFailureListener { e ->
                Log.e("EmergencyService", "Nearby Advertising Başarısız: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("EmergencyService", "Nearby Istasyon başlatma hatası: ${e.message}")
        }
    }

    private fun stopNearbyAdvertising() {
        try {
            Nearby.getConnectionsClient(this).stopAdvertising()
            isAdvertisingNearby = false
            Log.d("EmergencyService", "Nearby Advertising durduruldu.")
        } catch (e: Exception) {
            Log.e("EmergencyService", "Nearby durdurma hatası: ${e.message}")
        }
    }

    /**
     * Bağlantı Taleplerini Kullanıcı Onayı Beklemeden OTOMATİK KABUL Etme
     */
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.w("EmergencyService", "Kurtarıcı Bağlantı İsteği Geldi! Otomatik Kabul Ediliyor. Endpoint: $endpointId (${connectionInfo.endpointName})")
            // OTOMATİK KABUL: Kullanıcı butonuna basmaya gerek yok
            Nearby.getConnectionsClient(applicationContext).acceptConnection(
                endpointId,
                payloadCallback
            )
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                Log.w("EmergencyService", "KURTARICI İLE BAGLANTI KURULDU! Tünel Aktif: $endpointId")
                configureAudioHardware()
                NearbyWalkieTalkieManager.onConnectionEstablished(endpointId, "Kurtarıcı Ekip")
                // AFET DURUMUNDAKİ KİŞİ İÇİN ELLER SERBEST MİKROFON AKIŞI OTOMATİK BAŞLAR
                NearbyWalkieTalkieManager.startHandsFreeAutoStreaming(applicationContext, endpointId)
            } else {
                Log.e("EmergencyService", "Kurtarıcı bağlantısı başarısız: ${result.status.statusCode}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.i("EmergencyService", "Kurtarıcı bağlantısı kesildi: $endpointId")
            NearbyWalkieTalkieManager.onConnectionTerminated(endpointId)
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            // Gelen Telsiz Ses Akışını Dinleme (NearbyWalkieTalkieManager veya AudioTrack ile işlenir)
            Log.d("EmergencyService", "Ses/Veri Paketi Alındı! Type: ${payload.type}")
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes()
                if (bytes != null) {
                    NearbyWalkieTalkieManager.playAudioStreamChunk(bytes)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "EmergencySosApp::WakeLockTag"
        ).apply {
            acquire(24 * 60 * 60 * 1000L) // 24 saat max
        }
        Log.d("EmergencyService", "CPU WakeLock alındı.")
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d("EmergencyService", "CPU WakeLock serbest bırakıldı.")
            }
        } catch (e: Exception) {
            Log.e("EmergencyService", "WakeLock serbest bırakma hatası: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Afet SOS Arka Plan Servisi",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Enkaz altı SOS sinyali ve Nearby mesh ağını canlı tutar."
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚨 AFET S.O.S MODU AKTİF")
            .setContentText("Cihaz BLE & Nearby ağında enkaz altı sinyali yayıyor. Hoparlör açıldı.")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()
    }

    override fun onDestroy() {
        stopEmergencyMode()
        super.onDestroy()
    }
}
