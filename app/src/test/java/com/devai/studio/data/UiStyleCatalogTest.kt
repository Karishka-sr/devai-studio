package com.devai.studio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiStyleCatalogTest {

    @Test
    fun catalogHasAtLeastTwentyFourStyles() {
        assertTrue(UiStyleCatalog.all.size >= 24)
    }

    @Test
    fun idsAndNamesAreUnique() {
        assertEquals(UiStyleCatalog.all.size, UiStyleCatalog.all.map { it.id }.distinct().size)
        assertEquals(UiStyleCatalog.all.size, UiStyleCatalog.all.map { it.name }.distinct().size)
    }

    @Test
    fun requestedStylesExist() {
        val ids = UiStyleCatalog.all.map { it.id }.toSet()
        listOf(
            "chatgpt", "claude", "glass", "liquid-glass", "material-you",
            "material-expressive", "gemini", "perplexity", "terminal",
        ).forEach { assertTrue("missing $it", it in ids) }
    }

    @Test
    fun everyStyleHasUsablePalette() {
        UiStyleCatalog.all.forEach { style ->
            assertTrue(style.name.isNotBlank())
            assertTrue(style.description.isNotBlank())
            assertTrue(style.corner in 0..40)
            assertNotEquals(style.light.primary, style.light.background)
            assertNotEquals(style.dark.primary, style.dark.background)
            assertNotEquals(style.light.userBubble, style.light.aiBubble)
        }
    }

    @Test
    fun unknownIdFallsBackToClassicAndNextCycles() {
        assertEquals(UiStyleCatalog.CLASSIC_ID, UiStyleCatalog.byId("missing").id)
        val first = UiStyleCatalog.all.first()
        assertEquals(UiStyleCatalog.all[1].id, UiStyleCatalog.next("missing").id)
        assertEquals(first.id, UiStyleCatalog.next(UiStyleCatalog.all.last().id).id)
    }
}
