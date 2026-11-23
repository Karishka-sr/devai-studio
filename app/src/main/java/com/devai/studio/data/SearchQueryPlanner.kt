package com.devai.studio.data

import java.time.Year

/**
 * Turns a chat message into a compact search query. Search engines do badly
 * with "подскажи, пожалуйста, что такое ..." and lose the useful terms.
 */
object SearchQueryPlanner {
    data class Plan(
        val primary: String,
        val alternates: List<String>,
        val fresh: Boolean,
        val code: Boolean,
        val language: String,
    )

    private val LEADING = listOf(
        "привет", "приветик", "здравствуй", "здравствуйте", "хай", "ку", "hello", "hi",
        "пожалуйста", "плиз", "подскажи", "подскажите", "расскажи", "расскажите",
        "объясни", "объясните", "помоги", "помогите", "можешь", "можете",
        "не мог бы", "не могли бы", "хочу узнать", "интересует",
        "please", "can you", "could you", "would you", "tell me", "explain",
    )

    private val QUESTION_PREFIXES = listOf(
        "что такое", "что значит", "кто такой", "кто такая", "кто такие",
        "про", "о", "об",
    )

    private val FRESH = Regex(
        "(?iu)(новост|последн|сегодня|сейчас|срочно|курс|цена|стоимость|погод|" +
            "что нового|происход|событ|выбор|матч|счёт|релиз|вышел|вышла|202[4-9])",
    )

    private val CODE = Regex(
        "(?iu)(код|ошибк|error|exception|stack ?trace|kotlin|java|android|gradle|" +
            "python|sql|json|api|flutter|swift|rust|javascript|typescript|docker|git|" +
            "compose|coroutine|http|regex|bash)",
    )

    private val CYRILLIC = Regex("[\\u0400-\\u04FF]")

    fun plan(raw: String, year: Int = Year.now().value): Plan {
        val base = normalize(raw)
        val fresh = FRESH.containsMatchIn(raw)
        val code = CODE.containsMatchIn(base)
        val language = if (CYRILLIC.containsMatchIn(base)) "ru" else "en"
        val alternates = LinkedHashSet<String>()

        if (fresh) alternates += "$base $year"
        if (code) alternates += "$base stackoverflow"

        return Plan(
            primary = base,
            alternates = alternates.filter { it != base }.take(2),
            fresh = fresh,
            code = code,
            language = language,
        )
    }

    fun normalize(raw: String): String {
        var text = raw.trim()
            .replace(Regex("\\s+"), " ")
            .trim(' ', '"', '\'', '«', '»', '?', '!', '.', ',', ';', ':')

        var changed = true
        while (changed) {
            changed = false
            for (prefix in LEADING) {
                if (text.startsWithWord(prefix)) {
                    text = text.drop(prefix.length).trimStart(' ', ',', ':', '-', '—')
                    changed = true
                }
            }
        }

        for (prefix in QUESTION_PREFIXES) {
            if (text.startsWithWord(prefix)) {
                text = text.drop(prefix.length).trimStart(' ', ',', ':', '-', '—')
                break
            }
        }

        while (text.endsWith("пожалуйста", ignoreCase = true)) {
            text = text.dropLast("пожалуйста".length).trimEnd(' ', ',', '.', '!')
        }
        text = text.trim(' ', '"', '\'', '«', '»', '?', '!', '.', ',', ';', ':')

        return text.ifBlank { raw.trim().take(200) }.take(200).trimEnd()
    }

    private fun String.startsWithWord(prefix: String): Boolean {
        if (!startsWith(prefix, ignoreCase = true)) return false
        if (length == prefix.length) return true
        return !this[prefix.length].isLetterOrDigit()
    }
}
