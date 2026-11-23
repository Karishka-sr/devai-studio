package com.devai.studio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeLanguagesTest {
    @Test
    fun catalogHasAtLeastFiftyLanguages() {
        assertTrue(CodeLanguages.all.size >= 50)
        assertEquals(CodeLanguages.all.size, CodeLanguages.all.map { it.id }.distinct().size)
        assertEquals(CodeLanguages.all.size, CodeLanguages.all.map { it.name }.distinct().size)
    }

    @Test
    fun everyLanguageHasUsableMetadata() {
        CodeLanguages.all.forEach { language ->
            assertTrue(language.id.isNotBlank())
            assertTrue(language.name.isNotBlank())
            assertTrue(language.extension.isNotBlank())
            assertTrue(language.description.isNotBlank())
            assertTrue(language.example.isNotBlank())
        }
    }

    @Test
    fun defaultAndInstructionPointAtKotlin() {
        assertEquals("kotlin", CodeLanguages.DEFAULT_ID)
        assertEquals("Kotlin", CodeLanguages.default.name)
        assertEquals("Python", CodeLanguages.byId("python")?.name)
        assertTrue(CodeLanguages.instruction(CodeLanguages.default).contains("Kotlin"))
    }
}
