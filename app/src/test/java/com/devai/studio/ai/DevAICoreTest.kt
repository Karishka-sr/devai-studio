package com.devai.studio.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DevAICoreTest {
    @Test
    fun coreKeepsOwnIdentity() {
        assertEquals("0.2", DevAICore.VERSION)
        assertTrue(DevAICore.SYSTEM_PROMPT.contains("DevAI Core"))
    }

    @Test
    fun promptSearchHandlesPhrase() {
        assertTrue(DevAICore.findPrompts("Kotlin API").isNotEmpty())
        assertTrue(DevAICore.findPrompts("тестов").isNotEmpty())
    }
}
