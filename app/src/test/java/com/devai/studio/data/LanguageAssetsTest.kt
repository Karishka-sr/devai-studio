package com.devai.studio.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageAssetsTest {
    private val dir = File("src/main/assets/languages")

    @Test
    fun languageCatalogIsStoredInBuildFiles() {
        assertTrue("assets/languages is missing", dir.isDirectory)
        val files = dir.listFiles { file -> file.extension == "json" }.orEmpty()
        assertTrue("expected at least 75 language files, got ${files.size}", files.size >= 75)
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
