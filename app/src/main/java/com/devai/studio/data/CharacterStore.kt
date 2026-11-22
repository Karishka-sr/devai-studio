package com.devai.studio.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class CharacterCard(
    val id: String,
    val name: String,
    val description: String,
    val personality: String = "",
    val scenario: String = "",
    val greeting: String = "",
    val prompt: String = "",
    val builtIn: Boolean = false,
) {
    fun systemText(): String {
        if (prompt.isNotBlank()) return prompt.trim()
        return buildString {
            append("Ты — ").append(name).append(". ")
            if (description.isNotBlank()) append(description).append(" ")
            if (personality.isNotBlank()) append("Характер: ").append(personality).append(" ")
            if (scenario.isNotBlank()) append("Сценарий: ").append(scenario).append(" ")
            append("Отвечай от лица этого персонажа, но не выдумывай факты.")
        }.trim()
    }
}

object CharacterStore {
    private const val PREFS = "devai_characters"
    private const val KEY_CARDS = "cards"
    private const val KEY_ACTIVE = "active"

    val builtIns: List<CharacterCard> = listOf(
        CharacterCard(
            id = "builtin-reviewer",
            name = "Строгий ревьюер",
            description = "Проверяет код как senior-ревьюер и ищет баги.",
            prompt = "Ты — строгий senior-ревьюер. Ищи баги, риски, проблемы безопасности и " +
                "плохие архитектурные решения. Давай конкретные исправления с кодом. " +
                "Не хвали без причины.",
            builtIn = true,
        ),
        CharacterCard(
            id = "builtin-mentor",
            name = "Наставник",
            description = "Объясняет сложное простыми словами и задаёт вопросы.",
            prompt = "Ты — терпеливый наставник по программированию. Объясняй простыми словами, " +
                "приводи маленькие примеры и в конце задавай один вопрос на проверку понимания.",
            builtIn = true,
        ),
        CharacterCard(
            id = "builtin-cat",
            name = "Кот-разработчик",
            description = "Помогает с кодом и иногда мяукает.",
            prompt = "Ты — кот-разработчик. Помогай с кодом, но добавляй лёгкий кошачий юмор. " +
                "Не мяукай в каждом предложении, код должен оставаться рабочим.",
            builtIn = true,
        ),
    )

    private fun p(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun list(ctx: Context): List<CharacterCard> = builtIns + custom(ctx)

    fun activeId(ctx: Context): String? = p(ctx).getString(KEY_ACTIVE, null)

    fun active(ctx: Context): CharacterCard? {
        val id = activeId(ctx) ?: return null
        return list(ctx).firstOrNull { it.id == id }
    }

    fun setActive(ctx: Context, id: String?) {
        if (id.isNullOrBlank()) p(ctx).edit().remove(KEY_ACTIVE).apply()
        else p(ctx).edit().putString(KEY_ACTIVE, id).apply()
    }

    fun save(ctx: Context, card: CharacterCard): CharacterCard {
        val saved = card.copy(
            id = card.id.ifBlank { UUID.randomUUID().toString() },
            builtIn = false,
        )
        val cards = custom(ctx).filterNot { it.id == saved.id } + saved
        write(ctx, cards)
        return saved
    }

    fun delete(ctx: Context, id: String) {
        write(ctx, custom(ctx).filterNot { it.id == id })
        if (activeId(ctx) == id) setActive(ctx, null)
    }

    private fun custom(ctx: Context): List<CharacterCard> {
        val raw = p(ctx).getString(KEY_CARDS, null) ?: return emptyList()
        val out = mutableListOf<CharacterCard>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                out.add(
                    CharacterCard(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        description = o.optString("description"),
                        personality = o.optString("personality"),
                        scenario = o.optString("scenario"),
                        greeting = o.optString("greeting"),
                        prompt = o.optString("prompt"),
                    )
                )
            }
        } catch (_: Exception) {
        }
        return out
    }

    private fun write(ctx: Context, cards: List<CharacterCard>) {
        val arr = JSONArray()
        cards.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("description", it.description)
                    .put("personality", it.personality)
                    .put("scenario", it.scenario)
                    .put("greeting", it.greeting)
                    .put("prompt", it.prompt)
            )
        }
        p(ctx).edit().putString(KEY_CARDS, arr.toString()).apply()
    }
}
