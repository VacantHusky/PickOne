package com.playdice.pickone.data

import com.playdice.pickone.model.SelectorType
import com.playdice.pickone.model.SoundChoice
import org.junit.Assert.assertEquals
import org.junit.Test

class EnumParsingTest {
    @Test
    fun unknownStoredValuesUseStableDefaults() {
        assertEquals(SelectorType.WHEEL, "future_selector".enumOrDefault(SelectorType.WHEEL))
        assertEquals(SoundChoice.CRISP, "future_sound".enumOrDefault(SoundChoice.CRISP))
        assertEquals(SelectorType.WHEEL, null.enumOrDefault(SelectorType.WHEEL))
    }

    @Test
    fun enumValuesIgnoreStorageWhitespaceAndCase() {
        assertEquals(SelectorType.DICE, " dice ".enumOrDefault(SelectorType.WHEEL))
    }
}
