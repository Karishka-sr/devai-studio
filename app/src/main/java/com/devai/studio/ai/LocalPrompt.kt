package com.devai.studio.ai

object LocalPrompt {

    private const val RUSSIAN =
        "Ты DevAI Studio. Отвечай на русском языке, если пользователь не попросил другой. " +
            "Отвечай только на последнее сообщение пользователя. Не придумывай задания и не делай " +
            "разбор, если об этом не просят. Если не знаешь — скажи прямо. Если пользователь " +
            "просто здоровается, ответь: Привет! Чем помочь?"

    /**
     * Small models have 512–1280 token windows, so the char budget is as important
     * as the prompt wording. The last user message always survives truncation.
     */
    fun build(messages: List<AiMessage>, @Suppress("UNUSED_PARAMETER") format: String, maxChars: Int = 3600): String {
        val budget = maxChars.coerceIn(1200, 12000)
        val cleanedSystem = PromptSanitizer.clean(
            messages.firstOrNull { it.role == "system" }?.content.orEmpty()
        ).trim()
        val searchMarker = "Актуальные результаты поиска"
        val hasSearch = cleanedSystem.contains(searchMarker)
        val systemBudget = (budget * if (hasSearch) 0.68 else 0.35).toInt()
        val system = if (!hasSearch) {
            cleanedSystem.take(systemBudget)
        } else {
            val head = cleanedSystem.substringBefore(searchMarker).trim().take((systemBudget * 0.4).toInt())
            val results = cleanedSystem.substringAfter(searchMarker).trim().take((systemBudget * 0.6).toInt())
            (head + "\n\n" + searchMarker + "\n" + results).trim()
        }

        val turns = messages
            .filter { it.role == "user" || it.role == "assistant" }
            .filterNot { it.role == "assistant" && ResponseGuard.isMeta(it.content) }
            .takeLast(6)

        val selected = ArrayList<AiMessage>()
        var used = system.length + RUSSIAN.length + 80
        val lastUserShare = if (hasSearch) 0.18 else 0.45
        for (message in turns.asReversed()) {
            val isLastUser = selected.isEmpty() && message.role == "user"
            val limit = if (isLastUser) (budget * lastUserShare).toInt() else 700
            val content = PromptSanitizer.clean(message.content).take(limit)
            val cost = content.length + 10
            if (selected.isNotEmpty() && used + cost > budget) break
            selected.add(0, message.copy(content = content))
            used += cost
        }

        return buildString {
            append("System: ")
            if (system.isNotBlank()) append(system).append(' ')
            append(RUSSIAN).append("\n\n")
            selected.forEach { m ->
                append(if (m.role == "user") "User: " else "Assistant: ")
                    .append(m.content).append("\n\n")
            }
            append("Assistant:")
        }
    }
}
