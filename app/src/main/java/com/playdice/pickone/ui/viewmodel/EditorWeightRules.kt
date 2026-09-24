package com.playdice.pickone.ui.viewmodel

/** Shared weight boundaries for editor controls and persistence updates. */
object EditorWeightRules {
    const val MIN_WEIGHT = 1
    const val MAX_WEIGHT = 10

    fun canDecrease(weight: Int): Boolean = weight > MIN_WEIGHT

    fun canIncrease(weight: Int): Boolean = weight < MAX_WEIGHT

    fun adjust(weight: Int, delta: Int): Int = (weight + delta).coerceIn(MIN_WEIGHT, MAX_WEIGHT)
}
