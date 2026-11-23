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
    fun degenerateLoopIsRejected() {
        val loop = "повторяю одно и то же предложение снова и снова ".repeat(20)
        assertTrue(ResponseGuard.isDegenerate(loop))
    }

    @Test
    fun variedAnswerIsNotDegenerate() {
        val answer = (1..30).joinToString(" ") { "слово$it" }
        assertFalse(ResponseGuard.isDegenerate(answer))
    }

    @Test
    fun localPromptDropsOldestTurnsToFitBudget() {
        val messages = (1..20).map { AiMessage("user", "длинный вопрос $it " + "контекст ".repeat(80)) } +
            AiMessage("assistant", "ответ") +
            AiMessage("user", "последний вопрос")
        val prompt = LocalPrompt.build(messages, "chatml", maxChars = 2000)
        assertTrue(prompt.length < 2300)
        assertTrue(prompt.contains("последний вопрос"))
        assertFalse(prompt.contains("длинный вопрос 1 "))
    }

    @Test
    fun searchResultsSurviveLocalPromptBudget() {
        val system = "Ты помощник. " + "Правило. ".repeat(100) +
            "\n\nАктуальные результаты поиска:\n" + "1. Свежий факт https://example.com/source ".repeat(30)
        val prompt = LocalPrompt.build(
            listOf(
                AiMessage("system", system),
                AiMessage("user", "Что нового?"),
            ),
            "chatml",
            maxChars = 2000,
        )
        assertTrue(prompt.contains("Актуальные результаты поиска"))
        assertTrue(prompt.contains("Свежий факт"))
        assertTrue(prompt.contains("User: Что нового?"))
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
