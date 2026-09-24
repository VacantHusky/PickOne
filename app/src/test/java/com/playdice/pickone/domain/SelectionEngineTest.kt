package com.playdice.pickone.domain

import com.playdice.pickone.model.ChoiceItem
import com.playdice.pickone.model.Scene
import com.playdice.pickone.model.SelectorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionEngineTest {
    private val engine = SelectionEngine()

    @Test
    fun weightedBoundariesMapToExpectedChoices() {
        val choices = listOf(
            ChoiceItem(name = "A", weight = 1),
            ChoiceItem(name = "B", weight = 2),
            ChoiceItem(name = "C", weight = 3),
        )
        val scene = Scene(name = "test", choices = choices)

        assertEquals("A", engine.select(scene, fixed(0)).selected.name)
        assertEquals("B", engine.select(scene, fixed(1)).selected.name)
        assertEquals("B", engine.select(scene, fixed(2)).selected.name)
        assertEquals("C", engine.select(scene, fixed(3)).selected.name)
        assertEquals("C", engine.select(scene, fixed(5)).selected.name)
    }

    @Test
    fun weightsAreReducedByGreatestCommonDivisor() {
        val scene = Scene(
            name = "test",
            choices = listOf(
                ChoiceItem(name = "A", weight = 2),
                ChoiceItem(name = "B", weight = 4),
            ),
        )

        val outcome = engine.select(scene, fixed(2))

        assertEquals(listOf(1, 2), outcome.normalizedWeights)
        assertEquals("B", outcome.selected.name)
    }

    @Test
    fun diceUsesSmallestExactNumberOfUnits() {
        assertEquals(1, SelectionEngine.unitCountFor(total = 6, base = 6))
        assertEquals(2, SelectionEngine.unitCountFor(total = 7, base = 6))
        assertEquals(3, SelectionEngine.unitCountFor(total = 200, base = 6))
    }

    @Test
    fun coinsUseSmallestExactNumberOfUnits() {
        assertEquals(1, SelectionEngine.unitCountFor(total = 2, base = 2))
        assertEquals(3, SelectionEngine.unitCountFor(total = 5, base = 2))
        assertEquals(8, SelectionEngine.unitCountFor(total = 200, base = 2))
    }

    @Test
    fun diceRejectsCombinationOutsideWeightRange() {
        val scene = Scene(
            name = "dice",
            selectorType = SelectorType.DICE,
            choices = listOf(
                ChoiceItem(name = "A", weight = 1),
                ChoiceItem(name = "B", weight = 1),
                ChoiceItem(name = "C", weight = 1),
                ChoiceItem(name = "D", weight = 1),
            ),
        )
        val values = ArrayDeque(listOf(5, 2))

        val outcome = engine.select(scene, RandomSource { values.removeFirst() })

        assertEquals(1, outcome.rejectedRolls)
        assertEquals(listOf(2), outcome.unitValues)
        assertEquals("C", outcome.selected.name)
    }

    @Test
    fun coinCombinationIsReturnedWithSelection() {
        val scene = Scene(
            name = "coin",
            selectorType = SelectorType.COIN,
            choices = listOf(
                ChoiceItem(name = "A", weight = 1),
                ChoiceItem(name = "B", weight = 2),
                ChoiceItem(name = "C", weight = 1),
            ),
        )
        val values = ArrayDeque(listOf(1, 0))

        val outcome = engine.select(scene, RandomSource { values.removeFirst() })

        assertEquals(listOf(1, 0), outcome.unitValues)
        assertEquals(2, outcome.sampledIndex)
        assertEquals("B", outcome.selected.name)
        assertTrue(outcome.rejectedRolls == 0)
    }

    @Test
    fun wheelStopAlwaysPlacesSelectedMidpointAtTopPointer() {
        val weights = listOf(1, 2, 3, 4)
        weights.indices.forEach { selectedIndex ->
            val rotation = SelectionVisualMapping.wheelStopRotation(weights, selectedIndex, power = .67f)
            val midpoint = (weights.take(selectedIndex).sum() + weights[selectedIndex] / 2f) / weights.sum() * 360f
            val finalAngle = normalizeDegrees(-90f + midpoint + rotation)
            assertEquals(270f, finalAngle, .001f)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsWeightedTotalThatDoesNotFitRandomBound() {
        val scene = Scene(
            name = "overflow",
            choices = listOf(
                ChoiceItem(name = "A", weight = Int.MAX_VALUE),
                ChoiceItem(name = "B", weight = Int.MAX_VALUE - 1),
            ),
        )
        engine.select(scene)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnitCountOverflow() {
        SelectionEngine.unitCountFor(Int.MAX_VALUE, 2)
    }

    private fun fixed(value: Int) = RandomSource { bound -> value.coerceIn(0, bound - 1) }

    private fun normalizeDegrees(value: Float): Float = ((value % 360f) + 360f) % 360f
}
