package com.devai.studio.ai

import com.devai.studio.data.McpServer
import com.devai.studio.data.McpStore
import com.devai.studio.data.McpTool
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Минимальный MCP-клиент для Streamable HTTP:
 * initialize -> notifications/initialized -> tools/list -> tools/call.
 */
class McpClient(
    private val server: McpServer,
    private val token: String?,
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var sessionId: String? = null
    private var nextId = 1
    private var initialized = false

    suspend fun listTools(): List<McpTool> = withContext(Dispatchers.IO) {
        initialize()
        val result = rpc("tools/list", JSONObject())
        val arr = result.optJSONArray("tools") ?: JSONArray()
        val out = mutableListOf<McpTool>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("name")
            if (name.isBlank()) continue
            out.add(
                McpTool(
                    serverId = server.id,
                    serverName = server.name,
                    name = name,
                    description = o.optString("description"),
                    inputSchema = o.optJSONObject("inputSchema")?.toString()
                        ?: "{\"type\":\"object\",\"properties\":{}}",
                    functionName = McpStore.functionName(server.id, name),
                )
            )
        }
        out
    }

    suspend fun callTool(toolName: String, arguments: JSONObject): String =
        withContext(Dispatchers.IO) {
            initialize()
            val result = rpc(
                "tools/call",
                JSONObject().put("name", toolName).put("arguments", arguments),
            )
            val content = result.optJSONArray("content")
            val sb = StringBuilder()
            if (content != null) {
                for (i in 0 until content.length()) {
                    val item = content.optJSONObject(i) ?: continue
                    val text = item.optString("text")
                    if (text.isNotBlank()) sb.append(text).append("\n")
                    else sb.append(item.toString()).append("\n")
                }
            }
            val text = sb.toString().trim().ifBlank { result.toString() }
            if (result.optBoolean("isError")) "Ошибка инструмента: $text" else text
        }

    private fun initialize() {
        if (initialized) return
        rpc(
            "initialize",
            JSONObject().apply {
                put("protocolVersion", "2024-11-05")
                put("capabilities", JSONObject())
                put("clientInfo", JSONObject().put("name", "DevAI Studio").put("version", "2.10.0"))
            },
        )
        notify("notifications/initialized", JSONObject())
        initialized = true
    }

    private fun rpc(method: String, params: JSONObject): JSONObject {
        val id = nextId++
        val payload = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", id)
            .put("method", method)
            .put("params", params)
        val body = post(payload)
        val json = parseResponse(body, id)
        val error = json.optJSONObject("error")
        if (error != null) {
            error("MCP ${error.optInt("code")}: ${error.optString("message")}")
        }
        return json.optJSONObject("result") ?: JSONObject()
    }

    private fun notify(method: String, params: JSONObject) {
        val payload = JSONObject()
            .put("jsonrpc", "2.0")
            .put("method", method)
            .put("params", params)
        post(payload)
    }

    private fun post(payload: JSONObject): String {
        val builder = Request.Builder()
            .url(server.url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .header("MCP-Protocol-Version", "2024-11-05")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
        token?.takeIf { it.isNotBlank() }?.let { builder.header("Authorization", "Bearer $it") }
        sessionId?.let { builder.header("Mcp-Session-Id", it) }

        http.newCall(builder.build()).execute().use { resp ->
            sessionId = resp.header("Mcp-Session-Id") ?: sessionId
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                error("MCP HTTP ${resp.code}: ${body.take(500)}")
            }
            return body
        }
    }

    private fun parseResponse(body: String, id: Int): JSONObject {
        val candidates = mutableListOf<String>()
        if (body.contains("data:")) {
            body.lineSequence().forEach { line ->
                if (line.startsWith("data:")) {
                    val data = line.removePrefix("data:").trim()
                    if (data.isNotEmpty()) candidates.add(data)
                }
            }
        }
        candidates.add(body)
        for (candidate in candidates) {
            if (candidate.isBlank()) continue
            try {
                val obj = JSONObject(candidate)
                val responseId = obj.opt("id")?.toString()
                if (responseId == id.toString()) return obj
            } catch (_: Exception) {
            }
        }
        error("MCP: некорректный ответ сервера")
    }
}
