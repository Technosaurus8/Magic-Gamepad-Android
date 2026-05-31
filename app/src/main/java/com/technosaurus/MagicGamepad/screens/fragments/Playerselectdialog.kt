package com.technosaurus.MagicGamepad.screens.fragments

import android.app.Dialog
import android.os.Bundle
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import com.technosaurus.MagicGamepad.screens.RemoteHost
import com.technosaurus.MagicGamepad.util.FullscreenHelper

// ── Palette ───────────────────────────────────────────────────────────────────
private val PD_BgDeep    = Color(0xFF07080F)
private val PD_BgCard    = Color(0xFF0F1120)
private val PD_Div       = Color(0xFF252A45)
private val PD_TextPrim  = Color(0xFFECEEFF)
private val PD_TextSub   = Color(0xFF8A9CC8)

private val playerAccents = listOf(
    Color(0xFF3D8EFF), // P1 — blue
    Color(0xFF00E5A0), // P2 — green
    Color(0xFFFF6FD8), // P3 — pink
    Color(0xFFFFB547), // P4 — amber
)

class PlayerSelectDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "player_select"

        fun newInstance(): PlayerSelectDialogFragment = PlayerSelectDialogFragment()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false
        val host = requireActivity() as RemoteHost
        val dialog = Dialog(requireContext())
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        fun onPlayerChosen(player: String) {
            host.setPlayer(player)
            FullscreenHelper.setFullscreen(requireActivity())
            dismiss()
        }

        // Handle back button
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK &&
                event.action == android.view.KeyEvent.ACTION_UP
            ) {
                onPlayerChosen("p1")
                true
            } else {
                false
            }
        }

        val composeView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    PlayerSelectContent(onSelect = { onPlayerChosen(it) })
                }
            }
        }
        dialog.setContentView(composeView)
        return dialog
    }
}

@Composable
private fun PlayerSelectContent(onSelect: (String) -> Unit) {
    val players = listOf(
        Triple("p1", "Player 1", playerAccents[0]),
        Triple("p2", "Player 2", playerAccents[1]),
        Triple("p3", "Player 3", playerAccents[2]),
        Triple("p4", "Player 4", playerAccents[3]),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PD_BgDeep)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        playerAccents[0].copy(alpha = 0.3f),
                        playerAccents[2].copy(alpha = 0.2f)
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)  // was 24.dp all sides
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)  // was 16.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text          = "SELECT PLAYER",
                    color         = PD_TextSub,
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily    = FontFamily.Monospace
                )
                Spacer(Modifier.height(2.dp))  // was 4.dp
                Text(
                    text       = "Who are you?",
                    color      = PD_TextPrim,
                    fontSize   = 18.sp,           // was 20.sp
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PD_Div)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {  // was 10.dp
                players.chunked(2).forEach { rowPlayers ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)  // was 10.dp
                    ) {
                        rowPlayers.forEach { (id, label, accent) ->
                            PlayerButton(
                                label    = label,
                                accent   = accent,
                                modifier = Modifier.weight(1f),
                                onClick  = { onSelect(id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerButton(
    label: String,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(350, easing = EaseOutCubic)) }

    Box(
        modifier = modifier
            .graphicsLayer { alpha = anim.value; translationY = (1f - anim.value) * 20f }
            .clip(RoundedCornerShape(14.dp))          // was 16.dp
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.15f), accent.copy(alpha = 0.05f))
                )
            )
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),               // was 18.dp
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)  // was 8.dp
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)                      // was 40.dp
                    .background(accent.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, accent.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    tint     = accent,
                    modifier = Modifier.size(18.dp)   // was 22.dp
                )
            }
            Text(
                text       = label,
                color      = PD_TextPrim,
                fontSize   = 12.sp,                   // was 13.sp
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier
                    .size(5.dp)                       // was 6.dp
                    .background(accent, CircleShape)
            )
        }
    }
}