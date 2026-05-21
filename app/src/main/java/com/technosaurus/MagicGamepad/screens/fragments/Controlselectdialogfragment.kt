package com.technosaurus.MagicGamepad.screens
import android.app.Dialog
import android.os.Bundle
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import com.technosaurus.MagicGamepad.R

// ── Palette ───────────────────────────────────────────────────────────────────
private val CS_BgDeep    = Color(0xFF07080F)
private val CS_BgCard    = Color(0xFF0F1120)
private val CS_BgChip    = Color(0xFF131628)
private val CS_Div       = Color(0xFF252A45)
private val CS_TextPrim  = Color(0xFFECEEFF)
private val CS_TextSub   = Color(0xFF8A9CC8)

// Accent per control group
private val CS_AccentBlue   = Color(0xFF3D8EFF)  // face buttons
private val CS_AccentGreen  = Color(0xFF00E5A0)  // shoulder / trigger
private val CS_AccentPink   = Color(0xFFFF6FD8)  // joysticks
private val CS_AccentAmber  = Color(0xFFFFB547)  // dpad
private val CS_AccentViolet = Color(0xFF8B7FFF)  // menu/view

// ── Control item model ────────────────────────────────────────────────────────
data class ControlItem(
    val index:  Int,
    val label:  String,
    val accent: Color,
    val group:  String,
    val drawableRes: Int? = null
)

private val allControls = listOf(
    // Triggers & shoulders
    ControlItem(0,  "LT",          CS_AccentGreen,  "Triggers"),
    ControlItem(1,  "LB",          CS_AccentGreen,  "Triggers"),
    ControlItem(2,  "RB",          CS_AccentGreen,  "Triggers"),
    ControlItem(3,  "RT",          CS_AccentGreen,  "Triggers"),
    // Joysticks
    ControlItem(4,  "RS",          CS_AccentPink,   "Joysticks"),
    ControlItem(5,  "LS",          CS_AccentPink,   "Joysticks"),
    ControlItem(6,  "Right Stick", CS_AccentPink,   "Joysticks"),
    ControlItem(7,  "Left Stick",  CS_AccentPink,   "Joysticks"),
    // Face buttons — use drawables
    ControlItem(8,  "A",  CS_AccentBlue, "Face", R.drawable.a),
    ControlItem(9,  "B",  CS_AccentBlue, "Face", R.drawable.b),
    ControlItem(10, "X",  CS_AccentBlue, "Face", R.drawable.x),
    ControlItem(11, "Y",  CS_AccentBlue, "Face", R.drawable.y),
    // D-Pad — use drawables
    ControlItem(12, "Up",    CS_AccentAmber, "D-Pad", R.drawable.u),
    ControlItem(13, "Down",  CS_AccentAmber, "D-Pad", R.drawable.d),
    ControlItem(14, "Left",  CS_AccentAmber, "D-Pad", R.drawable.l),
    ControlItem(15, "Right", CS_AccentAmber, "D-Pad", R.drawable.r),
    // System — use drawables
    ControlItem(16, "VIEW", CS_AccentViolet, "System", R.drawable.view),
    ControlItem(17, "MENU", CS_AccentViolet, "System", R.drawable.menu),
)

class ControlSelectDialogFragment : DialogFragment() {

    private var isHidden: BooleanArray = BooleanArray(18) { false }
    private var onToggle: ((index: Int, hidden: Boolean) -> Unit)? = null

    companion object {
        fun newInstance(
            isHidden: BooleanArray,
            onToggle: (index: Int, hidden: Boolean) -> Unit
        ): ControlSelectDialogFragment {
            return ControlSelectDialogFragment().also {
                it.isHidden = isHidden.copyOf()
                it.onToggle = onToggle
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = true

        val dialog = Dialog(requireContext())
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val composeView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    ControlSelectContent(
                        initialHidden = isHidden,
                        onToggle      = { index, hidden ->
                            onToggle?.invoke(index, hidden)
                        },
                        onDone = { dismissAllowingStateLoss() }
                    )
                }
            }
        }

        dialog.setContentView(composeView)
        return dialog
    }
}

// ── Dialog content ────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ControlSelectContent(
    initialHidden: BooleanArray,
    onToggle: (index: Int, hidden: Boolean) -> Unit,
    onDone: () -> Unit
) {
    // Local visibility state — mirrors isHidden array
    val visible = remember {
        mutableStateListOf(*Array(initialHidden.size) { !initialHidden[it] })
    }

    val scrollState = rememberScrollState()
    val groups = allControls.groupBy { it.group }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CS_BgDeep)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(CS_AccentBlue.copy(alpha = 0.3f), CS_AccentViolet.copy(alpha = 0.2f))
                ),
                RoundedCornerShape(24.dp)
            )
    ) {
        Column {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF0E1020), CS_BgDeep))
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text          = "CONTROLS",
                        color         = CS_TextSub,
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontFamily    = FontFamily.Monospace
                    )
                    Text(
                        text       = "Show / Hide Elements",
                        color      = CS_TextPrim,
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Done button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(CS_AccentBlue.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, CS_AccentBlue.copy(alpha = 0.4f), CircleShape)
                        .clickable(onClick = onDone),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Done",
                        tint     = CS_AccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Divider
            Box(Modifier.fillMaxWidth().height(1.dp).background(CS_Div))

            // ── Scrollable groups ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groups.forEach { (groupName, items) ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Group label
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(items.first().accent, CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text          = groupName.uppercase(),
                                color         = items.first().accent,
                                fontSize      = 10.sp,
                                fontWeight    = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                fontFamily    = FontFamily.Monospace
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                items.first().accent.copy(alpha = 0.3f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }

                        // Chips in a flow row
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement   = Arrangement.spacedBy(8.dp)
                        ) {
                            items.forEachIndexed { i, control ->
                                ControlChip(
                                    control   = control,
                                    isVisible = visible[control.index],
                                    index     = i,
                                    onClick   = {
                                        val newVisible = !visible[control.index]
                                        visible[control.index] = newVisible
                                        onToggle(control.index, !newVisible)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ── Control chip ──────────────────────────────────────────────────────────────
@Composable
private fun ControlChip(
    control: ControlItem,
    isVisible: Boolean,
    index: Int,
    onClick: () -> Unit
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 40L)
        anim.animateTo(1f, tween(300, easing = EaseOutCubic))
    }

    val accent = control.accent

    Box(
        modifier = Modifier
            .graphicsLayer { alpha = anim.value; translationY = (1f - anim.value) * 12f }
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isVisible)
                    Brush.linearGradient(listOf(accent.copy(alpha = 0.2f), accent.copy(alpha = 0.08f)))
                else
                    Brush.linearGradient(listOf(CS_BgChip, CS_BgChip))
            )
            .border(
                1.dp,
                accent.copy(alpha = if (isVisible) 0.5f else 0.15f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Check / hide indicator
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        if (isVisible) accent.copy(alpha = 0.2f) else CS_Div,
                        CircleShape
                    )
                    .border(
                        1.dp,
                        if (isVisible) accent.copy(alpha = 0.6f)
                        else CS_TextSub.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isVisible) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint     = accent,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            // Drawable icon if available, otherwise text label
            if (control.drawableRes != null) {
                Image(
                    painter = painterResource(id = control.drawableRes),
                    contentDescription = control.label,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Text(
                    text       = control.label,
                    color      = if (isVisible) accent else CS_TextSub,
                    fontSize   = 12.sp,
                    fontWeight = if (isVisible) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}