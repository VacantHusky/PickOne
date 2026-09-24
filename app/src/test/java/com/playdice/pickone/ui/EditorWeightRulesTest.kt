package com.playdice.pickone.ui

import com.playdice.pickone.ui.viewmodel.EditorWeightRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorWeightRulesTest {
    @Test
    fun controlsAreDisabledAtWeightBounds() {
        assertFalse(EditorWeightRules.canDecrease(EditorWeightRules.MIN_WEIGHT))
        assertTrue(EditorWeightRules.canIncrease(EditorWeightRules.MIN_WEIGHT))
        assertTrue(EditorWeightRules.canDecrease(EditorWeightRules.MAX_WEIGHT))
        assertFalse(EditorWeightRules.canIncrease(EditorWeightRules.MAX_WEIGHT))
    }

    @Test
    fun adjustmentNeverLeavesAllowedRange() {
        assertEquals(EditorWeightRules.MIN_WEIGHT, EditorWeightRules.adjust(1, -1))
        assertEquals(EditorWeightRules.MAX_WEIGHT, EditorWeightRules.adjust(10, 1))
        assertEquals(6, EditorWeightRules.adjust(5, 1))
    }
}
