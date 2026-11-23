package com.devai.studio.ai

import android.content.Context
import com.devai.studio.BuildConfig
import com.devai.studio.util.AiSupport
import com.devai.studio.data.CodeLanguages
import com.devai.studio.data.LocalModelStore
import com.devai.studio.data.MemoryStore
import com.devai.studio.data.ProfileStore
import com.devai.studio.data.PromptLibrary
import com.devai.studio.data.PromptTemplate
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * DevAI Core v0.1 — собственный слой ассистента.
 *
 * Работает в двух режимах:
 * 1. Локальные навыки: память, промпты, справка, статус, время. Не нужен API-ключ.
 * 2. Оркестрация выбранной модели: если локальный навык не сработал, чат идёт
 *    через обычный провайдер с системным промптом Core.
 */
object DevAICore {
    const val NAME = "DevAI Core"
    const val VERSION = "0.2"

    val SYSTEM_PROMPT = AiQuality.CORE_PROMPT.replace("{VERSION}", VERSION)

    fun localReply(ctx: Context, rawInput: String): String? {
        val input = rawInput.trim()
        if (input.isEmpty()) return null
        val lower = input.lowercase()

        if (lower.matches(Regex("^(привет|здравствуй|хай|hello|hi|ку)\\W*$"))) {
            return "Привет. Я DevAI Core v$VERSION — свой ИИ DevAI Studio.\n\n" +
                "Могу помочь с кодом, промптами, памятью и настройками. Напиши «что ты умеешь»."
        }
        if (containsAny(lower, "кто ты", "ты кто", "что ты за", "о себе", "devai core")) {
            return "Я DevAI Core v$VERSION — собственный слой ассистента DevAI Studio.\n\n" +
                "Я не отдельная внешняя модель: я добавляю память, локальные навыки, " +
                "промпты и оркестрацию к выбранному тобой провайдеру."
        }
        if (containsAny(lower, "что умеешь", "твои возможности", "возможности core", "команды core", "список команд") ||
            lower.matches(Regex("^(help|помощь)\\W*$"))
        ) {
            return capabilities()
        }
        if (containsAny(lower, "твоя версия", "версия core", "version core")) {
            return "DevAI Core v$VERSION · DevAI Studio ${BuildConfig.VERSION_NAME}"
        }
        if (containsAny(lower, "как сменить ник", "сменить ник", "поменять ник", "мой ник", "мой профиль", "раздел профиль")) {
            return "Ник меняется так:\n\n" +
                "1. Настройки → Профиль.\n" +
                "2. Введи ник (2–32 символа).\n" +
                "3. Нажми «Сохранить ник».\n\n" +
                "Ник хранится в зашифрованных prefs и сразу появляется на главном экране настроек."
        }
        if (containsAny(lower, "телефон", "sms", "смс", "регистрац", "номер телефона", "войти по номеру")) {
            return "Вход по номеру телефона:\n\n" +
                "1. Настройки → Профиль.\n" +
                "2. Введи номер в формате +7 999 123-45-67.\n" +
                "3. Нажми «Получить код по SMS».\n" +
                "4. Введи 6 цифр из SMS.\n\n" +
                if (ProfileStore.isVerified(ctx)) "Сейчас номер подтверждён." else
                    "Сейчас номер не подтверждён. Если SMS не приходит, проверь Firebase: Phone Auth и отпечатки ключа."
        }
        if (containsAny(lower, "локальная модель", "локальную модель", "скачать модель", "модель скачать", "local model")) {
            return "Локальные модели: вкладка «ИИ» → «Локальные модели».\n\n" +
                "Скачай модель, выбери её и включи провайдер Local. После этого Core сможет " +
                "работать без облака. Сейчас активна: " +
                LocalModelStore.activeName(ctx).ifBlank { "модель не выбрана" } + "."
        }
        if (containsAny(lower, "языки программирования", "добавить язык", "языки в приложении", "languages")) {
            return "Языки лежат в файлах приложения, а не в assets.\n\n" +
                "Стартовый набор: ${CodeLanguages.all.size} JSON. Добавить свой язык можно в " +
                "Android/data/com.devai.studio/files/languages — без пересборки."
        }
        if (containsAny(lower, "промпт", "prompt")) {
            return promptReply(input)
        }
        if (lower.startsWith("запомни")) {
            val fact = input.removePrefix("запомни").trim().trim(':', '-', '—', ' ')
            if (fact.length < 3) return "Напиши, что запомнить: «Запомни: я делаю DevAI Studio»."
            val added = MemoryStore.add(ctx, fact)
            return if (added) "Запомнил: $fact" else "Это уже есть в памяти."
        }
        if (containsAny(lower, "что ты помнишь", "что помнишь", "моя память", "твоя память", "покажи память")) {
            val items = MemoryStore.list(ctx)
            return if (items.isEmpty()) {
                "Память пока пустая. Напиши «Запомни: ...», и я сохраню факт на устройстве."
            } else {
                "Я помню:\n" + items.joinToString("\n") { "• $it" }
            }
        }
        if (containsAny(lower, "забудь всё", "забудь все", "очисти память", "удали память")) {
            MemoryStore.clear(ctx)
            return "Память очищена."
        }
        if (containsAny(lower, "проверь систему", "статус системы", "диагност", "self-test", "selftest")) {
            return status(ctx)
        }
        if (containsAny(lower, "который час", "сколько времени", "текущая дата", "какое сегодня", "дата сегодня")) {
            val date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            return "Сегодня $date, сейчас $time."
        }
        if (containsAny(lower, "спасибо", "благодарю", "thanks")) {
            return "Пожалуйста. Если нужно — продолжим с кодом или настройками."
        }
        if (lower.matches(Regex("^(пока|до связи|bye)\\W*$"))) {
            return "До связи. Я останусь в DevAI Studio."
        }
        return null
    }

    fun offlineReply(ctx: Context, rawInput: String): String {
        return "Сейчас внешняя модель не подключена, поэтому я работаю локальными навыками.\n\n" +
            "Могу помочь с:\n" +
            "• памятью: «Запомни: ...», «Что ты помнишь?»;\n" +
            "• промптами: «Промпт для Kotlin API»;\n" +
            "• настройками: «Как сменить ник?»;\n" +
            "• проверкой: «Проверь систему».\n\n" +
            "Для свободного кода подключи провайдера во вкладке «ИИ». " +
            "Твой запрос: «" + rawInput.take(180) + "»."
    }

    private fun capabilities(): String {
        return "DevAI Core v$VERSION умеет:\n\n" +
            "• держать память о тебе на устройстве;\n" +
            "• искать по библиотеке из ${PromptLibrary.items.size} промптов;\n" +
            "• переформулировать запрос и искать в интернете, когда нужны свежие данные;\n" +
            "• подсказывать настройки ника, SMS, языков и моделей;\n" +
            "• проверять состояние приложения;\n" +
            "• отвечать на вопросы через выбранную модель.\n\n" +
            "Примеры: «Запомни: я делаю DevAI Studio», «Промпт для Python API», «Проверь систему»."
    }

    private fun promptReply(input: String): String {
        val marker = if (input.contains("промпт", ignoreCase = true)) "промпт" else "prompt"
        val start = input.indexOf(marker, ignoreCase = true).let { if (it >= 0) it + marker.length else 0 }
        val after = input.substring(start).substringAfter(":", input.substring(start))
        val query = after
            .replace(Regex("(?iu)^\\s*(для|на|по|about|for)\\s+"), "")
            .trim()
        if (query.isBlank()) {
            return "В библиотеке ${PromptLibrary.items.size} промптов.\n\n" +
                "Напиши тему: «Промпт для Kotlin API», «Промпт для тестов» или «Промпт для Docker»."
        }
        val found = findPrompts(query)
        if (found.isEmpty()) {
            return "По запросу «$query» ничего не нашёл. Попробуй короче: «Kotlin», «API», «тесты», «Docker»."
        }
        return "Нашёл ${found.size} промптов по запросу «$query»:\n\n" +
            found.joinToString("\n\n") { prompt ->
                "• ${prompt.title}" + (if (prompt.category.isNotBlank()) " · ${prompt.category}" else "") +
                    "\n" + prompt.prompt.take(220).trim() + "…"
            } + "\n\nСкопируй нужный промпт в чат или открой библиотеку промптов."
    }

    internal fun findPrompts(query: String, limit: Int = 5): List<PromptTemplate> {
        val exact = PromptLibrary.search(query)
        if (exact.isNotEmpty()) return exact.take(limit)
        val terms = query.split(Regex("\\s+")).filter { it.length >= 2 }
        if (terms.isEmpty()) return emptyList()
        return PromptLibrary.items.filter { prompt ->
            terms.all { term ->
                val stem = if (term.length >= 5) term.dropLast(1) else term
                fun has(text: String) = text.contains(term, true) || text.contains(stem, true)
                has(prompt.title) || has(prompt.prompt) || has(prompt.category)
            }
        }.take(limit)
    }

    private fun status(ctx: Context): String {
        val provider = AiSupport.provider(ctx)
        val providerText = when {
            provider == null -> "не выбран"
            provider.format == AiFormat.LOCAL ->
                provider.name + " · " + LocalModelStore.activeName(ctx).ifBlank { "модель не выбрана" }
            else -> provider.name + " · " + AiSupport.model(ctx, provider)
        }
        val phone = if (ProfileStore.isVerified(ctx)) "подтверждён" else "не подтверждён"
        return "DevAI Core v$VERSION — проверка:\n\n" +
            "• Провайдер: $providerText\n" +
            "• Память: ${MemoryStore.list(ctx).size} фактов\n" +
            "• Промпты: ${PromptLibrary.items.size}\n" +
            "• Языки: ${CodeLanguages.all.size}\n" +
            "• Телефон: $phone\n" +
            "• Версия приложения: ${BuildConfig.VERSION_NAME}"
    }

    private fun containsAny(text: String, vararg needles: String): Boolean =
        needles.any { text.contains(it) }
}
