/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.playback

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class CroixShakeDetector(
    context: Context,
    private val onShake: () -> Unit,
) : SensorEventListener {
    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var sensitivity: Float = 0.55f
    private var lastTriggerMs: Long = 0L
    private var registered: Boolean = false

    fun configure(enabled: Boolean, sensitivity: Float) {
        this.sensitivity = sensitivity.coerceIn(0.1f, 1.0f)
        if (enabled && !registered && accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            registered = true
        } else if (!enabled && registered) {
            sensorManager?.unregisterListener(this)
            registered = false
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val gx = event.values[0] / SensorManager.GRAVITY_EARTH
        val gy = event.values[1] / SensorManager.GRAVITY_EARTH
        val gz = event.values[2] / SensorManager.GRAVITY_EARTH
        val gForce = sqrt(gx * gx + gy * gy + gz * gz)
        val threshold = 3.2f - (sensitivity * 1.55f)
        val now = System.currentTimeMillis()
        if (gForce >= threshold && now - lastTriggerMs > 1_600L) {
            lastTriggerMs = now
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    fun release() {
        if (registered) {
            sensorManager?.unregisterListener(this)
            registered = false
        }
    }
}
