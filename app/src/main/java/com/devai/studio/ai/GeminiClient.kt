package com.devai.studio.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Клиент для Google Gemini (Generative Language API).
 */
class GeminiClient(private val cfg: AiConfig) : AiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun chat(messages: List<AiMessage>, onDelta: (String) -> Unit): String =
        withContext(Dispatchers.IO) {
            val url = cfg.baseUrl.trimEnd('/') +
                "/models/${cfg.model}:streamGenerateContent?alt=sse&key=${cfg.apiKey}"

            val system = messages.filter { it.role == "system" }.joinToString("\n") { it.content }
            val contents = messages.filter { it.role != "system" }
                .map {
                    JSONObject().put(
                        "role",
                        if (it.role == "assistant") "model" else "user"
                    ).put("parts", JSONArray().put(JSONObject().put("text", it.content)))
                }

            val generationConfig = JSONObject().apply {
                put("temperature", cfg.temperature)
                put("maxOutputTokens", cfg.maxTokens)
            }

            val payload = JSONObject().apply {
                put("contents", JSONArray().apply { contents.forEach(::put) })
                put("generationConfig", generationConfig)
                if (system.isNotBlank()) {
                    put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
                }
            }

            val request = Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    error("HTTP ${resp.code}: ${resp.body?.string()}")
                }
                val sb = StringBuilder()
                val source = resp.body?.source() ?: error("Пустой ответ")
                while (true) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data.isEmpty()) continue
                    try {
                        val json = JSONObject(data)
                        val candidates = json.optJSONArray("candidates")
                        val text = candidates?.optJSONObject(0)?.optJSONObject("content")
                            ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
                        if (!text.isNullOrEmpty()) {
                            sb.append(text)
                            onDelta(text)
                        }
                    } catch (_: JSONException) {
                        // пропускаем служебные кадры
                    }
                }
                sb.toString()
            }
        }
}