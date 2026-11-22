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
 * Клиент для Anthropic Claude (Messages API).
 */
class AnthropicClient(private val cfg: AiConfig) : AiClient {

    private val timeoutSeconds = cfg.timeoutSeconds.coerceIn(15, 600).toLong()
    private val client = OkHttpClient.Builder()
        .connectTimeout(minOf(30L, timeoutSeconds), TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(minOf(60L, timeoutSeconds), TimeUnit.SECONDS)
        .build()

    override suspend fun chat(messages: List<AiMessage>, onDelta: (String) -> Unit): String =
        withContext(Dispatchers.IO) {
            val url = cfg.baseUrl.trimEnd('/') + "/v1/messages"

            val system = messages.filter { it.role == "system" }.joinToString("\n") { it.content }
            val turns = messages.filter { it.role != "system" }
                .map { JSONObject().put("role", if (it.role == "assistant") "assistant" else "user").put("content", it.content) }

            val payload = JSONObject().apply {
                put("model", cfg.model)
                put("max_tokens", cfg.maxTokens)
                put("temperature", cfg.temperature)
                put("top_p", cfg.topP)
                if (system.isNotBlank()) put("system", system)
                put("messages", JSONArray().apply { turns.forEach(::put) })
            }

            val request = Request.Builder()
                .url(url)
                .header("x-api-key", cfg.apiKey)
                .header("anthropic-version", "2023-06-01")
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
                        val type = json.optString("type")
                        when (type) {
                            "content_block_delta" -> {
                                val text = json.optJSONObject("delta")?.optString("text") ?: continue
                                sb.append(text)
                                onDelta(text)
                            }
                            "message_start", "content_block_start", "ping", "message_delta", "message_stop" -> {}
                            else -> {
                                // не-stream ответ: массив content
                                val content = json.optJSONArray("content")
                                if (content != null) {
                                    for (i in 0 until content.length()) {
                                        val text = content.optJSONObject(i)?.optString("text") ?: continue
                                        sb.append(text)
                                        onDelta(text)
                                    }
                                }
                            }
                        }
                    } catch (_: JSONException) {
                        // пропускаем служебные кадры
                    }
                }
                sb.toString()
            }
        }
}