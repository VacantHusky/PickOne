package com.playdice.pickone.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionTest {
    @Test
    fun comparesNumericVersionSegments() {
        assertTrue(UpdateVersion.isNewer("1.0.1", "1.0.0"))
        assertTrue(UpdateVersion.isNewer("2.0", "1.99.99"))
        assertFalse(UpdateVersion.isNewer("1.0.0", "1.0.0"))
        assertFalse(UpdateVersion.isNewer("1.0.0", "1.0.1"))
    }

    @Test
    fun acceptsOptionalVersionPrefix() {
        assertTrue(UpdateVersion.isNewer("v1.2.0", "1.1.9"))
    }
}
