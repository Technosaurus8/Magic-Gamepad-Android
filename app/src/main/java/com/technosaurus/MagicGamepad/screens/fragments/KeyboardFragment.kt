package com.technosaurus.MagicGamepad.screens.fragments

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.technosaurus.MagicGamepad.R
import com.technosaurus.MagicGamepad.screens.RemoteHost
import com.technosaurus.MagicGamepad.ui.AdBanner
import com.technosaurus.MagicGamepad.util.FullscreenHelper
import kotlinx.coroutines.delay

// ── Palette ───────────────────────────────────────────────────────────────────
private val KB_BgDeep       = Color(0xFF07080F)
private val KB_BgField      = Color(0xFF0D1025)
private val KB_BgKey        = Color(0xFF1C2140)
private val KB_AccentViolet = Color(0xFF8B7FFF)
private val KB_AccentPink   = Color(0xFFFF6FD8)
private val KB_AccentCyan   = Color(0xFF47E5FF)
private val KB_AccentAmber  = Color(0xFFFFB547)
private val KB_AccentGreen  = Color(0xFF00E5A0)
private val KB_TextPrim     = Color(0xFFECEEFF)
private val KB_TextSub      = Color(0xFF8A9CC8) // ← was 0xFF4A4F7A, much brighter
private val KB_Div          = Color(0xFF252A45) // ← was 0xFF181B30, more visible

class KeyboardFragment : Fragment(), DrawerAwareFragment {

    private val _isDrawerOpen = mutableStateOf(true)// set to true because initially the drawer is open.

    override fun onDrawerStateChanged(isOpen: Boolean) {
        _isDrawerOpen.value = isOpen
    }

    private var host: RemoteHost? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        host = context as? RemoteHost
            ?: throw RuntimeException("$context must implement RemoteHost")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            MaterialTheme {
                KeyboardScreen(onSend = { host?.send(it) }, isDrawerOpen = _isDrawerOpen.value )
            }
        }
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FullscreenHelper.exitFullscreen(requireActivity())
        host?.setDrawerLocked(false)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
    }

    override fun onDetach() {
        super.onDetach()
        host = null
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
fun KeyboardScreen(onSend: (String) -> Unit, isDrawerOpen: Boolean) {
    var keystroke    by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboard     = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KB_BgDeep)
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(Color(0x258B7FFF), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Text input row ────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value       = keystroke,
                        onValueChange = { keystroke = it },
                        placeholder = {
                            Text(
                                "Type to send...",
                                color      = KB_TextSub,
                                fontSize   = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction      = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            if (keystroke.isNotBlank()) {
                                onSend("k3y$keystroke")
                                keystroke = ""
                            }
                            keyboard?.hide()
                            focusManager.clearFocus()
                        }),
                        shape  = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor      = KB_AccentViolet,
                            unfocusedBorderColor    = KB_Div,
                            focusedTextColor        = KB_TextPrim,
                            unfocusedTextColor      = KB_TextPrim,
                            cursorColor             = KB_AccentViolet,
                            focusedContainerColor   = KB_BgField,
                            unfocusedContainerColor = KB_BgField,
                        ),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize   = 14.sp,
                            color      = KB_TextPrim
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Send button
                    HoldKey(
                        label    = null,
                        icon     = Icons.AutoMirrored.Rounded.Send,
                        accent   = KB_AccentViolet,
                        modifier = Modifier.size(56.dp),
                        onDown   = {
                            if (keystroke.isNotBlank()) {
                                onSend("k3y$keystroke")
                                keystroke = ""
                            }
                            keyboard?.hide()
                            focusManager.clearFocus()
                        },
                        onUp = {}
                    )
                }

                // ── Row 1: Modifier keys ──────────────────────────────────────────
                KeyRow {
                    HoldKey("CTRL",  accent = KB_AccentCyan,    modifier = Modifier.weight(1f).height(52.dp), downMsg = "ctrl_down",  upMsg = "ctrl_up",  onSend = onSend)
                    HoldKey("ALT",   accent = KB_AccentCyan,    modifier = Modifier.weight(1f).height(52.dp), downMsg = "alt_down",   upMsg = "alt_up",   onSend = onSend)
                    HoldKey("WIN",   accent = KB_AccentViolet,  modifier = Modifier.weight(1f).height(52.dp), downMsg = "win_down",   upMsg = "win_up",   onSend = onSend)
                    HoldKey("TAB",   accent = KB_AccentViolet,  modifier = Modifier.weight(1f).height(52.dp), downMsg = "tab_down",   upMsg = "tab_up",   onSend = onSend)
                }

                // ── Row 2: Action keys ────────────────────────────────────────────
                KeyRow {
                    HoldKey("SHIFT",  accent = KB_AccentGreen,  modifier = Modifier.weight(2f).height(52.dp), downMsg = "shift_down",     upMsg = "shift_up",     onSend = onSend)
                    HoldKey("ENTER",  accent = KB_AccentGreen,  modifier = Modifier.weight(2f).height(52.dp), downMsg = "enter_down",     upMsg = "enter_up",     onSend = onSend)
                    HoldKey(label = null, icon = Icons.AutoMirrored.Rounded.Backspace,     accent = KB_AccentAmber,  modifier = Modifier.weight(2f).height(52.dp), downMsg = "backspace_down", upMsg = "backspace_up", onSend = onSend)
                    HoldKey("DEL",    accent = KB_AccentAmber,  modifier = Modifier.weight(1f).height(52.dp), downMsg = "delete_down",    upMsg = "delete_up",    onSend = onSend)
                }

                // ── Row 3: Arrow + media keys ─────────────────────────────────────
                KeyRow {
                    HoldKey(label = null, icon = Icons.Rounded.SkipPrevious,  accent = KB_AccentGreen, modifier = Modifier.weight(1f).height(52.dp), onDown = { onSend("prev") },       onUp = {})
                    HoldKey(label = null, icon = Icons.Rounded.PlayArrow,  accent = KB_AccentGreen, modifier = Modifier.weight(1f).height(52.dp), onDown = { onSend("play") },       onUp = {})
                    HoldKey(label = null, icon = Icons.Rounded.SkipNext,  accent = KB_AccentGreen, modifier = Modifier.weight(1f).height(52.dp), onDown = { onSend("next") },       onUp = {})
                    HoldKey(label = null, icon = Icons.AutoMirrored.Rounded.VolumeOff, accent = KB_AccentAmber, modifier = Modifier.weight(1f).height(52.dp), onDown = { onSend("mute") },       onUp = {})
                    HoldKey(label = null, icon = Icons.AutoMirrored.Rounded.VolumeDown, accent = KB_AccentAmber,modifier = Modifier.weight(1f).height(52.dp), onDown = { onSend("down") },   onUp = {}, isRepeatMode = true)
                    HoldKey(label = null, icon = Icons.AutoMirrored.Rounded.VolumeUp, accent = KB_AccentAmber,modifier = Modifier.weight(1f).height(52.dp), onDown = { onSend("up") },     onUp = {}, isRepeatMode = true)
                }
                // ── Row 4: Function-style shortcuts ──────────────────────────────────
                KeyRow {
                    HoldKey("ESC",    accent = KB_AccentAmber,   modifier = Modifier.weight(1f).height(52.dp), downMsg = "esc_down",    upMsg = "esc_up",    onSend = onSend)
                    HoldKey("HOME",   accent = KB_AccentCyan,    modifier = Modifier.weight(1f).height(52.dp), downMsg = "home_down",   upMsg = "home_up",   onSend = onSend)
                    HoldKey("END",    accent = KB_AccentCyan,    modifier = Modifier.weight(1f).height(52.dp), downMsg = "end_down",    upMsg = "end_up",    onSend = onSend)
                    HoldKey("PG UP",  accent = KB_AccentViolet,  modifier = Modifier.weight(1f).height(52.dp), downMsg = "pgup_down",   upMsg = "pgup_up",   onSend = onSend)
                    HoldKey("PG DN",  accent = KB_AccentViolet,  modifier = Modifier.weight(1f).height(52.dp), downMsg = "pgdown_down", upMsg = "pgdown_up", onSend = onSend)
                }

                // ── Row 5: Arrow cross ────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HoldKey(
                            label = null, icon = Icons.Rounded.KeyboardArrowUp,
                            accent = KB_AccentPink, modifier = Modifier.width(110.dp).height(72.dp),
                            downMsg = "up_arrow_down", upMsg = "up_arrow_up", onSend = onSend
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HoldKey(
                                label = null, icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                accent = KB_AccentPink, modifier = Modifier.width(110.dp).height(72.dp),
                                downMsg = "left_arrow_down", upMsg = "left_arrow_up", onSend = onSend
                            )
                            HoldKey(
                                label = null, icon = Icons.Rounded.KeyboardArrowDown,
                                accent = KB_AccentPink, modifier = Modifier.width(110.dp).height(72.dp),
                                downMsg = "down_arrow_down", upMsg = "down_arrow_up", onSend = onSend
                            )
                            HoldKey(
                                label = null, icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                accent = KB_AccentPink, modifier = Modifier.width(110.dp).height(72.dp),
                                downMsg = "right_arrow_down", upMsg = "right_arrow_up", onSend = onSend
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
            if (!isDrawerOpen){
                Column(
                    modifier = Modifier
                        .imePadding() //moves ad above keyboard when it appears
                ) {
                    AdBanner(stringResource(R.string.ad_kb))
                }
            }
        }
    }
}

// ── Key row container ─────────────────────────────────────────────────────────
@Composable
private fun KeyRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = { content() }
    )
}

// ── Hold key — press sends downMsg, release sends upMsg ───────────────────────
@Composable
private fun HoldKey(
    label: String?,
    accent: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    downMsg: String? = null,
    upMsg: String? = null,
    isRepeatMode: Boolean? = false,
    onSend: ((String) -> Unit)? = null,
    onDown: () -> Unit = { if (downMsg != null && onSend != null) onSend(downMsg) },
    onUp: () -> Unit = { if (upMsg != null && onSend != null) onSend(upMsg) }
) {
    var isPressed by remember { mutableStateOf(false) }
    LaunchedEffect(isPressed, isRepeatMode) {
        if (isPressed && isRepeatMode == true) {
            while (isPressed) {
                onDown()
                delay(80)
            }
        }
    }
    val bgColor by animateColorAsState(
        targetValue   = if (isPressed) accent.copy(alpha = 0.22f) else KB_BgKey,
        animationSpec = tween(80),
        label         = "key_bg_$label"
    )
    val borderAlpha by animateFloatAsState(
        targetValue   = if (isPressed) 0.9f else 0.35f,
        animationSpec = tween(80),
        label         = "key_border_$label"
    )
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.94f else 1f,
        animationSpec = tween(80),
        label         = "key_scale_$label"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = borderAlpha),
                        accent.copy(alpha = borderAlpha * 0.3f)
                    )
                ),
                RoundedCornerShape(12.dp)
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event   = awaitPointerEvent()
                        val pressed = event.changes.any { it.pressed }
                        if (pressed && !isPressed) {
                            isPressed = true
                            if (isRepeatMode != true) {
                                onDown()
                            }
                            event.changes.forEach { it.consume() }
                        } else if (!pressed && isPressed) {
                            isPressed = false
                            onUp()
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = if (isPressed) accent else KB_TextSub,
                modifier           = Modifier.size(22.dp)
            )
        } else {
            Text(
                text          = label ?: "",
                color         = if (isPressed) accent else KB_TextSub,
                fontSize      = 13.sp,
                fontWeight    = FontWeight.Bold,
                fontFamily    = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}