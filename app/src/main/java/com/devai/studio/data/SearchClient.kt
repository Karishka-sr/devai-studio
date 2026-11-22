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

    suspend fun search(ctx: Context, query: String, maxResults: Int = 5): String =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            require(q.isNotEmpty()) { "Пустой поисковый запрос" }
            val limit = maxResults.coerceIn(1, 8)
            val tavilyKey = ApiKeyStore.get(ctx, KEY_ID)
            val githubToken = ApiKeyStore.get(ctx, GITHUB_KEY_ID)

            val sections = coroutineScope {
                listOf(
                    async { runCatching { tavily(tavilyKey, q, limit) }.getOrNull() },
                    async { runCatching { github(q, githubToken, limit) }.getOrNull() },
                    async { runCatching { stackOverflow(q, limit) }.getOrNull() },
                    async { runCatching { wikipedia(q, limit) }.getOrNull() },
                    async { runCatching { duckDuckGo(q, limit) }.getOrNull() },
                    async { runCatching { hackerNews(q, limit) }.getOrNull() },
                ).awaitAll().filterNotNull().filter { it.isNotBlank() }
            }

            if (sections.isEmpty()) {
                error("Поиск не дал результатов. Проверьте интернет и повторите запрос.")
            }
            sections.joinToString("\n\n").take(8000)
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
        val json = getJson(
            url(
                "https://ru.wikipedia.org/w/api.php",
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
        return buildString {
            append("### Wikipedia\n")
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                append(i + 1).append(". ").append(clean(item.optString("title"))).append("\n")
                append("https://ru.wikipedia.org/?curid=")
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
            .trim()
}
