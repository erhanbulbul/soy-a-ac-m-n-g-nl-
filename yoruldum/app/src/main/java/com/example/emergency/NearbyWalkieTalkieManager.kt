package com.example.emergency

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NearbyStation(
    val endpointId: String,
    val name: String
)

/**
 * Nearby Connections ve İnternetsiz Ses Akışı (Bas-Konuş Telsiz) Sınıfı
 * 
 * Bu sınıf:
 * 1. Nearby Connections API Discovery (Kurtarıcı Modu) ile S.O.S istasyonlarını bulur.
 * 2. Cihaza tünel üzerinden Wi-Fi Direct / Bluetooth P2P bağlantısı kurar.
 * 3. AudioRecord (VOICE_COMMUNICATION) ile mikrofondan ham PCM ses verilerini okuyup Byte Stream (Payload) olarak iletir.
 * 4. Gelen ses paketlerini AudioTrack (USAGE_VOICE_COMMUNICATION) ile hoparlörden canlı sesli mesaj olarak çalar.
 */
class NearbyWalkieTalkieManager(private val context: Context) {

    companion object {
        private const val SAMPLE_RATE = 16000 // 16kHz Telsiz kalitesi
        private const val CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        private var audioTrack: AudioTrack? = null

        private val _globalConnectedEndpointId = MutableStateFlow<String?>(null)
        val globalConnectedEndpointId: StateFlow<String?> = _globalConnectedEndpointId.asStateFlow()

        private val _globalConnectedStationName = MutableStateFlow("")
        val globalConnectedStationName: StateFlow<String> = _globalConnectedStationName.asStateFlow()

        private val _globalIsConnected = MutableStateFlow(false)
        val globalIsConnected: StateFlow<Boolean> = _globalIsConnected.asStateFlow()

        private val _globalIsHandsFreeStreaming = MutableStateFlow(false)
        val globalIsHandsFreeStreaming: StateFlow<Boolean> = _globalIsHandsFreeStreaming.asStateFlow()

        private var globalAudioRecordJob: Job? = null
        private var globalAudioRecord: AudioRecord? = null
        private val globalScope = CoroutineScope(Dispatchers.IO)

        fun onConnectionEstablished(endpointId: String, stationName: String) {
            _globalConnectedEndpointId.value = endpointId
            _globalConnectedStationName.value = stationName
            _globalIsConnected.value = true
            Log.w("WalkieTalkie", "KÜRESEL TELSİZ BAĞLANTISI AKTİF! Target: $endpointId ($stationName)")
        }

        fun onConnectionTerminated(endpointId: String) {
            stopHandsFreeAutoStreaming()
            if (_globalConnectedEndpointId.value == endpointId) {
                _globalConnectedEndpointId.value = null
                _globalConnectedStationName.value = ""
                _globalIsConnected.value = false
                Log.i("WalkieTalkie", "KÜRESEL TELSİZ BAĞLANTISI KESİLDİ: $endpointId")
            }
        }

        /**
         * Afet Durumundaki Kişi İçin Otomatik Eller Serbest Mikrofon Akışı (Bas-Konuşsuz Canlı Mikrofon)
         */
        @SuppressLint("MissingPermission")
        fun startHandsFreeAutoStreaming(context: Context, targetEndpointId: String) {
            if (_globalIsHandsFreeStreaming.value) return
            _globalIsHandsFreeStreaming.value = true

            globalAudioRecordJob = globalScope.launch {
                try {
                    val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, AUDIO_FORMAT)
                    val bufferSize = maxOf(minBufferSize * 4, 4096)

                    globalAudioRecord = AudioRecord(
                        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                        SAMPLE_RATE,
                        CHANNEL_IN,
                        AUDIO_FORMAT,
                        bufferSize
                    )

                    globalAudioRecord?.startRecording()
                    Log.w("WalkieTalkie", "🚨 ELLER SERBEST MİKROFON YAYINI BAŞLADI (Enkaz Altı Canlı Dinleme -> $targetEndpointId)")

                    val buffer = ByteArray(1024)
                    while (_globalIsHandsFreeStreaming.value && globalAudioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        val readBytes = globalAudioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (readBytes > 0) {
                            val chunk = buffer.copyOf(readBytes)
                            val payload = Payload.fromBytes(chunk)
                            Nearby.getConnectionsClient(context).sendPayload(targetEndpointId, payload)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WalkieTalkie", "Eller serbest yayın hatası: ${e.message}")
                } finally {
                    stopHandsFreeAutoStreamingInternal()
                }
            }
        }

        fun stopHandsFreeAutoStreaming() {
            _globalIsHandsFreeStreaming.value = false
            globalAudioRecordJob?.cancel()
            globalAudioRecordJob = null
            stopHandsFreeAutoStreamingInternal()
        }

        private fun stopHandsFreeAutoStreamingInternal() {
            try {
                globalAudioRecord?.stop()
                globalAudioRecord?.release()
                globalAudioRecord = null
                Log.i("WalkieTalkie", "Eller serbest mikrofon yayını kapatıldı.")
            } catch (e: Exception) {
                Log.e("WalkieTalkie", "AudioRecord durdurma hatası: ${e.message}")
            }
        }

        /**
         * Gelen ses paketlerini hoparlörden canlı oynatır
         */
        @Synchronized
        fun playAudioStreamChunk(audioBytes: ByteArray) {
            try {
                if (audioTrack == null || audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                    val minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_OUT, AUDIO_FORMAT)
                    val bufferSize = maxOf(minBufferSize * 4, 4096)

                    audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val attributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                        val format = AudioFormat.Builder()
                            .setEncoding(AUDIO_FORMAT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(CHANNEL_OUT)
                            .build()
                        AudioTrack.Builder()
                            .setAudioAttributes(attributes)
                            .setAudioFormat(format)
                            .setBufferSizeInBytes(bufferSize)
                            .setTransferMode(AudioTrack.MODE_STREAM)
                            .build()
                    } else {
                        @Suppress("DEPRECATION")
                        AudioTrack(
                            AudioManager.STREAM_VOICE_CALL,
                            SAMPLE_RATE,
                            CHANNEL_OUT,
                            AUDIO_FORMAT,
                            bufferSize,
                            AudioTrack.MODE_STREAM
                        )
                    }
                    audioTrack?.play()
                }
                audioTrack?.write(audioBytes, 0, audioBytes.size)
            } catch (e: Exception) {
                Log.e("WalkieTalkie", "Ses oynatma hatası: ${e.message}")
            }
        }
    }

    private val _discoveredStations = MutableStateFlow<List<NearbyStation>>(emptyList())
    val discoveredStations: StateFlow<List<NearbyStation>> = _discoveredStations.asStateFlow()

    val isConnected: StateFlow<Boolean> = globalIsConnected
    val connectedStationName: StateFlow<String> = globalConnectedStationName

    private val _isTransmitting = MutableStateFlow(false)
    val isTransmitting: StateFlow<Boolean> = _isTransmitting.asStateFlow()

    private var isDiscovering = false

    private var audioRecordJob: Job? = null
    private var audioRecord: AudioRecord? = null

    private val scope = CoroutineScope(Dispatchers.IO)

    fun startDiscovery() {
        if (isDiscovering) return
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        Nearby.getConnectionsClient(context).startDiscovery(
            EmergencySosForegroundService.SERVICE_ID,
            endpointDiscoveryCallback,
            options
        ).addOnSuccessListener {
            isDiscovering = true
            Log.d("WalkieTalkie", "Nearby Discovery başlatıldı.")
        }.addOnFailureListener { e ->
            Log.e("WalkieTalkie", "Nearby Discovery başlatılamadı: ${e.message}")
        }
    }

    fun stopDiscovery() {
        try {
            Nearby.getConnectionsClient(context).stopDiscovery()
            isDiscovering = false
            Log.d("WalkieTalkie", "Nearby Discovery durduruldu.")
        } catch (e: Exception) {
            Log.e("WalkieTalkie", "Discovery durdurma hatası: ${e.message}")
        }
    }

    fun connectToStation(station: NearbyStation) {
        val userName = Build.MODEL ?: "Kurtarıcı Ekip"
        Nearby.getConnectionsClient(context).requestConnection(
            userName,
            station.endpointId,
            connectionLifecycleCallback
        ).addOnSuccessListener {
            Log.d("WalkieTalkie", "Bağlantı isteği gönderildi: ${station.name} (${station.endpointId})")
        }.addOnFailureListener { e ->
            Log.e("WalkieTalkie", "Bağlantı isteği atılamadı: ${e.message}")
        }
    }

    fun disconnect() {
        val id = globalConnectedEndpointId.value
        if (id != null) {
            Nearby.getConnectionsClient(context).disconnectFromEndpoint(id)
            onConnectionTerminated(id)
        }
        stopTransmitting()
    }

    /**
     * Bas-Konuş (Push-to-Talk) Basıldığında Mikrofonu Dinleyip Byte Stream Yollar
     */
    @SuppressLint("MissingPermission")
    fun startTransmitting() {
        val targetId = globalConnectedEndpointId.value
        if (targetId == null) {
            Log.e("WalkieTalkie", "Bağlı cihaz yok, ses gönderilemez!")
            return
        }

        if (_isTransmitting.value) return
        _isTransmitting.value = true

        audioRecordJob = scope.launch {
            try {
                val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, AUDIO_FORMAT)
                val bufferSize = maxOf(minBufferSize * 4, 4096)

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE,
                    CHANNEL_IN,
                    AUDIO_FORMAT,
                    bufferSize
                )

                audioRecord?.startRecording()
                Log.w("WalkieTalkie", "TELSİZ YAYINI BAŞLADI (Mikrofon Dinleniyor -> $targetId)")

                val buffer = ByteArray(1024)
                while (_isTransmitting.value && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readBytes > 0) {
                        val chunk = buffer.copyOf(readBytes)
                        val payload = Payload.fromBytes(chunk)
                        Nearby.getConnectionsClient(context).sendPayload(targetId, payload)
                    }
                }
            } catch (e: SecurityException) {
                Log.e("WalkieTalkie", "Mikrofon izin hatası: ${e.message}")
            } catch (e: Exception) {
                Log.e("WalkieTalkie", "Ses gönderme hatası: ${e.message}")
            } finally {
                stopTransmittingInternal()
            }
        }
    }

    /**
     * Bas-Konuş Bırakıldığında Mikrofonu Durdurup Dinleme Moduna Geçer
     */
    fun stopTransmitting() {
        _isTransmitting.value = false
        audioRecordJob?.cancel()
        audioRecordJob = null
        stopTransmittingInternal()
    }

    private fun stopTransmittingInternal() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.i("WalkieTalkie", "Telsiz yayını durduruldu. Dinleme moduna geçildi.")
        } catch (e: Exception) {
            Log.e("WalkieTalkie", "AudioRecord kapatma hatası: ${e.message}")
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.w("WalkieTalkie", "S.O.S İstasyonu Bulundu! ID: $endpointId (${info.endpointName})")
            val current = _discoveredStations.value.toMutableList()
            if (current.none { it.endpointId == endpointId }) {
                current.add(NearbyStation(endpointId, info.endpointName))
                _discoveredStations.value = current
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.i("WalkieTalkie", "İstasyon ağı kaybetti: $endpointId")
            val current = _discoveredStations.value.toMutableList()
            current.removeAll { it.endpointId == endpointId }
            _discoveredStations.value = current
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d("WalkieTalkie", "İstasyon ile bağlantı başlatılıyor: $endpointId (${connectionInfo.endpointName})")
            Nearby.getConnectionsClient(context).acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                val name = _discoveredStations.value.find { it.endpointId == endpointId }?.name ?: "S.O.S Cihazı"
                onConnectionEstablished(endpointId, name)
            } else {
                Log.e("WalkieTalkie", "Bağlantı başarısız: ${result.status.statusCode}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            onConnectionTerminated(endpointId)
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes()
                if (bytes != null) {
                    playAudioStreamChunk(bytes)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }
}
