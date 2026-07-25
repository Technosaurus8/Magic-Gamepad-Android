package com.technosaurus.MagicGamepad.views
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.Surface
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import com.technosaurus.MagicGamepad.R
import com.technosaurus.MagicGamepad.util.SteeringWheelPrefs

class SteeringWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs), SensorEventListener {

    // Config - loaded from preferences
    var maxTiltDeg = 45f
    var maxWheelRotation = 180f
    /** lower the value higher the smoothness but delay increases.
    higher value less delay but less smooth. **/
    var smoothingFactor = 0.40f
    private var sensorDelayConstant = SensorManager.SENSOR_DELAY_GAME

    // Callback for your gamepad axis
    /**the smoothed angle is passed as normalized will always be between -1 to 1**/
    var onSteeringChanged: ((normalized: Float) -> Unit)? = null

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val prefs = context.getSharedPreferences(SteeringWheelPrefs.PREFERENCES_FILE, Context.MODE_PRIVATE)
    
    enum class SensorMode { AUTO, ACCELEROMETER, GYROSCOPE }

    var sensorMode = SensorMode.ACCELEROMETER

    private val sensor: Sensor?
        get() = when (sensorMode) {
            SensorMode.GYROSCOPE -> sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            SensorMode.ACCELEROMETER -> sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            SensorMode.AUTO -> sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
                ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    /**the smoothed angle will always be between -1 to 1**/
    private var smoothedAngle = 0f
    private var rawRollDeg = 0f
    private var calibrationOffset = 0f

    init {
        setImageResource(R.drawable.steering_wheel)
        loadSettingsFromPrefs()
    }

    private var sensorRegistered = false

    private fun loadSettingsFromPrefs() {
        maxTiltDeg = SteeringWheelPrefs.getMaxTiltDeg(prefs)
        maxWheelRotation = SteeringWheelPrefs.getMaxWheelRotation(prefs)
        smoothingFactor = SteeringWheelPrefs.getSmoothingFactor(prefs)
        sensorDelayConstant = SteeringWheelPrefs.getSensorDelayConstant(prefs)
        calibrationOffset = SteeringWheelPrefs.getCalibrationOffset(prefs)
    }

    fun reloadSettingsFromPrefs() {
        loadSettingsFromPrefs()
    }

    fun registerSensor() {
        if (sensorRegistered) return
        sensor?.let {
            sensorManager.registerListener(this, it, sensorDelayConstant)
            sensorRegistered = true
        }
    }

    fun unregisterSensor() {
        sensorManager.unregisterListener(this)
        sensorRegistered = false
    }

    fun calibrate() {
        calibrationOffset = rawRollDeg
        SteeringWheelPrefs.saveCalibrationOffset(prefs, calibrationOffset)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        registerSensor()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unregisterSensor()
    }

    // on custom layout the steering will always be attached to the window
    // so register/unregister based on the visibility.
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        loadSettingsFromPrefs()  // Reload settings when visibility changes
        if (visibility == VISIBLE) registerSensor() else unregisterSensor()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val rollDeg: Float

        when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val remapped = FloatArray(9)
                when (display?.rotation) {
                    Surface.ROTATION_90 ->
                        SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, remapped)
                    Surface.ROTATION_270 ->
                        SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, remapped)
                    else ->
                        SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Y, remapped)
                }
                SensorManager.getOrientation(remapped, orientationAngles)
                rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                rollDeg = when (display?.rotation) {
                    Surface.ROTATION_90  -> (event.values[1] / SensorManager.GRAVITY_EARTH) * 90f
                    Surface.ROTATION_270 -> -(event.values[1] / SensorManager.GRAVITY_EARTH) * 90f
                    else -> (event.values[0] / SensorManager.GRAVITY_EARTH) * 90f
                }
            }
            else -> return
        }

        rawRollDeg = rollDeg
        val corrected = rollDeg - calibrationOffset
        val clamped = corrected.coerceIn(-maxTiltDeg, maxTiltDeg)
        val normalized = clamped / maxTiltDeg

        smoothedAngle += (normalized - smoothedAngle) * smoothingFactor

        post {
            rotation = smoothedAngle * maxWheelRotation
            onSteeringChanged?.invoke(smoothedAngle)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}