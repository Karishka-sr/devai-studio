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
    fun namedStylesUseRealStructuralLayouts() {
        val expected = mapOf(
            "chatgpt" to StyleLayout.CHATGPT,
            "claude" to StyleLayout.CLAUDE,
            "gemini" to StyleLayout.GEMINI,
            "glass" to StyleLayout.GLASS,
            "liquid-glass" to StyleLayout.LIQUID_GLASS,
            "material-you" to StyleLayout.MATERIAL_YOU,
            "material-design" to StyleLayout.MATERIAL_DESIGN,
            "material-expressive" to StyleLayout.MATERIAL_EXPRESSIVE,
            "terminal" to StyleLayout.TERMINAL,
        )
        expected.forEach { (id, layout) ->
            assertEquals("$id must use $layout", layout, UiStyleCatalog.byId(id).layout)
        }
        assertTrue(expected.values.distinct().size >= 9)
    }

    @Test
    fun glassStylesAreBackedByRealBlurBackground() {
        listOf("glass", "liquid-glass").forEach { id ->
            val style = UiStyleCatalog.byId(id)
            assertEquals(StyleBackground.GLASS, style.background)
            assertTrue(style.layout == StyleLayout.GLASS || style.layout == StyleLayout.LIQUID_GLASS)
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
