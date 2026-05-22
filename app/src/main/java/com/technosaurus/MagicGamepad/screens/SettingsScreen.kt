package com.technosaurus.MagicGamepad.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.NetworkWifi
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.technosaurus.MagicGamepad.R
import com.technosaurus.MagicGamepad.ui.AdBanner

// ── Palette ───────────────────────────────────────────────────────────────────
private val S_BgDeep       = Color(0xFF07080F)
private val S_BgCard       = Color(0xFF0F1120)
private val S_BgField      = Color(0xFF0B0D1A)
private val S_BgChip       = Color(0xFF131628)
private val S_AccentViolet = Color(0xFF8B7FFF)
private val S_AccentPink   = Color(0xFFFF6FD8)
private val S_AccentCyan   = Color(0xFF47E5FF)
private val S_AccentAmber  = Color(0xFFFFB547)
private val S_TextPrim     = Color(0xFFECEEFF)
private val S_TextSub      = Color(0xFF4A4F7A)
private val S_Div          = Color(0xFF181B30)

// ── SharedPreferences constants (mirrors the old Activity) ────────────────────
private const val PREFERENCES_FILE      = "com.technosaurus.MagicGamepad.preferences"
private const val TOUCH_FEEDBACK_KEY    = "touch_feedback_key"
private const val WIFI_SCAN_PORT_KEY    = "wifi_scan_port_key"

// ── Touch feedback options ────────────────────────────────────────────────────
enum class TouchFeedback { VIBRATION, SOUND }

// helpers to convert between the string stored in prefs and the enum
private fun TouchFeedback.toPrefsString() = when (this) {
    TouchFeedback.VIBRATION -> "Vibration"
    TouchFeedback.SOUND     -> "Sound"
}
private fun String.toTouchFeedback() = when (this) {
    "Vibration" -> TouchFeedback.VIBRATION
    else        -> TouchFeedback.SOUND      // default matches old Activity default
}

// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
fun SettingsScreen() {
    val context      = LocalContext.current
    val prefs        = remember { context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE) }
    val focusManager = LocalFocusManager.current
    val scrollState  = rememberScrollState()

    // ── Load persisted values once ────────────────────────────────────────────
    var touchFeedback by remember {
        mutableStateOf(
            prefs.getString(TOUCH_FEEDBACK_KEY, "Sound")!!.toTouchFeedback()
        )
    }
    var scanPort by remember {
        mutableStateOf(prefs.getString(WIFI_SCAN_PORT_KEY, "8765") ?: "8765")
    }
    var portError by remember { mutableStateOf(false) }

    // ── Persist helpers ───────────────────────────────────────────────────────
    fun saveTouchFeedback(value: TouchFeedback) {
        prefs.edit { putString(TOUCH_FEEDBACK_KEY, value.toPrefsString())}
    }
    fun saveScanPort(value: String) {
        prefs.edit { putString(WIFI_SCAN_PORT_KEY, value) }
    }
    val onNavigateToLayoutEditor = {
        val intent = Intent(context, CustomizeLayoutActivity::class.java)
        context.startActivity(intent)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(S_BgDeep)
    ) {
        // Ambient blobs
        Box(
            Modifier
                .size(300.dp)
                .offset((-80).dp, (-60).dp)
                .background(
                    Brush.radialGradient(listOf(Color(0x158B7FFF), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            Modifier
                .size(200.dp)
                .offset(200.dp, 500.dp)
                .background(
                    Brush.radialGradient(listOf(Color(0x10FF6FD8), Color.Transparent)),
                    CircleShape
                )
        )

        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            SettingsHeader()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ── Network section ───────────────────────────────────────────
                SectionLabel(text = "NETWORK", icon = Icons.Rounded.NetworkWifi, tint = S_AccentCyan)

                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(S_AccentCyan.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.NetworkWifi,
                                    contentDescription = null,
                                    tint = S_AccentCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Wi-Fi Scan Port",
                                    color = S_TextPrim,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Port used when scanning for devices",
                                    color = S_TextSub,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        OutlinedTextField(
                            value = scanPort,
                            onValueChange = { input ->
                                if (input.length <= 5) {
                                    scanPort = input.filter { it.isDigit() }
                                    portError = false
                                    // save immediately on every valid keystroke
                                    saveScanPort(scanPort)
                                }
                            },
                            placeholder = {
                                Text(
                                    "e.g.  8080",
                                    color = S_TextSub,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            isError = portError,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction    = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                val port = scanPort.toIntOrNull()
                                portError = port == null || port !in 1..65535
                                // clear persisted value if invalid so it isn't used downstream
                                if (portError) saveScanPort("")
                            }),
                            shape  = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = S_AccentCyan,
                                unfocusedBorderColor    = S_Div,
                                errorBorderColor        = S_AccentAmber,
                                focusedTextColor        = S_TextPrim,
                                unfocusedTextColor      = S_TextPrim,
                                cursorColor             = S_AccentCyan,
                                focusedContainerColor   = S_BgField,
                                unfocusedContainerColor = S_BgField,
                                errorContainerColor     = Color(0xFF1A1208)
                            ),
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color      = S_TextPrim
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (portError) {
                            Text(
                                "Enter a valid port (1 – 65535)",
                                color      = S_AccentAmber,
                                fontSize   = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ── Touch feedback section ────────────────────────────────────
                SectionLabel(text = "TOUCH FEEDBACK", icon = Icons.Rounded.Tune, tint = S_AccentViolet)

                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(S_AccentViolet.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.GraphicEq,
                                    contentDescription = null,
                                    tint = S_AccentViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Feedback Type",
                                    color      = S_TextPrim,
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Response when a button is pressed",
                                    color    = S_TextSub,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        FeedbackToggle(
                            selected = touchFeedback,
                            onSelect = { selected ->
                                touchFeedback = selected
                                saveTouchFeedback(selected) // persist immediately, mirrors onItemSelected
                            }
                        )

                        val hint = if (touchFeedback == TouchFeedback.VIBRATION)
                            "Device will vibrate on each button press"
                        else
                            "A click sound plays on each button press"
                        Text(
                            text       = hint,
                            color      = S_TextSub,
                            fontSize   = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ── Layout section ────────────────────────────────────────────
                SectionLabel(text = "LAYOUT", icon = Icons.Rounded.Edit, tint = S_AccentPink)

                LayoutEditorButton(onClick = onNavigateToLayoutEditor)

                Spacer(Modifier.height(32.dp))
            }
            AdBanner(stringResource(R.string.ad_wifi))
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────
@Composable
private fun SettingsHeader() {
    val inf = rememberInfiniteTransition(label = "gear")
    val rot by inf.animateFloat(
        initialValue = 0f, targetValue = 360f, label = "gear_rot",
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF0E1020), S_BgDeep)))
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .drawBehind {
                        drawCircle(
                            color  = S_AccentViolet.copy(alpha = 0.18f),
                            radius = size.minDimension / 2,
                            style  = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = S_AccentViolet,
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer { rotationZ = rot }
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "Settings",
                    color      = S_TextPrim,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                )
                Text(
                    "Configure your gamepad",
                    color    = S_TextSub,
                    fontSize = 12.sp,
                    letterSpacing = 0.4.sp
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(S_AccentViolet.copy(alpha = 0.5f), S_AccentPink.copy(alpha = 0.2f), Color.Transparent)
                    )
                )
        )
    }
}

// ── Section label ─────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String, icon: ImageVector, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text          = text,
            color         = tint,
            fontSize      = 10.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = 1.8.sp,
            fontFamily    = FontFamily.Monospace
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(listOf(tint.copy(alpha = 0.3f), Color.Transparent))
                )
        )
    }
}

// ── Settings card container ───────────────────────────────────────────────────
@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(400, easing = EaseOutCubic)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = anim.value; translationY = (1f - anim.value) * 16f }
            .clip(RoundedCornerShape(16.dp))
            .background(S_BgCard)
            .border(
                1.dp,
                Brush.linearGradient(listOf(S_AccentViolet.copy(alpha = 0.15f), Color.Transparent)),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        content()
    }
}

// ── Feedback toggle ───────────────────────────────────────────────────────────
@Composable
private fun FeedbackToggle(
    selected: TouchFeedback,
    onSelect: (TouchFeedback) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(S_BgChip)
            .border(1.dp, S_Div, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TouchFeedback.entries.forEach { option ->
            val isSelected = selected == option
            val textColor by animateColorAsState(
                targetValue   = if (isSelected) S_TextPrim else S_TextSub,
                animationSpec = tween(250),
                label         = "chip_txt_${option.name}"
            )
            val iconTint by animateColorAsState(
                targetValue   = if (isSelected) S_TextPrim else S_TextSub,
                animationSpec = tween(250),
                label         = "chip_icon_${option.name}"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .then(
                        if (isSelected) Modifier.background(
                            Brush.linearGradient(listOf(S_AccentViolet, S_AccentPink))
                        ) else Modifier.background(Color.Transparent)
                    )
                    .clickable { onSelect(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (option == TouchFeedback.VIBRATION)
                            Icons.Rounded.Vibration
                        else
                            Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = null,
                        tint     = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text       = if (option == TouchFeedback.VIBRATION) "Vibration" else "Sound",
                        color      = textColor,
                        fontSize   = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ── Layout editor button ──────────────────────────────────────────────────────
@Composable
private fun LayoutEditorButton(onClick: () -> Unit) {
    val inf  = rememberInfiniteTransition(label = "btn_glow")
    val glow by inf.animateFloat(
        initialValue  = 0.4f, targetValue = 1f, label = "g",
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(S_AccentPink.copy(alpha = 0.18f), S_AccentViolet.copy(alpha = 0.12f))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        S_AccentPink.copy(alpha = glow * 0.7f),
                        S_AccentViolet.copy(alpha = glow * 0.5f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(S_AccentPink.copy(alpha = 0.25f), S_AccentViolet.copy(alpha = 0.15f))
                        ),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = null,
                    tint     = S_AccentPink,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Custom Layout Editor",
                    color      = S_TextPrim,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Rearrange and resize gamepad buttons",
                    color    = S_TextSub,
                    fontSize = 12.sp
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint     = S_AccentPink.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}