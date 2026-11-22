package com.devai.studio.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PromptSanitizerTest {

    @Test
    fun removesBosAndTurnTokens() {
        val input = "<bos>Привет<start_of_turn>user\nКак дела?<end_of_turn><eos>"
        assertEquals("Приветuser\nКак дела?", PromptSanitizer.clean(input))
    }

    @Test
    fun removesChatMlTokens() {
        val input = "<|im_start|>system\nТы ассистент<|im_end|><|endoftext|>"
        assertEquals("system\nТы ассистент", PromptSanitizer.clean(input))
    }

    @Test
    fun keepsOrdinaryXmlTags() {
        assertEquals("<user>", PromptSanitizer.clean("<user>"))
    }

    @Test
    fun keepsOrdinaryCodeAndText() {
        val input = "if (a < b) println(\"<tag>\")\n<bos>"
        val cleaned = PromptSanitizer.clean(input)
        assertEquals("if (a < b) println(\"<tag>\")\n", cleaned)
        assertFalse(cleaned.contains("<bos>"))
    }
}
