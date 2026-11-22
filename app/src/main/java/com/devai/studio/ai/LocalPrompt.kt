package com.devai.studio.ai

object LocalPrompt {

    private const val RUSSIAN =
        "Ты DevAI Studio. Отвечай на русском языке. Отвечай только на последнее сообщение " +
            "пользователя. Не придумывай задания, не делай разбор и не пиши код, если об этом " +
            "не просят. Если пользователь просто здоровается, ответь: Привет! Чем помочь?"

    /**
     * LiteRT-LM applies the model's own chat template and rejects control tokens in the
     * input. Keep the prompt as plain text, even for models imported with a legacy format.
     */
    fun build(messages: List<AiMessage>, @Suppress("UNUSED_PARAMETER") format: String): String {
        val system = PromptSanitizer.clean(
            messages.firstOrNull { it.role == "system" }?.content.orEmpty()
        ).trim()
        val turns = messages
            .filter { it.role == "user" || it.role == "assistant" }
            .filterNot { it.role == "assistant" && ResponseGuard.isMeta(it.content) }
            .takeLast(6)
            .map { it.copy(content = PromptSanitizer.clean(it.content).take(4000)) }

        return buildString {
            append("System: ")
            if (system.isNotBlank()) append(system).append(' ')
            append(RUSSIAN).append("\n\n")
            turns.forEach { m ->
                append(if (m.role == "user") "User: " else "Assistant: ")
                    .append(m.content).append("\n\n")
            }
            append("Assistant:")
        }
    }
}
