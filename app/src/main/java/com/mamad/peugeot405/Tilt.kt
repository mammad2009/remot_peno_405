package com.mamad.peugeot405

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * کج شدن گوشی به راست و چپ (roll) بر حسب درجه. مثبت = لبه‌ی راست گوشی پایین رفته.
 * (برای حالت عمودی یا افقی روی میز درست کار می‌کند)
 */
class TiltSensor(context: Context) : SensorEventListener {
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val acc: Sensor? = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var callback: ((Float) -> Unit)? = null
    private var smooth = 0f
    private var first = true

    val available: Boolean get() = acc != null

    fun start(onRoll: (Float) -> Unit) {
        callback = onRoll
        first = true
        acc?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        sm.unregisterListener(this)
        callback = null
    }

    override fun onSensorChanged(e: SensorEvent) {
        val ax = e.values[0].toDouble()
        val ay = e.values[1].toDouble()
        val az = e.values[2].toDouble()
        val roll = (-Math.toDegrees(Math.atan2(ax, Math.hypot(ay, az)))).toFloat()
        smooth = if (first) roll else smooth + 0.18f * (roll - smooth)
        first = false
        callback?.invoke(smooth)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
