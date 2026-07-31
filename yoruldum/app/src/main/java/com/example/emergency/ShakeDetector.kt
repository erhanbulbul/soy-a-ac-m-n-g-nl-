package com.example.emergency

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

/**
 * Otomatik Afet Tetikleme Sınıfı (İvmeölçer / SensorManager)
 * 
 * Bu sınıf telefonun donanımsal ivmeölçer (Accelerometer) sensörünü dinler.
 * Şiddetli bir sarsıntı (deprem / enkaz altı düşüş) algılandığında 5 saniye boyunca 
 * ivmeyi toplar. 5 saniye dolduğunda otomatik olarak "Afet Durumundayım" modunu tetikler.
 */
class ShakeDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var onEmergencyTriggered: (() -> Unit)? = null

    // Sarsıntı hesaplama değişkenleri
    private var shakeStartTimestamp: Long = 0L
    private var accumulatedShakeTimeMs: Long = 0L
    private val REQUIRED_SHAKE_DURATION_MS = 5000L // 5 saniye sarsıntı
    private val SHAKE_THRESHOLD_G = 2.4f // ~2.4G üzeri şiddetli sarsıntı eşiği

    private val _shakeProgress = MutableStateFlow(0f) // 0.0f - 1.0f arası UI ilerlemesi
    val shakeProgress: StateFlow<Float> = _shakeProgress.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private var hasTriggered = false

    fun start(onEmergency: () -> Unit) {
        if (accelerometer == null) {
            Log.e("ShakeDetector", "Cihazda ivmeölçer sensörü bulunamadı!")
            return
        }
        this.onEmergencyTriggered = onEmergency
        this.hasTriggered = false
        this.accumulatedShakeTimeMs = 0L
        _shakeProgress.value = 0f
        
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        _isMonitoring.value = true
        Log.d("ShakeDetector", "İvmeölçer sarsıntı takibi başlatıldı.")
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        _isMonitoring.value = false
        _shakeProgress.value = 0f
        accumulatedShakeTimeMs = 0L
        Log.d("ShakeDetector", "İvmeölçer sarsıntı takibi durduruldu.")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER || hasTriggered) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Toplam yerçekimi ivmesi (g-force) hesabı
        val gForce = sqrt((x * x + y * y + z * z).toDouble()).toFloat() / SensorManager.GRAVITY_EARTH

        val now = System.currentTimeMillis()

        if (gForce > SHAKE_THRESHOLD_G) {
            if (shakeStartTimestamp == 0L) {
                shakeStartTimestamp = now
            }
            val delta = now - shakeStartTimestamp
            accumulatedShakeTimeMs += delta
            shakeStartTimestamp = now

            val progress = (accumulatedShakeTimeMs.toFloat() / REQUIRED_SHAKE_DURATION_MS).coerceIn(0f, 1f)
            _shakeProgress.value = progress

            if (accumulatedShakeTimeMs >= REQUIRED_SHAKE_DURATION_MS) {
                hasTriggered = true
                Log.w("ShakeDetector", "5 Saniyelik Şiddetli Sarsıntı Algılandı! Afet Modu Otomatik Tetikleniyor!")
                onEmergencyTriggered?.invoke()
            }
        } else {
            // Sarsıntı kesildiyse kademeli olarak sıfırla (yanlış tetiklemeyi önleme)
            if (accumulatedShakeTimeMs > 0) {
                accumulatedShakeTimeMs = (accumulatedShakeTimeMs - 150).coerceAtLeast(0)
                _shakeProgress.value = (accumulatedShakeTimeMs.toFloat() / REQUIRED_SHAKE_DURATION_MS).coerceIn(0f, 1f)
            }
            shakeStartTimestamp = 0L
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
