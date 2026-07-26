package com.technosaurus.MagicGamepad.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.technosaurus.MagicGamepad.util.SteeringWheelPrefs
import com.technosaurus.MagicGamepad.views.SteeringWheelView

// ── Palette ───────────────────────────────────────────────────────────────────
private val CAL_BgDeep       = Color(0xFF07080F)
private val CAL_BgCard       = Color(0xFF0F1120)
private val CAL_AccentCyan   = Color(0xFF47E5FF)
private val CAL_AccentViolet = Color(0xFF8B7FFF)
private val CAL_AccentGreen  = Color(0xFF4ADE80)
private val CAL_AccentRed    = Color(0xFFFF6B6B)
private val CAL_TextPrim     = Color(0xFFECEEFF)
private val CAL_TextSub      = Color(0xFF8A9CC8)
private val CAL_Div          = Color(0xFF181B30)

@Composable
fun SteeringCalibrationScreen(navController: NavController? = null) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val prefs = remember { context.getSharedPreferences(SteeringWheelPrefs.PREFERENCES_FILE, Context.MODE_PRIVATE) }
    val scrollState = rememberScrollState()
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Load current settings
    var maxTiltDeg by remember { mutableStateOf(SteeringWheelPrefs.getMaxTiltDeg(prefs)) }
    var maxWheelRotation by remember { mutableStateOf(SteeringWheelPrefs.getMaxWheelRotation(prefs)) }
    var smoothingFactor by remember { mutableStateOf(SteeringWheelPrefs.getSmoothingFactor(prefs)) }
    var calibrationOffset by remember { mutableStateOf(SteeringWheelPrefs.getCalibrationOffset(prefs)) }
    var sensorDelay by remember { mutableStateOf(SteeringWheelPrefs.getSensorDelay(prefs)) }

    var steeringWheelView by remember { mutableStateOf<SteeringWheelView?>(null) }
    var steeringValue by remember { mutableFloatStateOf(0f) }
    // Persist helpers
    fun saveAndReload() {
        SteeringWheelPrefs.saveMaxTiltDeg(prefs, maxTiltDeg)
        SteeringWheelPrefs.saveMaxWheelRotation(prefs, maxWheelRotation)
        SteeringWheelPrefs.saveSmoothingFactor(prefs, smoothingFactor)
        SteeringWheelPrefs.saveCalibrationOffset(prefs, calibrationOffset)
        SteeringWheelPrefs.saveSensorDelay(prefs, sensorDelay)
        steeringWheelView?.reloadSettingsFromPrefs()
    }

    fun resetToDefaults() {
        SteeringWheelPrefs.resetAllToDefaults(prefs)
        maxTiltDeg = SteeringWheelPrefs.getMaxTiltDeg(prefs)
        maxWheelRotation = SteeringWheelPrefs.getMaxWheelRotation(prefs)
        smoothingFactor = SteeringWheelPrefs.getSmoothingFactor(prefs)
        calibrationOffset = SteeringWheelPrefs.getCalibrationOffset(prefs)
        sensorDelay = SteeringWheelPrefs.getSensorDelay(prefs)
        steeringWheelView?.reloadSettingsFromPrefs()
    }

    DisposableEffect(Unit) {
        if(android.os.Build.VERSION.SDK_INT > 34) return@DisposableEffect onDispose {}
        val activity = context as? Activity
        val original = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = original
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CAL_BgDeep)
    ) {
        // Ambient blobs
        Box(
            Modifier
                .size(300.dp)
                .background(
                    Brush.radialGradient(listOf(Color(0x158B7FFF), Color.Transparent)),
                    CircleShape
                )
        )

        if (isLandscape) {
            // Landscape layout: two columns
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        key(sensorDelay) {
                            AndroidView(
                                factory = { ctx ->
                                    SteeringWheelView(ctx).apply {
                                        steeringWheelView = this
                                        maxTiltDeg = this.maxTiltDeg
                                        maxWheelRotation = this.maxWheelRotation
                                        smoothingFactor = this.smoothingFactor
                                        onSteeringChanged = { normalized -> steeringValue = normalized * this.maxTiltDeg }
                                        reloadSettingsFromPrefs()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize(0.85f)
                                    .background(CAL_BgCard, RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            )
                        }
                    }
                    Text(
                        text = String.format("%.2f", steeringValue),
                        color = CAL_AccentCyan,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .background(CAL_AccentGreen.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .clickable {
                                steeringWheelView?.calibrate()
                                calibrationOffset = SteeringWheelPrefs.getCalibrationOffset(prefs)
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Calibrate Steering",
                            color = CAL_AccentGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Right: Controls (50%)
                Column(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Steering Settings",
                            color = CAL_TextPrim,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        navController?.let {
                            IconButton(onClick = { it.popBackStack() }) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    tint = CAL_TextSub,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Max Tilt Deg Slider
                    ControlSlider(
                        label = "Max Tilt Angle",
                        value = maxTiltDeg,
                        onValueChange = { newValue ->
                            maxTiltDeg = newValue
                            saveAndReload()
                        },
                        valueRange = 20f..90f,
                        steps = 13,  // (90-20)/5
                        displayValue = "${maxTiltDeg.toInt()}°",
                        color = CAL_AccentCyan
                    )

                    // Max Wheel Rotation Slider
                    ControlSlider(
                        label = "Max Wheel Rotation",
                        value = maxWheelRotation,
                        onValueChange = { newValue ->
                            maxWheelRotation = newValue
                            saveAndReload()
                        },
                        valueRange = 90f..360f,
                        steps = 26,  // (360-90)/10
                        displayValue = "${maxWheelRotation.toInt()}°",
                        color = CAL_AccentViolet
                    )

                    // Smoothing Factor Slider
                    ControlSlider(
                        label = "Smoothing Factor",
                        value = 1.05f - smoothingFactor,  // invert for display
                        onValueChange = { newValue ->
                            smoothingFactor = 1.05f - newValue  // invert back
                            saveAndReload()
                        },
                        valueRange = 0.05f..1f,
                        steps = 18,
                        displayValue = String.format("%.2f", smoothingFactor),
                        color = CAL_AccentGreen
                    )

                    // Calibration Offset Slider
                    ControlSlider(
                        label = "Calibration Offset",
                        value = calibrationOffset,
                        onValueChange = { newValue ->
                            calibrationOffset = newValue
                            saveAndReload()
                        },
                        valueRange = -45f..45f,
                        steps = 89,  // (45-(-45))/1
                        displayValue = "${calibrationOffset.toInt()}°",
                        color = CAL_TextSub
                    )

                    // Sensor Delay Dropdown
                    SensorDelaySelector(
                        selected = sensorDelay,
                        onSelect = { newDelay ->
                            sensorDelay = newDelay
                            saveAndReload()
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    // Reset Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CAL_AccentRed.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .clickable { resetToDefaults() }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Reset to Defaults",
                            color = CAL_AccentRed,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        } else {

            // Portrait: Show "rotate to landscape" message
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(CAL_AccentCyan.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "⟲",
                        fontSize = 48.sp,
                        color = CAL_AccentCyan
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    "Please rotate to landscape",
                    color = CAL_TextPrim,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    "The steering calibration screen works properly for landscape orientation.",
                    color = CAL_TextSub,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth()
                )

                // Close button
                Spacer(Modifier.height(24.dp))
                navController?.let {
                    TextButton(onClick = { it.popBackStack() }) {
                        Text("Back to Settings", color = CAL_AccentCyan)
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayValue: String,
    color: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CAL_BgCard, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = CAL_TextPrim,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                displayValue,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SensorDelaySelector(
    selected: SteeringWheelPrefs.SensorDelayOption,
    onSelect: (SteeringWheelPrefs.SensorDelayOption) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CAL_BgCard, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Sensor Delay",
            color = CAL_TextPrim,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SteeringWheelPrefs.SensorDelayOption.entries.forEach { option ->
                TextButton(
                    onClick = { onSelect(option) },
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (selected == option) CAL_AccentCyan.copy(alpha = 0.2f)
                            else CAL_Div,
                            RoundedCornerShape(6.dp)
                        ),
                ) {
                    Text(
                        option.displayName,
                        color = if (selected == option) CAL_AccentCyan else CAL_TextSub,
                        fontSize = 10.sp,
                        fontWeight = if (selected == option) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
