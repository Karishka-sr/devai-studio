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
    fun chatmlFormatsTurnsAndOpensAssistant() {
        val prompt = LocalPrompt.build(messages, "chatml")
        assertTrue(prompt.startsWith("<|im_start|>system\nТы помощник<|im_end|>"))
        assertTrue(prompt.contains("<|im_start|>user\nПривет<|im_end|>"))
        assertTrue(prompt.endsWith("<|im_start|>assistant\n"))
    }

    @Test
    fun zephyrUsesItsOwnTags() {
        val prompt = LocalPrompt.build(messages, "zephyr")
        assertTrue(prompt.startsWith("<|system|>\nТы помощник</s>"))
        assertTrue(prompt.endsWith("<|assistant|>\n"))
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
}
