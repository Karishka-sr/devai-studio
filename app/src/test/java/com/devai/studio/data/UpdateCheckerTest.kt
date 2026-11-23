package com.devai.studio.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun detectsNewerVersions() {
        assertTrue(UpdateChecker.isNewer("v3.6.2", "3.6.1"))
        assertTrue(UpdateChecker.isNewer("4.0.0", "3.9.9"))
        assertTrue(UpdateChecker.isNewer("3.6.1", "3.6"))
        assertTrue(UpdateChecker.isNewer("3.10.0", "3.9.9"))
    }

    @Test
    fun ignoresSameAndOlderVersions() {
        assertFalse(UpdateChecker.isNewer("v3.6.1", "3.6.1"))
        assertFalse(UpdateChecker.isNewer("3.5.9", "3.6.0"))
        assertFalse(UpdateChecker.isNewer("3.6.0-beta", "3.6.0"))
    }
}
