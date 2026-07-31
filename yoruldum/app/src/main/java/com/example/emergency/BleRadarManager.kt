package com.example.emergency

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Radar Görselleştirme ve S.O.S Cihazı Veri Modeli
 */
data class DiscoveredBeacon(
    val id: String,                  // MAC adresi veya benzersiz ID
    val name: String,                // Cihaz adı (örn. S.O.S Cihazı #A41F)
    val rssi: Int,                   // Sinyal gücü (dBm)
    val distanceMeters: Double,      // Hesaplanan mesafe (Metre)
    val colorHex: String,            // Dinamik kırmızı renk tonu (Hex)
    val color: Color,                // Compose Color nesnesi
    val lastSeenMs: Long,            // Son görülme zamanı
    val radarAngle: Float,           // Radar üzerinde gösterim açısı (0° - 360°)
    val radarRadiusFactor: Float     // Radardaki yarıçap katsayısı (0.1 - 0.95)
)

/**
 * BLE Radar ve Sinyal Gücü (RSSI) Mesafe & Dinamik Renk Algoritması Sınıfı
 * 
 * Bu sınıf:
 * 1. Etraftaki BLE S.O.S sinyallerini tara.
 * 2. Formül ile RSSI değerini metreye çevirir.
 * 3. Yaklaştıkça (RSSI arttıkça) cihaz rengini Açık Kırmızıdan Koyu Kan Kırmızısına dönüştürür.
 */
class BleRadarManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var bluetoothScanner: BluetoothLeScanner? = null

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _beaconsMap = mutableMapOf<String, DiscoveredBeacon>()
    private val _smoothedRssiMap = mutableMapOf<String, Double>()
    private val _smoothedDistanceMap = mutableMapOf<String, Double>()

    private val _discoveredBeacons = MutableStateFlow<List<DiscoveredBeacon>>(emptyList())
    val discoveredBeacons: StateFlow<List<DiscoveredBeacon>> = _discoveredBeacons.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e("BleRadarManager", "Bluetooth kapalı!")
            return
        }

        try {
            bluetoothScanner = bluetoothAdapter.bluetoothLeScanner
            if (bluetoothScanner == null) {
                Log.e("BleRadarManager", "BLE Scanner alınamadı!")
                return
            }

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            val filters = listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(EmergencySosForegroundService.SOS_UUID))
                    .build()
            )

            bluetoothScanner?.startScan(filters, settings, scanCallback)
            _isScanning.value = true
            Log.d("BleRadarManager", "BLE Radar taraması başlatıldı.")
        } catch (e: SecurityException) {
            Log.e("BleRadarManager", "BLE Tarama İzin hatası: ${e.message}")
        } catch (e: Exception) {
            Log.e("BleRadarManager", "BLE Tarama başlatma hatası: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            bluetoothScanner?.stopScan(scanCallback)
            _isScanning.value = false
            Log.d("BleRadarManager", "BLE Radar taraması durduruldu.")
        } catch (e: Exception) {
            Log.e("BleRadarManager", "BLE Tarama durdurma hatası: ${e.message}")
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result == null) return
            processScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { processScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BleRadarManager", "BLE Tarama hatası: ErrorCode=$errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private fun processScanResult(result: ScanResult) {
        val scanRecord = result.scanRecord
        // Sadece uygulamayı kullanan S.O.S cihazlarını filtrele
        val serviceUuids = scanRecord?.serviceUuids
        val isAppSosDevice = serviceUuids?.any { it.uuid == EmergencySosForegroundService.SOS_UUID } == true
        if (!isAppSosDevice) {
            return
        }

        val device = result.device
        val address = device.address ?: "00:00:00:00:00:00"
        val rawRssi = result.rssi.toDouble()
        val name = try {
            scanRecord?.deviceName ?: device.name ?: "S.O.S Cihazı (${address.takeLast(4)})"
        } catch (e: SecurityException) {
            "S.O.S Cihazı (${address.takeLast(4)})"
        }

        // RSSI Üstel Hareketli Ortalama (EMA Filtresi - Sinyal Dalgalanmasını Önleme)
        val prevRssi = _smoothedRssiMap[address]
        val smoothedRssi = if (prevRssi == null) {
            rawRssi
        } else {
            // alpha = 0.15 (Sinyali yumuşatır, anlık sıçramaları engeller)
            0.15 * rawRssi + 0.85 * prevRssi
        }
        _smoothedRssiMap[address] = smoothedRssi
        val rssiInt = smoothedRssi.roundToInt()

        // 1. Matematiksel Formül: RSSI -> Metre Hesabı
        val rawDistance = calculateDistance(rssiInt)
        val prevDist = _smoothedDistanceMap[address]
        val smoothedDistance = if (prevDist == null) {
            rawDistance
        } else {
            0.15 * rawDistance + 0.85 * prevDist
        }
        _smoothedDistanceMap[address] = smoothedDistance

        // 2. Dinamik Renk Hesabı
        val (colorHex, composeColor) = calculateDynamicProximityColor(rssiInt, smoothedDistance)

        // Radar Gösterim Açısı: Canvas koordinatlarında 270° = Tam Üst (Ön Taraf / 12 Yönü)
        // Birden fazla cihaz varsa ön sektörde (-30° ile +30° yayılım / 240° - 300°) dağıtılır
        val deviceIndex = (address.hashCode() and 0x7FFFFFFF) % 5
        val angleOffset = (deviceIndex - 2) * 15f // -30°, -15°, 0°, +15°, +30°
        val frontAngle = (270f + angleOffset)

        // 0.1m ile 25m arası radar yarıçap oranlama katsayısı
        val radiusFactor = (smoothedDistance / 20.0).coerceIn(0.12, 0.92).toFloat()

        val beacon = DiscoveredBeacon(
            id = address,
            name = name,
            rssi = rssiInt,
            distanceMeters = (smoothedDistance * 10.0).roundToInt() / 10.0,
            colorHex = colorHex,
            color = composeColor,
            lastSeenMs = System.currentTimeMillis(),
            radarAngle = frontAngle,
            radarRadiusFactor = radiusFactor
        )

        _beaconsMap[address] = beacon
        cleanupOldBeacons()
        _discoveredBeacons.value = _beaconsMap.values.toList().sortedBy { it.distanceMeters }
    }

    /**
     * RSSI (dBm) Değerini Matematiksel Formül ile Metreye Çevirme
     * 
     * Formül: Distance = 10 ^ ((MeasuredPower - RSSI) / (10 * n))
     * - MeasuredPower: 1 metredeki varsayılan RSSI değeri (genelde -59 dBm)
     * - n: Çevresel sinyal sönümlenme katsayısı (2.0 - 4.0 arası, enkaz/bina için ~2.5 alınır)
     */
    fun calculateDistance(rssi: Int, txPowerAt1m: Int = -59, pathLossExponent: Double = 2.5): Double {
        if (rssi == 0) return -1.0
        val ratio = (txPowerAt1m - rssi) / (10.0 * pathLossExponent)
        return 10.0.pow(ratio)
    }

    /**
     * Dinamik Renk Algoritması (RSSI & Mesafeye Göre Renk Dönüşümü)
     * 
     * - Uzak (> 15 metre, RSSI <= -90 dBm): Açık Pembe / Kırmızı (#FFCDD2)
     * - Orta (5 - 15 metre, RSSI ~ -75 dBm): Mercan Kırmızısı (#EF5350)
     * - Yakın (2 - 5 metre, RSSI ~ -60 dBm): Canlı Kırmızı (#E53935)
     * - Çok Yakın / Enkaz Üstü (< 2 metre, RSSI >= -45 dBm): Koyu Kan Kırmızısı (#B71C1C)
     */
    private fun calculateDynamicProximityColor(rssi: Int, distance: Double): Pair<String, Color> {
        return when {
            rssi >= -50 || distance <= 2.0 -> Pair("#B71C1C", Color(0xFFB71C1C)) // Çok Yakın -> Koyu Kan Kırmızısı
            rssi >= -65 || distance <= 5.0 -> Pair("#D32F2F", Color(0xFFD32F2F)) // Yakın -> Koyu Kırmızı
            rssi >= -80 || distance <= 12.0 -> Pair("#E53935", Color(0xFFE53935)) // Orta -> Kırmızı
            rssi >= -90 || distance <= 20.0 -> Pair("#EF5350", Color(0xFFEF5350)) // Uzak -> Mercan Kırmızı
            else -> Pair("#FF8A80", Color(0xFFFF8A80))                          // Çok Uzak -> Açık Kırmızı / Pembe
        }
    }

    private fun cleanupOldBeacons() {
        val now = System.currentTimeMillis()
        val it = _beaconsMap.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            if (now - entry.value.lastSeenMs > 20000) { // 20 saniye görünmeyen cihazları temizle
                it.remove()
            }
        }
    }
}
