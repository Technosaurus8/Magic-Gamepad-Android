package com.technosaurus.MagicGamepad.screens.fragments

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.technosaurus.MagicGamepad.R
import com.technosaurus.MagicGamepad.screens.RemoteHost
import com.technosaurus.MagicGamepad.ui.AdBanner
import com.technosaurus.MagicGamepad.util.FullscreenHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Palette ───────────────────────────────────────────────────────────────────
private val TP_BgDeep      = Color(0xFF080C10)
private val TP_BgButton    = Color(0xFF1A2535)
private val TP_BgTouchpad  = Color(0xFF0F1A24)
private val TP_AccentBlue  = Color(0xFF3D8EFF)
private val TP_AccentCyan  = Color(0xFF00D2FF)
private val TP_TextSub     = Color(0xFF7A9CC0)

class TouchpadFragment : Fragment(), DrawerAwareFragment {

    private val _isDrawerOpen = mutableStateOf(true)// set to true because initially the drawer is open.

    override fun onDrawerStateChanged(isOpen: Boolean) {
        _isDrawerOpen.value = isOpen
    }

    private var host: RemoteHost? = null

    override fun onAttach(context: android.content.Context) {
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
                TouchpadScreen(onSend = { host?.send(it) },
                    isDrawerOpen = _isDrawerOpen.value )
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

// ── Touchpad Screen ───────────────────────────────────────────────────────────
@Composable
fun TouchpadScreen(onSend: (String) -> Unit, isDrawerOpen: Boolean) {
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TP_BgDeep)
    ) {
        // Ambient glow — fills entire screen including behind bars
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x253D8EFF), Color.Transparent),
                        center = Offset(0.3f, 0.2f),
                        radius = 800f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // ── Main content ──────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)           // ← takes all space above ad
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Touchpad + scroll bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TouchpadSurface(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onSend   = onSend,
                        onTap    = {
                            scope.launch {
                                delay(50)
                                onSend("lmb")
                            }
                        }
                    )
                }

                // Mouse buttons
                MouseButtons(
                    modifier  = Modifier.fillMaxWidth().height(72.dp),
                    onLmbDown = { onSend("mousedown") },
                    onLmbUp   = { onSend("mouseup") },
                    onMmbDown = { onSend("mmb_down") },
                    onMmbUp   = { onSend("mmb_up") },
                    onRmbDown = { onSend("rmb_down") },
                    onRmbUp   = { onSend("rmb_up") }
                )
            }
            if (!isDrawerOpen){
                AdBanner(stringResource(R.string.ad_tp))
            }
        }
    }
}

// ── Touchpad Surface ──────────────────────────────────────────────────────────
private const val DRAG_THRESHOLD_PX = 12f
private const val DOUBLE_TAP_TIMEOUT_MS = 300L
@Composable
private fun TouchpadSurface(
    modifier: Modifier,
    onSend: (String) -> Unit,
    onTap: () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var lastTapTime  by remember { mutableLongStateOf(0L) }
    var isDragLocked by remember { mutableStateOf(false) }
    val scope        = rememberCoroutineScope()

    val borderAlpha by animateFloatAsState(
        targetValue   = if (isDragging) 0.6f else 0.3f,
        animationSpec = tween(150),
        label         = "border"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(listOf(TP_BgTouchpad, Color(0xFF0A1018)))
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        TP_AccentBlue.copy(alpha = borderAlpha),
                        TP_AccentCyan.copy(alpha = borderAlpha * 0.5f)
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down       = awaitFirstDown(requireUnconsumed = false)
                        val downTime   = System.currentTimeMillis()
                        var lastPos    = down.position
                        var totalMoved = 0f
                        var didDrag    = false
                        var isPinching     = false
                        var gestureDecided = false
                        var lastSecondX    = 0f
                        var lastSecondY    = 0f
                        var lastPinchDist  = 0f
                        var totalPinchDist = 0f

                        // ── Double-tap-to-drag detection ──────────────────────────────────
                        val timeSinceLastTap = downTime - lastTapTime
                        val isDoubleTapHold  = timeSinceLastTap < DOUBLE_TAP_TIMEOUT_MS && !isDragLocked

                        if (isDoubleTapHold) {
                            isDragLocked = true
                            onSend("mousedown") // no lmb before this — clean continuous hold
                        }

                        while (true) {
                            val event   = awaitPointerEvent()
                            val changes = event.changes

                            // ── 2-finger gesture ──────────────────────────────────────────
                            if (changes.count { it.pressed } == 2) {
                                didDrag    = true
                                isDragging = false

                                val first  = changes[0]
                                val second = changes[1]

                                val currentX    = second.position.x
                                val currentY    = second.position.y
                                val currentDist = kotlin.math.sqrt(
                                    (second.position.x - first.position.x).let { it * it } +
                                            (second.position.y - first.position.y).let { it * it }
                                )

                                if (lastSecondY == 0f) {
                                    lastSecondX   = currentX
                                    lastSecondY   = currentY
                                    lastPinchDist = currentDist
                                    changes.forEach { it.consume() }
                                    continue
                                }

                                val dx          = currentX - lastSecondX
                                val dy          = currentY - lastSecondY
                                val distDiff    = currentDist - lastPinchDist
                                val translation = kotlin.math.sqrt(dx * dx + dy * dy)

                                if (!gestureDecided) {
                                    if (kotlin.math.abs(distDiff) > 8f || translation > 8f) {
                                        isPinching     = kotlin.math.abs(distDiff) > translation
                                        gestureDecided = true
                                        if (isPinching) onSend("ctrl_down")
                                    }
                                }

                                if (gestureDecided) {
                                    if (isPinching) {
                                        totalPinchDist += distDiff
                                        val scrollSteps = (totalPinchDist / 15f).toInt()
                                        if (scrollSteps != 0) {
                                            repeat(kotlin.math.abs(scrollSteps)) {
                                                onSend(if (scrollSteps > 0) "v,10" else "v,-10")
                                            }
                                            totalPinchDist -= scrollSteps * 15f
                                        }
                                    } else {
                                        if (kotlin.math.abs(dy) > kotlin.math.abs(dx)) {
                                            if (kotlin.math.abs(dy) > 1f) onSend("v,${dy.toInt()}")
                                        } else {
                                            if (kotlin.math.abs(dx) > 1f) onSend("h,${dx.toInt()}")
                                        }
                                    }
                                }

                                lastSecondX   = currentX
                                lastSecondY   = currentY
                                lastPinchDist = currentDist
                                changes.forEach { it.consume() }
                                continue
                            }

                            // ── Second finger lifted ──────────────────────────────────────
                            if (isPinching) onSend("ctrl_up")
                            isPinching     = false
                            gestureDecided = false
                            totalPinchDist = 0f
                            lastSecondX    = 0f
                            lastSecondY    = 0f
                            lastPinchDist  = 0f

                            val change = changes.firstOrNull() ?: break

                            if (!change.pressed) {
                                when {
                                    // Drag-lock active — finger lifted, release mouse
                                    isDragLocked -> {
                                        onSend("mouseup")
                                        isDragLocked = false
                                        didDrag      = true // prevent tap firing
                                    }
                                    // Short tap — record time for double-tap detection, fire click
                                    !didDrag && System.currentTimeMillis() - downTime < 200 -> {
                                        val tapTime = System.currentTimeMillis()
                                        lastTapTime = tapTime
                                        scope.launch {
                                            delay(DOUBLE_TAP_TIMEOUT_MS)
                                            if (!isDragLocked) onTap()
                                        }
                                    }
                                }
                                isDragging = false
                                break
                            }

                            // ── Single finger move ────────────────────────────────────────
                            val delta = change.position - lastPos
                            totalMoved += kotlin.math.sqrt(
                                delta.x * delta.x + delta.y * delta.y
                            )

                            if (totalMoved > DRAG_THRESHOLD_PX) {
                                didDrag    = true
                                isDragging = true
                                val dx = delta.x.toInt()
                                val dy = delta.y.toInt()
                                if (dx != 0 || dy != 0) onSend("$dx,$dy")
                            }

                            lastPos = change.position
                            change.consume()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
        ) {
            val dotSpacing = 28.dp.toPx()
            val dotRadius  = 1.2.dp.toPx()
            val cols = (size.width  / dotSpacing).toInt() + 1
            val rows = (size.height / dotSpacing).toInt() + 1

            // Center the grid
            val offsetX = (size.width  - (cols - 1) * dotSpacing) / 2f
            val offsetY = (size.height - (rows - 1) * dotSpacing) / 2f

            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    val x = offsetX + col * dotSpacing
                    val y = offsetY + row * dotSpacing

                    // Fade dots near edges using distance from center
                    val distFromCenterX = kotlin.math.abs(x - size.width  / 2f) / (size.width  / 2f)
                    val distFromCenterY = kotlin.math.abs(y - size.height / 2f) / (size.height / 2f)
                    val edgeFade = (1f - distFromCenterX * distFromCenterX) *
                            (1f - distFromCenterY * distFromCenterY)

                    drawCircle(
                        color  = TP_AccentBlue.copy(alpha = 0.25f * edgeFade),
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                }
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Rounded.DragHandle,
                contentDescription = null,
                tint = TP_TextSub.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isDragging) "moving" else "slide to move",
                color = TP_TextSub.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// ── Mouse Buttons ─────────────────────────────────────────────────────────────
@Composable
private fun MouseButtons(
    modifier: Modifier,
    onLmbDown: () -> Unit,
    onLmbUp: () -> Unit,
    onMmbDown: () -> Unit,
    onMmbUp: () -> Unit,
    onRmbDown: () -> Unit,
    onRmbUp: () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Left mouse button — needs press/release tracking
        MouseButton(
            label    = "LEFT",
            modifier = Modifier.weight(2f).fillMaxHeight(),
            accent   = TP_AccentBlue,
            onDown   = onLmbDown,
            onUp     = onLmbUp
        )
        // Middle button
        MouseButton(
            label    = "MID",
            modifier = Modifier.weight(1f).fillMaxHeight(),
            accent   = TP_AccentCyan,
            onDown   = onMmbDown,
            onUp     = onMmbUp
        )
        // Right mouse button
        MouseButton(
            label    = "RIGHT",
            modifier = Modifier.weight(2f).fillMaxHeight(),
            accent   = TP_AccentBlue,
            onDown   = onRmbDown,
            onUp     = onRmbUp,
        )
    }
}

@Composable
private fun MouseButton(
    label: String,
    modifier: Modifier,
    accent: Color,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val bgColor by animateColorAsState(
        targetValue   = if (isPressed) accent.copy(alpha = 0.25f) else TP_BgButton,
        animationSpec = tween(80),
        label         = "btn_bg_$label"
    )
    val borderAlpha by animateFloatAsState(
        targetValue   = if (isPressed) 0.8f else 0.35f,
        animationSpec = tween(80),
        label         = "btn_border_$label"
    )
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.96f else 1f,
        animationSpec = tween(80),
        label         = "btn_scale_$label"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = borderAlpha),
                        accent.copy(alpha = borderAlpha * 0.3f)
                    )
                ),
                RoundedCornerShape(14.dp)
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        // Wait for any event
                        val event = awaitPointerEvent()
                        val pressed = event.changes.any { it.pressed }

                        if (pressed && !isPressed) {
                            // Finger just went down
                            isPressed = true
                            onDown()
                            event.changes.forEach { it.consume() }
                        } else if (!pressed && isPressed) {
                            // Finger just lifted
                            isPressed = false
                            onUp()
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text          = label,
            color         = if (isPressed) accent else TP_TextSub,
            fontSize      = 11.sp,
            fontWeight    = FontWeight.Bold,
            fontFamily    = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )
    }
}