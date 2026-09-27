package com.example.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class TelemetryData(
    val stabilityScore: Int = 85, // 0 to 100
    val isTripodLocked: Boolean = false,
    val isSniperSteady: Boolean = false, // >= 85%
    val driftOffsetX: Float = 0f,
    val driftOffsetY: Float = 0f,
    val estimatedDistanceMeters: Float = 12.5f,
    val distanceDisplay: String = "12.5 m",
    val laserConfidence: Int = 94,
    val pitchAngle: Float = 0f
)

class SensorTelemetryManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val _telemetry = MutableStateFlow(TelemetryData())
    val telemetry: StateFlow<TelemetryData> = _telemetry.asStateFlow()

    private var lastAccX = 0f
    private var lastAccY = 0f
    private var lastAccZ = 9.8f
    private var smoothJitter = 0.1f

    private var currentZoom: Float = 1.0f

    fun startListening() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gyroscope?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    fun updateZoom(zoom: Float) {
        currentZoom = zoom
        updateTelemetryState()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]

                // calculate delta jitter
                val dX = abs(ax - lastAccX)
                val dY = abs(ay - lastAccY)
                val dZ = abs(az - lastAccZ)
                val rawJitter = sqrt(dX * dX + dY * dY + dZ * dZ)

                lastAccX = ax
                lastAccY = ay
                lastAccZ = az

                // low pass filter for jitter
                smoothJitter = 0.8f * smoothJitter + 0.2f * rawJitter

                updateTelemetryState()
            }
            Sensor.TYPE_GYROSCOPE -> {
                val gx = event.values[0]
                val gy = event.values[1]
                val gz = event.values[2]
                val gyroSpeed = sqrt(gx * gx + gy * gy + gz * gz)
                smoothJitter = 0.7f * smoothJitter + 0.3f * (gyroSpeed * 1.5f)
                updateTelemetryState()
            }
        }
    }

    private fun updateTelemetryState() {
        // High zoom amplifies shake perception, so stability calculation accounts for zoom level
        val zoomSensitivityFactor = 1.0f + (currentZoom / 10f) * 0.5f
        val effectiveShake = smoothJitter * zoomSensitivityFactor

        // Calculate stability from 0 to 100
        val stabilityRaw = (100f * exp(-effectiveShake * 1.8f)).roundToInt()
        val stability = stabilityRaw.coerceIn(5, 99)

        val isSteady = stability >= 82
        val isTripod = stability >= 94

        // Simulated AI Tripod lock drift offset (counter-shivering)
        val driftX = ((-lastAccX * 1.5f) * (1f - (stability / 100f))).coerceIn(-25f, 25f)
        val driftY = (((lastAccY - 4f) * 1.5f) * (1f - (stability / 100f))).coerceIn(-25f, 25f)

        // Estimated Distance calculation based on zoom magnification and perspective
        // At 1X: near 2m-10m; At 10X: 25m-150m; At 30X: 100m-800m
        val baseDist = when {
            currentZoom <= 1.0f -> 3.2f
            currentZoom <= 2.0f -> 7.8f
            currentZoom <= 5.0f -> 24.5f
            currentZoom <= 10.0f -> 68.0f
            currentZoom <= 20.0f -> 180.0f
            else -> 420.0f
        }
        val distanceDisplay = if (baseDist >= 1000f) {
            String.format("%.1f km", baseDist / 1000f)
        } else {
            String.format("%.1f m", baseDist)
        }

        val laserConf = (90 + (stability % 10)).coerceIn(88, 99)

        _telemetry.value = TelemetryData(
            stabilityScore = stability,
            isTripodLocked = isTripod,
            isSniperSteady = isSteady,
            driftOffsetX = driftX,
            driftOffsetY = driftY,
            estimatedDistanceMeters = baseDist,
            distanceDisplay = distanceDisplay,
            laserConfidence = laserConf,
            pitchAngle = lastAccY
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
