package com.technosaurus.MagicGamepad.util

import android.content.SharedPreferences
import android.hardware.SensorManager

/**
 * Preferences utility for SteeringWheelView settings.
 * Manages: maxTiltDeg, maxWheelRotation, smoothingFactor, sensorDelay, calibrationOffset
 */
object SteeringWheelPrefs {

    const val PREFERENCES_FILE = "com.technosaurus.MagicGamepad.preferences"
    
    // Preference keys
    private const val KEY_MAX_TILT_DEG = "steering_max_tilt_deg"
    private const val KEY_MAX_WHEEL_ROTATION = "steering_max_wheel_rotation"
    private const val KEY_SMOOTHING_FACTOR = "steering_smoothing_factor"
    private const val KEY_SENSOR_DELAY = "steering_sensor_delay"
    private const val KEY_CALIBRATION_OFFSET = "steering_calibration_offset"
    
    // Default values
    private const val DEFAULT_MAX_TILT_DEG = 45f
    private const val DEFAULT_MAX_WHEEL_ROTATION = 180f
    private const val DEFAULT_SMOOTHING_FACTOR = 0.40f
    private const val DEFAULT_SENSOR_DELAY = "FAST"
    private const val DEFAULT_CALIBRATION_OFFSET = 0f
    
    /**
     * Sensor delay options with user-friendly names
     */
    enum class SensorDelayOption(val displayName: String, val androidConstant: Int) {
        FAST("Fast", SensorManager.SENSOR_DELAY_GAME),
        NORMAL("Normal", SensorManager.SENSOR_DELAY_UI),
        SLOW("Slow", SensorManager.SENSOR_DELAY_NORMAL);
        
        companion object {
            fun fromAndroidConstant(constant: Int): SensorDelayOption {
                return when (constant) {
                    SensorManager.SENSOR_DELAY_GAME -> FAST
                    SensorManager.SENSOR_DELAY_UI -> NORMAL
                    SensorManager.SENSOR_DELAY_NORMAL -> SLOW
                    else -> FAST
                }
            }
        }
    }
    
    // ── Getters ────────────────────────────────────────────────────────────────
    
    fun getMaxTiltDeg(prefs: SharedPreferences): Float {
        return prefs.getFloat(KEY_MAX_TILT_DEG, DEFAULT_MAX_TILT_DEG)
    }
    
    fun getMaxWheelRotation(prefs: SharedPreferences): Float {
        return prefs.getFloat(KEY_MAX_WHEEL_ROTATION, DEFAULT_MAX_WHEEL_ROTATION)
    }
    
    fun getSmoothingFactor(prefs: SharedPreferences): Float {
        return prefs.getFloat(KEY_SMOOTHING_FACTOR, DEFAULT_SMOOTHING_FACTOR)
    }
    
    fun getSensorDelay(prefs: SharedPreferences): SensorDelayOption {
        val name = prefs.getString(KEY_SENSOR_DELAY, DEFAULT_SENSOR_DELAY) ?: DEFAULT_SENSOR_DELAY
        return try {
            SensorDelayOption.valueOf(name)
        } catch (e: IllegalArgumentException) {
            SensorDelayOption.FAST
        }
    }
    
    fun getSensorDelayConstant(prefs: SharedPreferences): Int {
        return getSensorDelay(prefs).androidConstant
    }
    
    fun getCalibrationOffset(prefs: SharedPreferences): Float {
        return prefs.getFloat(KEY_CALIBRATION_OFFSET, DEFAULT_CALIBRATION_OFFSET)
    }
    
    // ── Setters ────────────────────────────────────────────────────────────────
    
    fun saveMaxTiltDeg(prefs: SharedPreferences, value: Float) {
        prefs.edit().putFloat(KEY_MAX_TILT_DEG, value).apply()
    }
    
    fun saveMaxWheelRotation(prefs: SharedPreferences, value: Float) {
        prefs.edit().putFloat(KEY_MAX_WHEEL_ROTATION, value).apply()
    }
    
    fun saveSmoothingFactor(prefs: SharedPreferences, value: Float) {
        prefs.edit().putFloat(KEY_SMOOTHING_FACTOR, value).apply()
    }
    
    fun saveSensorDelay(prefs: SharedPreferences, option: SensorDelayOption) {
        prefs.edit().putString(KEY_SENSOR_DELAY, option.name).apply()
    }
    
    fun saveCalibrationOffset(prefs: SharedPreferences, value: Float) {
        prefs.edit().putFloat(KEY_CALIBRATION_OFFSET, value).apply()
    }
    
    // ── Reset to Defaults ──────────────────────────────────────────────────────
    
    fun resetAllToDefaults(prefs: SharedPreferences) {
        prefs.edit().apply {
            putFloat(KEY_MAX_TILT_DEG, DEFAULT_MAX_TILT_DEG)
            putFloat(KEY_MAX_WHEEL_ROTATION, DEFAULT_MAX_WHEEL_ROTATION)
            putFloat(KEY_SMOOTHING_FACTOR, DEFAULT_SMOOTHING_FACTOR)
            putString(KEY_SENSOR_DELAY, DEFAULT_SENSOR_DELAY)
            putFloat(KEY_CALIBRATION_OFFSET, DEFAULT_CALIBRATION_OFFSET)
        }.apply()
    }
}
