package com.devai.studio.data

import com.devai.studio.ai.AiFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderCatalogTest {
    @Test
    fun devaiCore_isListedAsOpenAiCompatibleProvider() {
        val provider = ProviderCatalog.find("devai-core")
        assertNotNull(provider)
        assertEquals("DevAI Core", provider!!.name)
        assertEquals(Category.FULLY_FREE, provider.category)
        assertEquals(AiFormat.OPENAI_COMPAT, provider.format)
        assertEquals(listOf("devai-core-0.1"), provider.models)
        assertTrue(provider.baseUrl.endsWith("/v1"))
        assertTrue(provider.getKeyUrl.contains("github.io/devai-studio"))
    }

    @Test
    fun providerIdsAreUnique() {
        val ids = ProviderCatalog.all.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }
}
