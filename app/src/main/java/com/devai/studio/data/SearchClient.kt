package com.devai.studio.data

import android.content.Context
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Веб-поиск через Tavily. Ключ хранится в ApiKeyStore под [KEY_ID].
 * Результаты добавляются в системный промпт, поэтому поиск работает
 * с любым провайдером, включая локальные модели.
 */
object SearchClient {
    const val KEY_ID = "search_tavily"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isConfigured(ctx: Context): Boolean =
        !ApiKeyStore.get(ctx, KEY_ID).isNullOrBlank()

    suspend fun search(ctx: Context, query: String, maxResults: Int = 5): String =
        withContext(Dispatchers.IO) {
            val key = ApiKeyStore.get(ctx, KEY_ID)
                ?: error("Добавьте ключ Tavily в разделе «Инструменты».")
            val payload = JSONObject().apply {
                put("api_key", key)
                put("query", query)
                put("search_depth", "basic")
                put("max_results", maxResults.coerceIn(1, 10))
                put("include_answer", true)
            }
            val request = Request.Builder()
                .url("https://api.tavily.com/search")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("Поиск HTTP ${resp.code}: $body")
                val json = JSONObject(body)
                buildString {
                    val answer = json.optString("answer")
                    if (answer.isNotBlank()) append("Краткий ответ: ").append(answer).append("\n\n")
                    val results = json.optJSONArray("results") ?: return@buildString
                    for (i in 0 until results.length()) {
                        val item = results.optJSONObject(i) ?: continue
                        append(i + 1).append(". ")
                        append(item.optString("title").ifBlank { "Без названия" }).append("\n")
                        append(item.optString("url")).append("\n")
                        val content = item.optString("content")
                        if (content.isNotBlank()) append(content.take(1200)).append("\n")
                        append("\n")
                    }
                }.trim()
            }
        }
}
