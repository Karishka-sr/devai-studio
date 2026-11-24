package com.devai.studio.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageAssetsTest {
    private val dir = File("../languages")

    @Test
    fun languageCatalogLivesOutsideAssets() {
        assertTrue("languages/ is missing", dir.isDirectory)
        assertFalse(
            "language packs must not be bundled into assets",
            File("src/main/assets/languages").exists(),
        )
        val files = dir.listFiles { file -> file.extension == "json" }.orEmpty()
        assertTrue("expected at least 175 language files, got ${files.size}", files.size >= 175)
        assertEquals(files.size, files.map { it.name }.distinct().size)
    }

    @Test
    fun everyLanguageFileHasRequiredFields() {
        dir.listFiles { file -> file.extension == "json" }.orEmpty().forEach { file ->
            val text = file.readText()
            listOf("\"id\"", "\"name\"", "\"short\"", "\"extension\"", "\"example\"", "\"order\"")
                .forEach { field ->
                    assertTrue("${file.name} is missing $field", text.contains(field))
                }
        }
    }
}
