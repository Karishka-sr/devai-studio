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

    private val timeoutSeconds = cfg.timeoutSeconds.coerceIn(15, 600).toLong()
    private val client = OkHttpClient.Builder()
        .connectTimeout(minOf(30L, timeoutSeconds), TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(minOf(60L, timeoutSeconds), TimeUnit.SECONDS)
        .build()

    override suspend fun chat(messages: List<AiMessage>, onDelta: (String) -> Unit): String =
        withContext(Dispatchers.IO) {
            val url = cfg.baseUrl.trimEnd('/') + "/chat/completions"

            val payload = JSONObject().apply {
                put("model", cfg.model)
                put("stream", true)
                put("temperature", cfg.temperature)
                put("top_p", cfg.topP)
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

    /**
     * Нестримовый запрос с tools. Нужен только MCP-агенту:
     * модель возвращает tool_calls, приложение выполняет их и повторяет запрос.
     */
    suspend fun chatWithTools(
        messages: List<JSONObject>,
        tools: List<AgentTool>,
    ): ToolResult = withContext(Dispatchers.IO) {
        val url = cfg.baseUrl.trimEnd('/') + "/chat/completions"
        val payload = JSONObject().apply {
            put("model", cfg.model)
            put("stream", false)
            put("temperature", cfg.temperature)
            put("top_p", cfg.topP)
            put("max_tokens", cfg.maxTokens)
            put("messages", JSONArray().apply { messages.forEach(::put) })
            put("tools", ToolAgent.toolDefinitions(tools))
            put("tool_choice", "auto")
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${cfg.apiKey}")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("HTTP ${resp.code}: $body")
            val message = JSONObject(body)
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?: error("Нет message в ответе")

            val content = if (message.isNull("content")) null
            else message.optString("content").takeIf { it.isNotBlank() }

            val calls = mutableListOf<ToolCall>()
            val arr = message.optJSONArray("tool_calls")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val function = item.optJSONObject("function") ?: continue
                    val name = function.optString("name")
                    if (name.isBlank()) continue
                    val args = function.opt("arguments")?.toString().orEmpty()
                    calls.add(
                        ToolCall(
                            id = item.optString("id").ifBlank { "call_$i" },
                            name = name,
                            arguments = args,
                        )
                    )
                }
            }
            ToolResult(content, calls)
        }
    }
}
