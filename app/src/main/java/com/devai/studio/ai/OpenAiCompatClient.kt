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
 * Универсальный клиент для любого OpenAI-совместимого API:
 * OpenAI, OpenRouter, Groq, DeepSeek, Mistral, Together, Cerebras, xAI,
 * GitHub Models, локальные Ollama / LM Studio / llama.cpp и другие.
 */
class OpenAiCompatClient(private val cfg: AiConfig) : AiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun chat(messages: List<AiMessage>, onDelta: (String) -> Unit): String =
        withContext(Dispatchers.IO) {
            val url = cfg.baseUrl.trimEnd('/') + "/chat/completions"

            val payload = JSONObject().apply {
                put("model", cfg.model)
                put("stream", true)
                put("temperature", cfg.temperature)
                put("max_tokens", cfg.maxTokens)
                put("messages", JSONArray().apply {
                    messages.forEach {
                        put(JSONObject().put("role", it.role).put("content", it.content))
                    }
                })
            }

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${cfg.apiKey}")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    error("HTTP " + resp.code + ": " + resp.body?.string())
                }
                val sb = StringBuilder()
                val raw = StringBuilder()
                var sawSse = false
                val source = resp.body?.source() ?: error("Пустой ответ")

                while (true) {
                    val line = source.readUtf8Line() ?: break
                    if (line.isBlank()) continue
                    if (line.startsWith("data:")) {
                        sawSse = true
                        val data = line.removePrefix("data:").trim()
                        if (data.isEmpty()) continue
                        try {
                            val json = JSONObject(data)
                            val choice = json.optJSONArray("choices")?.optJSONObject(0)
                            val delta = choice?.optJSONObject("delta")?.optString("content")
                            val text = if (!delta.isNullOrEmpty()) delta
                                else choice?.optJSONObject("message")?.optString("content").orEmpty()
                            if (text.isNotEmpty()) {
                                sb.append(text)
                                onDelta(text)
                            }
                        } catch (_: JSONException) {
                        }
                    } else {
                        raw.append(line)
                    }
                }

                if (!sawSse) {
                    try {
                        val json = JSONObject(raw.toString())
                        val text = json.optJSONArray("choices")?.optJSONObject(0)
                            ?.optJSONObject("message")?.optString("content").orEmpty()
                        if (text.isNotEmpty()) {
                            sb.append(text)
                            onDelta(text)
                        }
                    } catch (_: JSONException) {
                        if (sb.isEmpty()) error("Пустой ответ сервера")
                    }
                }
                sb.toString()
            }
        }
}
