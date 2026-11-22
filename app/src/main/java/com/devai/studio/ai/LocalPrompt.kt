package com.devai.studio.ai

object LocalPrompt {

    private const val RUSSIAN =
        "Отвечай на русском языке. Не начинай ответ с английских инструкций или шаблонов."

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
