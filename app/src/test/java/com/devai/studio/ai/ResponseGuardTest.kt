package com.devai.studio.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseGuardTest {

    private val leakedTask = """
        You have a list of strings, each representing a sequence of bytes. Your task is to
        write a function that takes a list of strings and returns a list of strings that
        match a specific pattern.
    """.trimIndent()

    @Test
    fun detectsLeakedCodingTask() {
        assertTrue(ResponseGuard.isMeta(leakedTask))
    }

    @Test
    fun normalAnswerIsNotMeta() {
        assertFalse(ResponseGuard.isMeta("Привет! Чем помочь?"))
        assertFalse(ResponseGuard.isMeta("Вот функция для сортировки списка:\n\n```kotlin\nfun sort(xs: List<Int>) = xs.sorted()\n```"))
    }

    @Test
    fun simpleGreetingGetsShortReply() {
        assertEquals("Привет! Чем помочь?", ResponseGuard.greetingReply("Привет!"))
        assertEquals("Привет! Чем помочь?", ResponseGuard.greetingReply("  hello  "))
        assertEquals(null, ResponseGuard.greetingReply("Привет, напиши функцию"))
    }

    @Test
    fun gateBlocksLeakedTaskBeforeItReachesUi() {
        val shown = StringBuilder()
        val gate = ResponseGate { shown.append(it) }
        val keepGoing = gate.onChunk(leakedTask)
        gate.finish()
        assertFalse(keepGoing)
        assertTrue(gate.blocked)
        assertEquals("", shown.toString())
    }

    @Test
    fun gateForwardsNormalLongAnswer() {
        val shown = StringBuilder()
        val gate = ResponseGate { shown.append(it) }
        val answer = "Это обычный ответ на вопрос пользователя. ".repeat(8)
        assertTrue(gate.onChunk(answer))
        gate.finish()
        assertFalse(gate.blocked)
        assertEquals(answer, shown.toString())
    }

    @Test
    fun gateEmitsShortAnswerOnFinish() {
        val shown = StringBuilder()
        val gate = ResponseGate { shown.append(it) }
        assertTrue(gate.onChunk("Привет! Чем помочь?"))
        gate.finish()
        assertFalse(gate.blocked)
        assertEquals("Привет! Чем помочь?", shown.toString())
    }

    @Test
    fun promptDropsMetaAssistantTurnsAndAddsRussianRule() {
        val prompt = LocalPrompt.build(
            listOf(
                AiMessage("system", "Ты помощник"),
                AiMessage("user", "Привет"),
                AiMessage("assistant", leakedTask),
                AiMessage("user", "Как дела?"),
            ),
            "chatml",
        )
        assertTrue(prompt.contains("Не придумывай задания"))
        assertTrue(prompt.contains("User: Как дела?"))
        assertFalse(prompt.contains("You have a list of strings"))
    }
}
