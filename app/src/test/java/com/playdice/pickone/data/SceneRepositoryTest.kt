package com.playdice.pickone.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SceneRepositoryTest {
    @Test
    fun duplicateNameIsLimitedToSceneNameMaximum() {
        val result = duplicateName("a".repeat(40))
        assertEquals(40, result.length)
        assertEquals("a".repeat(36) + " · 2", result)
    }
}
