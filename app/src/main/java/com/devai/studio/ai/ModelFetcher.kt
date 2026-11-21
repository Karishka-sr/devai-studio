package com.devai.studio.ai

import com.devai.studio.data.ProviderInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
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
            }
        }

    private fun openAi(baseUrl: String, apiKey: String): List<String> {
        val req = Request.Builder().url(baseUrl.trimEnd('/') + "/models").apply {
            if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
        }.build()
        return exec(req) { json ->
            val data = json.optJSONArray("data") ?: return@exec emptyList()
            val out = mutableListOf<String>()
            for (i in 0 until data.length()) {
                data.optJSONObject(i)?.optString("id")?.takeIf { it.isNotBlank() }?.let { out.add(it) }
            }
            out.sorted()
        }
    }

    private fun anthropic(baseUrl: String, apiKey: String): List<String> {
        val req = Request.Builder().url(baseUrl.trimEnd('/') + "/v1/models").apply {
            if (apiKey.isNotBlank()) {
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
            }
        }.build()
        return exec(req) { json ->
            val data = json.optJSONArray("data") ?: return@exec emptyList()
            val out = mutableListOf<String>()
            for (i in 0 until data.length()) {
                data.optJSONObject(i)?.optString("id")?.takeIf { it.isNotBlank() }?.let { out.add(it) }
            }
            out.sorted()
        }
    }

    private fun gemini(baseUrl: String, apiKey: String): List<String> {
        val req = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/models?key=" + apiKey)
            .build()
        return exec(req) { json ->
            val arr = json.optJSONArray("models") ?: return@exec emptyList()
            val out = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val name = arr.optJSONObject(i)?.optString("name").orEmpty()
                if (name.isNotBlank()) out.add(name.removePrefix("models/"))
            }
            out.sorted()
        }
    }

    private inline fun exec(req: Request, parse: (JSONObject) -> List<String>): List<String> {
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP " + resp.code)
            val body = resp.body?.string() ?: return emptyList()
            return parse(JSONObject(body))
        }
    }
}