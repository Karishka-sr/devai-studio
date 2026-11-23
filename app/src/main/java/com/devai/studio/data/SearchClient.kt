package com.devai.studio.data

import android.content.Context
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Мультиисточниковый поиск. Tavily используется, если сохранён ключ;
 * GitHub, Stack Overflow, Wikipedia, DuckDuckGo и Hacker News работают без ключа.
 * Результаты добавляются в системный промпт, поэтому поиск работает
 * с любым провайдером, включая локальные модели.
 */
object SearchClient {
    const val KEY_ID = "search_tavily"
    const val GITHUB_KEY_ID = "search_github"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    private val BING_BLOCK = Regex("<li class=\"b_algo\".*?</li>", RegexOption.DOT_MATCHES_ALL)
    private val BING_TITLE_DESKTOP = Regex(
        "<h2[^>]*>\\s*<a[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val BING_TITLE_MOBILE = Regex(
        "<a[^>]*href=\"([^\"]+)\"[^>]*>\\s*<h2[^>]*>(.*?)</h2>",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val BING_SNIPPET = Regex("<p[^>]*>(.*?)</p>", RegexOption.DOT_MATCHES_ALL)
    private val NEWS_HINT = Regex(
        "(?iu)(новост|последн|сегодня|сейчас|срочно|курс|цена|стоимость|погод|" +
            "что нового|происход|событ|выбор|матч|счёт|релиз|вышел|вышла|202[4-9])",
    )
    private val CURRENCY_HINT = Regex("(?iu)(курс|доллар|евро|юан|валют|usd|eur|cny)")
    private val GREETINGS = setOf(
        "привет", "приветик", "здравствуй", "здравствуйте", "хай", "hello", "hi", "ку",
        "спасибо", "благодарю", "thanks", "пока", "бай",
    )
    private val SMALL_TALK = listOf(
        "как дела", "как ты", "что делаешь", "чем занимаешься", "кто ты", "ты кто",
        "что ты умеешь", "твои возможности", "что нового у тебя", "как жизнь",
        "добрый день", "добрый вечер", "доброе утро", "доброй ночи",
    )
    private val WEATHER_HINT = Regex("(?iu)(погод|температур|дожд|снег|ветер|жарк|холод|градус)")
    private val URL = Regex("""https?://[^\s)>\]]+""")
    private val WEATHER_STOP = Regex(
        "(?iu)\\b(какая|какой|какие|сейчас|сегодня|завтра|погода|погоду|погоде|" +
            "температура|температуру|на|в|во|по|градусов|сколько|улице)\\b",
    )

    /**
     * Отсекает приветствия и короткую болтовню: для них веб-поиск только добавляет мусор.
     */
    fun isSearchWorthy(query: String): Boolean {
        val normalized = query.trim().lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (normalized.length < 3) return false
        var rest = normalized
        for (greeting in GREETINGS) {
            if (rest == greeting) return false
            if (rest.startsWith("$greeting ")) {
                rest = rest.removePrefix("$greeting ").trim()
                break
            }
        }
        if (rest.isBlank()) return false
        if (rest != normalized && rest.split(' ').size <= 1) return false
        if (SMALL_TALK.any { rest == it || rest.startsWith("$it ") }) return false
        return true
    }

    suspend fun search(ctx: Context, query: String, maxResults: Int = 5): String =
        withContext(Dispatchers.IO) {
            val plan = SearchQueryPlanner.plan(query)
            require(plan.primary.isNotBlank()) { "Пустой поисковый запрос" }
            val limit = maxResults.coerceIn(1, 8)

            val primary = searchOnce(ctx, plan.primary, limit)
            if (primary.isEmpty()) {
                error("Поиск не дал результатов. Проверьте интернет и повторите запрос.")
            }

            val extra = if (primary.joinToString("\n").length < 1800 && plan.alternates.isNotEmpty()) {
                coroutineScope {
                    plan.alternates.map { alternate ->
                        async { runCatching { searchOnce(ctx, alternate, limit) }.getOrDefault(emptyList()) }
                    }.awaitAll().flatten()
                }
            } else {
                emptyList()
            }

            compact(orderSections(primary + extra, plan)).ifBlank {
                error("Поиск не дал результатов. Проверьте интернет и повторите запрос.")
            }
        }

    private suspend fun searchOnce(ctx: Context, query: String, limit: Int): List<String> =
        coroutineScope {
            val tavilyKey = ApiKeyStore.get(ctx, KEY_ID)
            val githubToken = ApiKeyStore.get(ctx, GITHUB_KEY_ID)
            listOf(
                async { runCatching { tavily(tavilyKey, query, limit) }.getOrNull() },
                async { runCatching { bing(query, limit) }.getOrNull() },
                async { runCatching { googleNews(query, limit) }.getOrNull() },
                async { runCatching { currencyRates(query) }.getOrNull() },
                async { runCatching { weather(query) }.getOrNull() },
                async { runCatching { github(query, githubToken, limit) }.getOrNull() },
                async { runCatching { stackOverflow(query, limit) }.getOrNull() },
                async { runCatching { wikipedia(query, limit) }.getOrNull() },
                async { runCatching { duckDuckGo(query, limit) }.getOrNull() },
                async { runCatching { hackerNews(query, limit) }.getOrNull() },
            ).awaitAll().filterNotNull().filter { it.isNotBlank() }
        }

    private fun orderSections(sections: List<String>, plan: SearchQueryPlanner.Plan): List<String> {
        val preferred = when {
            plan.fresh -> listOf(
                "Новости", "Курсы", "Погода", "Tavily", "Bing", "Wikipedia",
                "DuckDuckGo", "GitHub", "Stack Overflow", "Hacker News",
            )
            plan.code -> listOf(
                "Stack Overflow", "GitHub", "Tavily", "Bing", "Wikipedia",
                "DuckDuckGo", "Hacker News", "Новости", "Курсы", "Погода",
            )
            else -> listOf(
                "Tavily", "Bing", "Wikipedia", "DuckDuckGo", "GitHub",
                "Stack Overflow", "Новости", "Курсы", "Погода", "Hacker News",
            )
        }
        return sections.sortedBy { section ->
            val header = section.substringAfter("### ", "").substringBefore('\n')
            preferred.indexOfFirst { header.contains(it) }.let { if (it < 0) preferred.size else it }
        }
    }

    internal fun compact(sections: List<String>, maxChars: Int = 9000): String {
        val seenUrls = HashSet<String>()
        val out = StringBuilder()
        for ((index, section) in sections.withIndex()) {
            if (index > 0 && out.length >= maxChars) break
            val body = section.trim().take(2600)
            if (body.isBlank()) continue
            val clean = body.lineSequence().filterNot { line ->
                val urls = URL.findAll(line).map { it.value.trimEnd('.', ',', ';', ')', ']') }.toList()
                if (urls.isEmpty()) return@filterNot false
                urls.all { !seenUrls.add(it) }
            }.joinToString("\n").trim()
            if (clean.isBlank()) continue
            if (out.isNotEmpty()) out.append("\n\n")
            out.append(clean)
        }
        return out.toString().take(maxChars).trim()
    }

    private fun tavily(key: String?, query: String, limit: Int): String? {
        if (key.isNullOrBlank()) return null
        val payload = JSONObject().apply {
            put("api_key", key)
            put("query", query)
            put("search_depth", "basic")
            put("max_results", limit.coerceAtMost(10))
            put("include_answer", true)
        }
        val json = postJson("https://api.tavily.com/search", payload)
        val answer = json.text("answer")
        val results = json.optJSONArray("results")
        val text = buildString {
            append("### Tavily (веб)\n")
            if (answer.isNotBlank()) append("Краткий ответ: ").append(answer).append("\n\n")
            if (results != null) {
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    append(i + 1).append(". ")
                    append(item.optString("title").ifBlank { "Без названия" }).append("\n")
                    append(item.optString("url")).append("\n")
                    val content = item.text("content")
                    if (content.isNotBlank()) append(content.take(900)).append("\n")
                    append("\n")
                }
            }
        }.trim()
        return text.takeIf { it.length > "### Tavily (веб)".length + 2 }
    }

    private fun bing(query: String, limit: Int): String? {
        if (query.any { it.code in 0x0400..0x04FF }) return null
        val html = getText(
            url(
                "https://www.bing.com/search",
                mapOf(
                    "q" to query,
                    "count" to limit.coerceAtMost(8).toString(),
                    "setlang" to "ru",
                ),
            ),
            mapOf("Accept-Language" to "ru-RU,ru;q=0.9,en;q=0.8"),
        )
        return parseBing(html, limit)
    }

    private fun googleNews(query: String, limit: Int): String? {
        if (!NEWS_HINT.containsMatchIn(query)) return null
        val russian = query.any { it.code in 0x0400..0x04FF }
        val xml = getText(
            url(
                "https://news.google.com/rss/search",
                if (russian) {
                    mapOf("q" to query, "hl" to "ru", "gl" to "RU", "ceid" to "RU:ru")
                } else {
                    mapOf("q" to query, "hl" to "en-US", "gl" to "US", "ceid" to "US:en")
                },
            ),
        )
        val items = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml)
            .take(limit.coerceAtMost(5))
            .toList()
        if (items.isEmpty()) return null
        return buildString {
            append("### Новости (Google News)\n")
            for ((index, item) in items.withIndex()) {
                val block = item.value
                val title = clean(
                    Regex("<title>(.*?)</title>", RegexOption.DOT_MATCHES_ALL)
                        .find(block)?.groupValues?.get(1).orEmpty(),
                )
                val link = Regex("<link>(.*?)</link>", RegexOption.DOT_MATCHES_ALL)
                    .find(block)?.groupValues?.get(1).orEmpty()
                    .replace("&amp;", "&")
                val date = Regex("<pubDate>(.*?)</pubDate>", RegexOption.DOT_MATCHES_ALL)
                    .find(block)?.groupValues?.get(1).orEmpty()
                val source = clean(
                    Regex("<source[^>]*>(.*?)</source>", RegexOption.DOT_MATCHES_ALL)
                        .find(block)?.groupValues?.get(1).orEmpty(),
                )
                if (title.isBlank()) continue
                append(index + 1).append(". ").append(title).append("\n")
                if (source.isNotBlank()) append("   ").append(source)
                if (date.isNotBlank()) append(" · ").append(date)
                if (source.isNotBlank() || date.isNotBlank()) append("\n")
                if (link.isNotBlank()) append("   ").append(link).append("\n")
            }
        }.trim().takeIf { it.length > "### Новости (Google News)".length + 2 }
    }

    private fun currencyRates(query: String): String? {
        if (!CURRENCY_HINT.containsMatchIn(query)) return null
        val json = getJson("https://www.cbr-xml-daily.ru/daily_json.js")
        val valute = json.optJSONObject("Valute") ?: return null
        val rows = listOf("USD" to "Доллар", "EUR" to "Евро", "CNY" to "Юань")
            .mapNotNull { (code, label) ->
                val item = valute.optJSONObject(code) ?: return@mapNotNull null
                val nominal = item.optDouble("Nominal", 1.0).coerceAtLeast(1.0)
                val value = item.optDouble("Value", 0.0) / nominal
                val previous = item.optDouble("Previous", 0.0) / nominal
                if (value <= 0.0) return@mapNotNull null
                val delta = value - previous
                val arrow = if (delta >= 0) "▲" else "▼"
                "$label: ${"%.2f".format(value)} ₽ $arrow ${"%.2f".format(kotlin.math.abs(delta))}"
            }
        if (rows.isEmpty()) return null
        val date = json.optString("Date").substringBefore('T')
        return buildString {
            append("### Курсы ЦБ РФ")
            if (date.isNotBlank()) append(" на ").append(date)
            append("\n")
            rows.forEach { append("• ").append(it).append("\n") }
        }.trim()
    }

    private fun weather(query: String): String? {
        if (!WEATHER_HINT.containsMatchIn(query)) return null
        val city = query
            .replace(WEATHER_STOP, " ")
            .replace(Regex("[?!.,]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { "Москва" }
        val encoded = java.net.URLEncoder.encode(city, "UTF-8").replace("+", "%20")
        val json = getJson("https://wttr.in/$encoded?format=j1&lang=ru")
        val current = json.optJSONArray("current_condition")?.optJSONObject(0) ?: return null
        val area = json.optJSONArray("nearest_area")
            ?.optJSONObject(0)
            ?.optJSONArray("areaName")
            ?.optJSONObject(0)
            ?.optString("value")
            .orEmpty()
        val temp = current.optString("temp_C")
        val feels = current.optString("FeelsLikeC")
        val humidity = current.optString("humidity")
        val desc = current.optJSONArray("lang_ru")
            ?.optJSONObject(0)
            ?.optString("value")
            .orEmpty()
            .ifBlank {
                current.optJSONArray("weatherDesc")
                    ?.optJSONObject(0)
                    ?.optString("value")
                    .orEmpty()
            }
        if (temp.isBlank()) return null
        return buildString {
            append("### Погода сейчас")
            if (area.isNotBlank()) append(" · ").append(area)
            append("\n")
            append("• Температура: ").append(temp).append(" °C")
            if (feels.isNotBlank()) append(" (ощущается ").append(feels).append(" °C)")
            append("\n")
            if (humidity.isNotBlank()) append("• Влажность: ").append(humidity).append(" %\n")
            if (desc.isNotBlank()) append("• ").append(desc).append("\n")
        }.trim()
    }

    internal fun parseBing(html: String, limit: Int): String? {
        val blocks = BING_BLOCK.findAll(html).take(limit)
        val text = buildString {
            append("### Bing (веб)\n")
            var found = false
            for (block in blocks) {
                val titleMatch = BING_TITLE_DESKTOP.find(block.value)
                    ?: BING_TITLE_MOBILE.find(block.value)
                    ?: continue
                val title = clean(titleMatch.groupValues[2])
                if (title.isBlank()) continue
                found = true
                append("- ").append(title).append("\n")
                append("  ").append(decodeBingUrl(titleMatch.groupValues[1])).append("\n")
                val snippet = BING_SNIPPET.find(block.value)?.groupValues?.get(1)
                if (!snippet.isNullOrBlank()) {
                    append("  ").append(clean(snippet).take(400)).append("\n")
                }
            }
            if (!found) clear()
        }.trim()
        return text.takeIf { it.isNotBlank() }
    }

    private fun decodeBingUrl(raw: String): String {
        val marker = "u=a1"
        val start = raw.indexOf(marker)
        if (start < 0) return raw.replace("&amp;", "&")
        val encoded = raw.substring(start + marker.length).substringBefore('&')
        return runCatching {
            String(java.util.Base64.getUrlDecoder().decode(encoded))
        }.getOrElse { raw.replace("&amp;", "&") }
    }

    private fun github(query: String, token: String?, limit: Int): String? {
        val perPage = limit.coerceAtMost(5)
        val headers = token?.takeIf { it.isNotBlank() }
        val repos = runCatching {
            getJson(
                url(
                    "https://api.github.com/search/repositories",
                    mapOf(
                        "q" to query,
                        "per_page" to perPage.toString(),
                        "sort" to "stars",
                        "order" to "desc",
                    ),
                ),
                headers,
            )
        }.getOrNull()
        val issues = runCatching {
            getJson(
                url(
                    "https://api.github.com/search/issues",
                    mapOf(
                        "q" to query,
                        "per_page" to perPage.toString(),
                        "sort" to "relevance",
                        "order" to "desc",
                    ),
                ),
                headers,
            )
        }.getOrNull()
        val code = if (headers != null) {
            runCatching {
                getJson(
                    url(
                        "https://api.github.com/search/code",
                        mapOf("q" to "$query in:file", "per_page" to "3"),
                    ),
                    headers,
                )
            }.getOrNull()
        } else {
            null
        }

        var found = false
        val text = buildString {
            append("### GitHub\n")
            repos?.optJSONArray("items")?.let { items ->
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    found = true
                    append("- Репозиторий: ")
                    append(item.optString("full_name").ifBlank { "без имени" })
                    append(" · ★ ").append(item.optInt("stargazers_count"))
                    item.text("language").takeIf { it.isNotBlank() }?.let {
                        append(" · ").append(it)
                    }
                    append("\n  ").append(item.optString("html_url")).append("\n")
                    item.text("description").takeIf { it.isNotBlank() }?.let {
                        append("  ").append(it.take(400)).append("\n")
                    }
                }
            }
            issues?.optJSONArray("items")?.let { items ->
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    found = true
                    val isPr = item.has("pull_request")
                    append("- ").append(if (isPr) "PR" else "Issue").append(": ")
                    append(item.optString("title").ifBlank { "Без названия" })
                    append(" · ").append(item.optString("state"))
                    append("\n  ").append(item.optString("html_url")).append("\n")
                    item.text("body").takeIf { it.isNotBlank() }?.let {
                        append("  ").append(it.replace('\n', ' ').take(300)).append("\n")
                    }
                }
            }
            code?.optJSONArray("items")?.let { items ->
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    found = true
                    append("- Код: ")
                    append(item.optJSONObject("repository")?.optString("full_name").orEmpty())
                    append("/").append(item.optString("path"))
                    append("\n  ").append(item.optString("html_url")).append("\n")
                }
            }
        }.trim()
        return text.takeIf { found }
    }

    private fun stackOverflow(query: String, limit: Int): String? {
        val json = getJson(
            url(
                "https://api.stackexchange.com/2.3/search/advanced",
                mapOf(
                    "order" to "desc",
                    "sort" to "relevance",
                    "q" to query,
                    "site" to "stackoverflow",
                    "pagesize" to limit.coerceAtMost(5).toString(),
                ),
            ),
        )
        val items = json.optJSONArray("items") ?: return null
        if (items.length() == 0) return null
        return buildString {
            append("### Stack Overflow\n")
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                append(i + 1).append(". ").append(clean(item.optString("title"))).append("\n")
                append(item.optString("link")).append("\n")
                append("Баллы: ").append(item.optInt("score"))
                append(" · ответов: ").append(item.optInt("answer_count"))
                if (item.optBoolean("is_answered")) append(" · есть принятый ответ")
                append("\n\n")
            }
        }.trim()
    }

    private fun wikipedia(query: String, limit: Int): String? {
        val lang = if (query.any { it.code in 0x0400..0x04FF }) "ru" else "en"
        val json = getJson(
            url(
                "https://$lang.wikipedia.org/w/api.php",
                mapOf(
                    "action" to "query",
                    "list" to "search",
                    "srsearch" to query,
                    "format" to "json",
                    "utf8" to "1",
                    "srlimit" to limit.coerceAtMost(5).toString(),
                ),
            ),
        )
        val items = json.optJSONObject("query")?.optJSONArray("search") ?: return null
        if (items.length() == 0) return null
        val firstTitle = items.optJSONObject(0)?.optString("title").orEmpty()
        val summary = if (firstTitle.isNotBlank()) {
            runCatching {
                getJson(
                    "https://$lang.wikipedia.org/api/rest_v1/page/summary/".toHttpUrl()
                        .newBuilder()
                        .addPathSegment(firstTitle)
                        .build()
                        .toString(),
                )
            }.getOrNull()
        } else {
            null
        }
        return buildString {
            append("### Wikipedia\n")
            val extract = clean(summary?.text("extract").orEmpty())
            if (extract.isNotBlank()) append(extract.take(900)).append("\n\n")
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                append(i + 1).append(". ").append(clean(item.optString("title"))).append("\n")
                append("https://$lang.wikipedia.org/?curid=")
                append(item.optInt("pageid")).append("\n")
                val snippet = clean(item.optString("snippet"))
                if (snippet.isNotBlank()) append(snippet.take(400)).append("\n")
                append("\n")
            }
        }.trim()
    }

    private fun duckDuckGo(query: String, limit: Int): String? {
        val json = getJson(
            url(
                "https://api.duckduckgo.com/",
                mapOf(
                    "q" to query,
                    "format" to "json",
                    "no_html" to "1",
                    "skip_disambig" to "1",
                ),
            ),
        )
        val text = buildString {
            append("### DuckDuckGo\n")
            val abstract = json.text("AbstractText")
            val abstractUrl = json.text("AbstractURL")
            var found = false
            if (abstract.isNotBlank()) {
                found = true
                append(abstract.take(600)).append("\n")
                if (abstractUrl.isNotBlank()) append(abstractUrl).append("\n")
            }
            val topics = json.optJSONArray("RelatedTopics")
            if (topics != null) {
                var count = 0
                for (i in 0 until topics.length()) {
                    if (count >= limit.coerceAtMost(4)) break
                    val item = topics.optJSONObject(i) ?: continue
                    val nested = item.optJSONArray("Topics")
                    if (nested != null) {
                        for (j in 0 until nested.length()) {
                            if (count >= limit.coerceAtMost(4)) break
                            val sub = nested.optJSONObject(j) ?: continue
                            val topicText = sub.optString("Text")
                            val topicUrl = sub.optString("FirstURL")
                            if (topicText.isBlank()) continue
                            found = true
                            append("- ").append(topicText.take(300)).append("\n")
                            if (topicUrl.isNotBlank()) append("  ").append(topicUrl).append("\n")
                            count++
                        }
                    } else {
                        val topicText = item.optString("Text")
                        val topicUrl = item.optString("FirstURL")
                        if (topicText.isBlank()) continue
                        found = true
                        append("- ").append(topicText.take(300)).append("\n")
                        if (topicUrl.isNotBlank()) append("  ").append(topicUrl).append("\n")
                        count++
                    }
                }
            }
            if (!found) clear()
        }.trim()
        return text.takeIf { it.isNotBlank() }
    }

    private fun hackerNews(query: String, limit: Int): String? {
        val json = getJson(
            url(
                "https://hn.algolia.com/api/v1/search",
                mapOf(
                    "query" to query,
                    "tags" to "story",
                    "hitsPerPage" to limit.coerceAtMost(5).toString(),
                ),
            ),
        )
        val hits = json.optJSONArray("hits") ?: return null
        if (hits.length() == 0) return null
        return buildString {
            append("### Hacker News\n")
            for (i in 0 until hits.length()) {
                val item = hits.optJSONObject(i) ?: continue
                val title = item.optString("title")
                if (title.isBlank()) continue
                append(i + 1).append(". ").append(title).append("\n")
                val link = item.text("url").ifBlank {
                    "https://news.ycombinator.com/item?id=" + item.optString("objectID")
                }
                append(link).append("\n")
                append("Баллы: ").append(item.optInt("points"))
                append(" · комментариев: ").append(item.optInt("num_comments"))
                append("\n\n")
            }
        }.trim()
    }

    private fun url(base: String, params: Map<String, String>): String {
        val builder = base.toHttpUrl().newBuilder()
        params.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        return builder.build().toString()
    }

    private fun getText(url: String, headers: Map<String, String> = emptyMap()): String {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/120.0 Safari/537.36",
            )
            .apply { headers.forEach { (key, value) -> header(key, value) } }
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("HTTP ${resp.code}: ${body.take(200)}")
            return body
        }
    }

    private fun getJson(url: String, githubToken: String? = null): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "DevAI-Studio")
            .apply {
                if (githubToken != null) header("Authorization", "Bearer $githubToken")
                if (url.contains("api.github.com")) {
                    header("Accept", "application/vnd.github+json")
                    header("X-GitHub-Api-Version", "2022-11-28")
                }
            }
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("HTTP ${resp.code}: ${body.take(200)}")
            return JSONObject(body)
        }
    }

    private fun postJson(url: String, payload: JSONObject): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("HTTP ${resp.code}: ${body.take(200)}")
            return JSONObject(body)
        }
    }

    private fun JSONObject.text(key: String): String =
        if (isNull(key)) "" else optString(key)

    private fun clean(value: String): String =
        value
            .replace(Regex("<[^>]+>"), "")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&ndash;", "–")
            .replace("&mdash;", "—")
            .replace(Regex("&#(\\d+);")) { match ->
                match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value
            }
            .replace(Regex("\\s+"), " ")
            .trim()
}
