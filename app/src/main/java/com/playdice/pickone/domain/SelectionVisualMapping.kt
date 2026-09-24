package com.playdice.pickone.domain

import kotlin.math.roundToInt

object SelectionVisualMapping {
    /**
     * Returns a clockwise wheel rotation that places the selected segment's
     * midpoint at the top pointer. The initial wheel starts at -90 degrees.
     */
    fun wheelStopRotation(
        weights: List<Int>,
        selectedIndex: Int,
        power: Float,
    ): Float {
        require(weights.isNotEmpty() && weights.all { it > 0 })
        require(selectedIndex in weights.indices)
        val total = weights.sum().toFloat()
        val before = weights.take(selectedIndex).sum()
        val midpoint = (before + weights[selectedIndex] / 2f) / total * 360f
        val fullTurns = 3 + (power.coerceIn(.12f, 1f) * 4f).roundToInt()
        return fullTurns * 360f + 360f - midpoint
    }
}
