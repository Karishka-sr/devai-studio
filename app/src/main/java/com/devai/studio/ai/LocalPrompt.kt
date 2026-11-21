package com.devai.studio.ai

object LocalPrompt {

    fun build(messages: List<AiMessage>, format: String): String {
        val system = messages.firstOrNull { it.role == "system" }?.content?.trim().orEmpty()
        val turns = messages
            .filter { it.role == "user" || it.role == "assistant" }
            .takeLast(6)
            .map { it.copy(content = it.content.take(4000)) }

        return when (format) {
            "chatml" -> buildString {
                if (system.isNotBlank()) append("<|im_start|>system\n").append(system).append("<|im_end|>\n")
                turns.forEach { m ->
                    val role = if (m.role == "user") "user" else "assistant"
                    append("<|im_start|>").append(role).append('\n')
                        .append(m.content).append("<|im_end|>\n")
                }
                append("<|im_start|>assistant\n")
            }
            "zephyr" -> buildString {
                if (system.isNotBlank()) append("<|system|>\n").append(system).append("</s>\n")
                turns.forEach { m ->
                    val role = if (m.role == "user") "user" else "assistant"
                    append("<|").append(role).append("|>\n").append(m.content).append("</s>\n")
                }
                append("<|assistant|>\n")
            }
            "gemma" -> buildString {
                if (system.isNotBlank()) {
                    append("<start_of_turn>user\n").append(system).append("\n\n")
                    val firstUser = turns.indexOfFirst { it.role == "user" }
                    if (firstUser >= 0) append(turns[firstUser].content).append("<end_of_turn>\n")
                    turns.forEachIndexed { i, m ->
                        if (i == firstUser) return@forEachIndexed
                        val role = if (m.role == "user") "user" else "model"
                        append("<start_of_turn>").append(role).append('\n')
                            .append(m.content).append("<end_of_turn>\n")
                    }
                } else {
                    turns.forEach { m ->
                        val role = if (m.role == "user") "user" else "model"
                        append("<start_of_turn>").append(role).append('\n')
                            .append(m.content).append("<end_of_turn>\n")
                    }
                }
                append("<start_of_turn>model\n")
            }
            else -> buildString {
                if (system.isNotBlank()) append("System: ").append(system).append("\n\n")
                turns.forEach { m ->
                    append(if (m.role == "user") "User: " else "Assistant: ")
                        .append(m.content).append("\n\n")
                }
                append("Assistant:")
            }
        }
    }
}
