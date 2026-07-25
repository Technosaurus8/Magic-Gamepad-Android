package com.technosaurus.MagicGamepad.util

data class CustomLayoutProfile(
    val id: String,
    val name: String,
    val positions: Array<IntArray>,
    val sizes: Array<IntArray>,
    val hiddenStates: BooleanArray,
)
