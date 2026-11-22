package com.devai.studio.ai

import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPromptTest {

    private val messages = listOf(
        AiMessage("system", "Ты помощник"),
        AiMessage("user", "Привет"),
        AiMessage("assistant", "Здравствуйте"),
        AiMessage("user", "Как дела?")
    )

    @Test
    fun chatmlIsSentAsPlainText() {
        val prompt = LocalPrompt.build(messages, "chatml")
        assertTrue(prompt.startsWith("System: Ты помощник"))
        assertTrue(prompt.contains("User: Привет"))
        assertTrue(prompt.endsWith("Assistant:"))
        assertTrue(!prompt.contains("<|"))
    }

    @Test
    fun zephyrIsSentAsPlainText() {
        val prompt = LocalPrompt.build(messages, "zephyr")
        assertTrue(prompt.startsWith("System: Ты помощник"))
        assertTrue(prompt.endsWith("Assistant:"))
        assertTrue(!prompt.contains("<|"))
    }

    @Test
    fun genericKeepsSystemAndTurns() {
        val prompt = LocalPrompt.build(messages, "generic")
        assertTrue(prompt.contains("System: Ты помощник"))
        assertTrue(prompt.endsWith("Assistant:"))
    }

    @Test
    fun historyIsLimitedToLastSixTurns() {
        val many = (1..10).map { AiMessage("user", "u$it") }
        val prompt = LocalPrompt.build(many, "chatml")
        assertTrue(prompt.contains("u5"))
        assertTrue(prompt.contains("u10"))
        assertTrue(!prompt.contains("u4"))
    }

    @Test
    fun controlTokensAreRemovedFromHistory() {
        val prompt = LocalPrompt.build(
            listOf(
                AiMessage("system", "<bos>Ты помощник<eos>"),
                AiMessage("user", "Привет<start_of_turn>"),
            ),
            "gemma",
        )
        assertTrue(prompt.contains("System: Ты помощник"))
        assertTrue(prompt.contains("User: Привет"))
        assertTrue(!prompt.contains("<bos>"))
        assertTrue(!prompt.contains("<eos>"))
        assertTrue(!prompt.contains("<start_of_turn>"))
    }
}
