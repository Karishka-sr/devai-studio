package com.devai.studio.ai

import com.devai.studio.data.ProviderInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object ModelFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun fetch(provider: ProviderInfo, apiKey: String, baseUrl: String): List<String> =
        withContext(Dispatchers.IO) {
            when (provider.format) {
                AiFormat.OPENAI_COMPAT -> openAi(baseUrl, apiKey)
                AiFormat.ANTHROPIC -> anthropic(baseUrl, apiKey)
                AiFormat.GEMINI -> gemini(baseUrl, apiKey)
                AiFormat.LOCAL -> emptyList()
            }
        }

    private fun openAi(baseUrl: String, apiKey: String): List<String> {
        val req = Request.Builder().url(normalize(baseUrl, "/models")).apply {
            header("User-Agent", "DevAI-Studio/2.13")
            if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
        }.build()
        val json = requestJson(req)
        val data = json.optJSONArray("data") ?: return emptyList()
        val out = mutableListOf<String>()
        for (i in 0 until data.length()) {
            data.optJSONObject(i)?.optString("id")?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        }
        return out.distinct().sorted()
    }

    private fun anthropic(baseUrl: String, apiKey: String): List<String> {
        val base = baseUrl.trim().trimEnd('/')
        val url = if (base.endsWith("/v1")) "$base/models" else "$base/v1/models"
        val req = Request.Builder().url(url).apply {
            header("User-Agent", "DevAI-Studio/2.13")
            if (apiKey.isNotBlank()) {
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
            }
        }.build()
        val json = requestJson(req)
        val data = json.optJSONArray("data") ?: return emptyList()
        val out = mutableListOf<String>()
        for (i in 0 until data.length()) {
            data.optJSONObject(i)?.optString("id")?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        }
        return out.distinct().sorted()
    }

    private fun gemini(baseUrl: String, apiKey: String): List<String> {
        val out = mutableListOf<String>()
        var pageToken: String? = null
        do {
            val url = StringBuilder(normalize(baseUrl, "/models"))
                .append("?pageSize=1000")
            if (apiKey.isNotBlank()) url.append("&key=").append(encode(apiKey))
            pageToken?.let { url.append("&pageToken=").append(encode(it)) }
            val req = Request.Builder().url(url.toString())
                .header("User-Agent", "DevAI-Studio/2.13")
                .build()
            val json = requestJson(req)
            val arr = json.optJSONArray("models")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val name = item.optString("name").removePrefix("models/")
                    if (name.isBlank()) continue
                    val methods = item.optJSONArray("supportedGenerationMethods")
                    val canChat = methods == null || (0 until methods.length()).any {
                        methods.optString(it) in setOf("generateContent", "streamGenerateContent")
                    }
                    if (canChat && !name.contains("embedding") && !name.contains("aqa")) out.add(name)
                }
            }
            pageToken = json.optString("nextPageToken").takeIf { it.isNotBlank() }
        } while (pageToken != null)
        return out.distinct().sorted()
    }

    private fun normalize(baseUrl: String, suffix: String): String {
        var base = baseUrl.trim().trimEnd('/')
        base = base.removeSuffix("/chat/completions").removeSuffix("/models")
        return base + suffix
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun requestJson(req: Request): JSONObject {
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val detail = body.take(240).replace(Regex("\\s+"), " ").trim()
                error("HTTP ${resp.code}" + if (detail.isBlank()) "" else ": $detail")
            }
            return JSONObject(body)
        }
    }
}