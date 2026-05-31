package com.technosaurus.MagicGamepad.screens.fragments

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewConfiguration
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.technosaurus.MagicGamepad.screens.RemoteHost
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

class TouchpadFragment : Fragment(){
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
                TouchpadScreen(onSend = { host?.send(it) })
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
fun TouchpadScreen(onSend: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val buttonStripHeight = if (isLandscape) 48.dp else 72.dp
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
                .padding(
                    horizontal = if (isLandscape) 24.dp else 12.dp,
                    vertical   = if (isLandscape) 8.dp  else 12.dp
                ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Touchpad
            TouchpadSurface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                onSend   = onSend,
                onTap    = {
                    scope.launch {
                        delay(50)
                        onSend("lmb")
                    }
                }
            )
            // Mouse buttons
            MouseButtons(
                modifier  = Modifier.fillMaxWidth().height(buttonStripHeight),
                onLmbDown = { onSend("mousedown") },
                onLmbUp   = { onSend("mouseup") },
                onMmbDown = { onSend("mmb_down") },
                onMmbUp   = { onSend("mmb_up") },
                onRmbDown = { onSend("rmb_down") },
                onRmbUp   = { onSend("rmb_up") }
            )
        }
    }
}

// ── Touchpad Surface ──────────────────────────────────────────────────────────
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
    val DRAG_THRESHOLD_PX = ViewConfiguration.get(LocalContext.current).scaledTouchSlop.toFloat()
    val DOUBLE_TAP_TIMEOUT_MS = ViewConfiguration.getDoubleTapTimeout().toLong()
    val GESTURE_THRESHOLD = DRAG_THRESHOLD_PX * 0.5f// higher the value harder to trigger.
    val PINCH_DIVISOR = DRAG_THRESHOLD_PX * 0.5f
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
                        var accX       = 0f
                        var accY       = 0f
                        var scrollAccX = 0f
                        var scrollAccY = 0f
                        var scrollAxis = 0
                        var wasMultiTouch = false

                        // ── Double-tap-to-drag detection ──────────────────────────────────
                        val timeSinceLastTap = downTime - lastTapTime
                        val isDoubleTapHold  = timeSinceLastTap < DOUBLE_TAP_TIMEOUT_MS && !isDragLocked

                        if (isDoubleTapHold) {
                            isDragLocked = true
                            onSend("mousedown")
                        }

                        while (true) {
                            val event   = awaitPointerEvent()
                            val changes = event.changes

                            // ── 2-finger gesture ──────────────────────────────────────────
                            if (changes.count { it.pressed } == 2) {
                                wasMultiTouch = true
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
                                    if (kotlin.math.abs(distDiff) > GESTURE_THRESHOLD || translation > GESTURE_THRESHOLD) {
                                        isPinching     = kotlin.math.abs(distDiff) > translation
                                        gestureDecided = true
                                        if (isPinching) onSend("ctrl_down")
                                    }
                                }

                                if (gestureDecided) {
                                    if (isPinching) {
                                        totalPinchDist += distDiff
                                        val scrollSteps = (totalPinchDist / PINCH_DIVISOR).toInt()
                                        if (scrollSteps != 0) {
                                            onSend("v,$scrollSteps")
                                            totalPinchDist -= scrollSteps * PINCH_DIVISOR
                                        }
                                    } else {
                                        if (scrollAxis == 0) {
                                            scrollAxis = if (kotlin.math.abs(dy) > kotlin.math.abs(dx)) 1 else 2
                                            Log.d("Scroll", "Axis locked: ${if (scrollAxis == 1) "VERTICAL" else "HORIZONTAL"}")
                                        }
                                        if (scrollAxis == 1) {
                                            Log.d("Scroll", "dy=$dy scrollAccY before=$scrollAccY")
                                            scrollAccY += dy
                                            val sendScrollY = scrollAccY.toInt()
                                            scrollAccY -= sendScrollY
                                            Log.d("Scroll", "sendScrollY=$sendScrollY scrollAccY after=$scrollAccY")
                                            if (sendScrollY != 0) onSend("v,$sendScrollY")
                                        } else {
                                            scrollAccX += dx
                                            val sendScrollX = scrollAccX.toInt()
                                            scrollAccX -= sendScrollX
                                            if (sendScrollX != 0) onSend("h,$sendScrollX")
                                        }
                                    }
                                } else {
                                    Log.d("Scroll", "gestureDecided=false translation=$translation distDiff=$distDiff")
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

                            // ── Release: handle BEFORE computing any delta ────────────────
                            if (!change.pressed) {
                                when {
                                    isDragLocked -> {
                                        onSend("mouseup")
                                        isDragLocked = false
                                        didDrag      = true
                                    }
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

                            // ── Single finger move (only reached when still pressed) ───────
                            if (wasMultiTouch) {
                                wasMultiTouch = false
                                lastPos = change.position   // resync position, skip delta
                                change.consume()
                                continue
                            }
                            val rawDelta = change.position - lastPos

                            accX += rawDelta.x
                            accY += rawDelta.y
                            val sendX = accX.toInt()
                            val sendY = accY.toInt()
                            accX -= sendX
                            accY -= sendY

                            totalMoved += kotlin.math.sqrt(
                                rawDelta.x * rawDelta.x + rawDelta.y * rawDelta.y
                            )

                            if (totalMoved > DRAG_THRESHOLD_PX) {
                                didDrag    = true
                                isDragging = true
                                if (sendX != 0 || sendY != 0) {   // was: abs > 1
                                    onSend("$sendX,$sendY")
                                }
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