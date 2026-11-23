package com.devai.studio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptLibraryTest {
    @Test
    fun megaLibraryHasAtLeastOneThousandPrompts() {
        assertTrue("expected >= 1000 prompts, got ${PromptLibrary.items.size}", PromptLibrary.items.size >= 1000)
        assertEquals(
            "prompt titles must be unique",
            PromptLibrary.items.size,
            PromptLibrary.items.map { it.title }.distinct().size,
        )
        assertTrue(PromptLibrary.items.all { it.prompt.length > 80 })
    }

    @Test
    fun searchFindsLanguageAndTask() {
        assertTrue(PromptLibrary.search("Rust").any { it.title.contains("Rust") })
        assertTrue(PromptLibrary.search("Unit-тесты").any { it.title.contains("Unit-тесты") })
        assertEquals(PromptLibrary.items.size, PromptLibrary.search("  ").size)
    }
}
