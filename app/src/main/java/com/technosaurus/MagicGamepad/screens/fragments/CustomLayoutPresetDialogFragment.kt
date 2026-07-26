package com.technosaurus.MagicGamepad.screens.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check

import androidx.compose.material3.Icon

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import com.technosaurus.MagicGamepad.util.CustomLayoutStore

private val PL_BgDeep = Color(0xFF07080F)
private val PL_BgCard = Color(0xFF0F1120)
private val PL_BgChip = Color(0xFF131628)
private val PL_AccentPink = Color(0xFFFF6FD8)
private val PL_TextPrim = Color(0xFFECEEFF)

private val PL_Div = Color(0xFF181B30)

class CustomLayoutPresetDialogFragment : DialogFragment() {

    private var currentId: String = ""
    private var onSelect: ((String) -> Unit)? = null
    private var onSelectedDismiss: (() -> Unit)? = null
    private var selectedDismissFired = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        isCancelable = false
        currentId = arguments?.getString(ARG_CURRENT_ID).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PresetPickerContent(
                    currentId = currentId,
                    onSelect = { id ->
                        onSelect?.invoke(id)
                        dismissAfterSelection()
                    },

                )
            }
        }
    }



    override fun onStart() {
        super.onStart()
        dialog?.setCanceledOnTouchOutside(false)
        // Handle back button
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK &&
                event.action == android.view.KeyEvent.ACTION_UP
            ) {
                requireActivity().finish()
                true
            } else {
                false
            }
        }
    }

    private fun dismissAfterSelection() {
        dismissAllowingStateLoss()
        if (selectedDismissFired) return
        selectedDismissFired = true
        onSelectedDismiss?.invoke()
    }
    @Composable
    private fun PresetPickerContent(
        currentId: String,
        onSelect: (String) -> Unit,
    ) {
        val profiles = CustomLayoutStore.getProfiles(requireContext())

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PL_BgDeep.copy(alpha = 0.92f))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PL_BgCard)
                    .border(1.dp, PL_Div, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Select layout",
                    color = PL_TextPrim,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    profiles.forEach { profile ->
                        val selected = profile.id == currentId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) PL_AccentPink.copy(alpha = 0.12f) else PL_BgChip,
                                )
                                .clickable { onSelect(profile.id) }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                profile.name,
                                color = PL_TextPrim,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                            )
                            if (selected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = PL_AccentPink,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "custom_layout_preset"
        private const val ARG_CURRENT_ID = "current_id"

        fun newInstance(
            currentId: String,
            onSelect: (String) -> Unit,
            onSelectedDismiss: Runnable? = null,
        ): CustomLayoutPresetDialogFragment {
            return CustomLayoutPresetDialogFragment().apply {
                arguments = Bundle().apply { putString(ARG_CURRENT_ID, currentId) }
                this.onSelect = onSelect
                this.onSelectedDismiss = onSelectedDismiss?.let { runnable -> { runnable.run() } }
            }
        }
    }
}