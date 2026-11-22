package com.devai.studio.ai

/**
 * Small local models sometimes continue a training sample instead of answering.
 * They may output an invented coding task such as "You have a list of strings...".
 * This guard keeps such text out of the chat and provides a normal fallback.
 */
object ResponseGuard {

    private val metaMarkers = listOf(
        "you have a list of strings",
        "your task is to write",
        "your code should pass",
        "the pattern is a string",
        "write a function that takes a list",
        "implement a function that takes a list",
        "here is the task",
        "here's the task",
        "task: write a function",
        "задача: напиши",
        "твоя задача —",
        "твоя задача:",
        "разбор задачи",
        "вот задача",
        "напиши функцию, которая принимает",
    )

    private val greetings = setOf(
        "привет",
        "приветик",
        "здравствуй",
        "здравствуйте",
        "хай",
        "ку",
        "hello",
        "hi",
        "добрый день",
        "добрый вечер",
        "доброе утро",
    )

    fun isMeta(text: String): Boolean {
        val normalized = normalize(text).take(600)
        return normalized.isNotEmpty() && metaMarkers.any { normalized.contains(it) }
    }

    fun greetingReply(text: String): String? =
        if (normalize(text).trim('!', '.', ',', '?', ' ') in greetings) {
            "Привет! Чем помочь?"
        } else {
            null
        }

    fun fallback(text: String): String =
        greetingReply(text)
            ?: "Не удалось получить нормальный ответ. Переформулируйте вопрос или выберите другую локальную модель."

    private fun normalize(text: String): String =
        text.lowercase()
            .replace(Regex("[`*_#>]+"), " ")
            .replace(Regex("[^\\p{L}\\p{N}' ]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}

/**
 * Buffers the beginning of a response so a leaked training task is never shown.
 * Once the prefix looks safe, chunks are forwarded to the UI unchanged.
 */
class ResponseGate(private val emit: (String) -> Unit) {
    private val pending = StringBuilder()
    private var released = false
    var blocked = false
        private set

    fun onChunk(chunk: String): Boolean {
        if (blocked) return false
        if (released) {
            emit(chunk)
            return true
        }
        pending.append(chunk)
        val text = pending.toString()
        if (ResponseGuard.isMeta(text)) {
            blocked = true
            return false
        }
        if (text.length >= 220) {
            released = true
            emit(text)
            pending.clear()
        }
        return true
    }

    fun finish() {
        if (blocked || released) return
        val text = pending.toString()
        if (ResponseGuard.isMeta(text)) {
            blocked = true
            return
        }
        if (text.isNotBlank()) emit(text)
        released = true
        pending.clear()
    }
}
