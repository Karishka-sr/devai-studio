package com.devai.studio.ai

import org.json.JSONArray
import org.json.JSONObject

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String,
)

data class ToolResult(
    val content: String?,
    val calls: List<ToolCall>,
)

/** Инструмент, который модель может вызвать: MCP-сервер или Lua-плагин. */
data class AgentTool(
    val functionName: String,
    val description: String,
    val inputSchema: String,
    val call: suspend (JSONObject) -> String,
)

/**
 * Небольшой агентный цикл для OpenAI-совместимых API:
 * модель может попросить инструмент, приложение выполняет его
 * и возвращает результат обратно модели. Максимум 3 раунда.
 */
object ToolAgent {
    suspend fun chat(
        cfg: AiConfig,
        messages: List<AiMessage>,
        tools: List<AgentTool>,
        onDelta: (String) -> Unit,
    ): String {
        val client = OpenAiCompatClient(cfg)
        val raw = messages.mapTo(mutableListOf()) {
            JSONObject().put("role", it.role).put("content", it.content)
        }

        repeat(3) { round ->
            val result = try {
                client.chatWithTools(raw, tools)
            } catch (e: Exception) {
                if (round == 0) {
                    // Провайдер не умеет tools — работаем как обычный чат.
                    return client.chat(messages, onDelta)
                }
                return "Ошибка инструментов: ${e.message ?: e.javaClass.simpleName}"
            }

            if (result.calls.isEmpty()) {
                val text = result.content.orEmpty()
                if (text.isNotBlank()) onDelta(text)
                return text
            }

            onDelta("🔧 " + result.calls.joinToString { it.name } + "\n\n")
            raw.add(
                JSONObject()
                    .put("role", "assistant")
                    .put("content", result.content ?: JSONObject.NULL)
                    .put(
                        "tool_calls",
                        JSONArray().apply {
                            result.calls.forEach { call ->
                                put(
                                    JSONObject()
                                        .put("id", call.id)
                                        .put("type", "function")
                                        .put(
                                            "function",
                                            JSONObject()
                                                .put("name", call.name)
                                                .put("arguments", call.arguments)
                                        )
                                )
                            }
                        }
                    )
            )

            result.calls.forEach { call ->
                val tool = tools.firstOrNull { it.functionName == call.name }
                val output = if (tool == null) {
                    "Инструмент не найден: ${call.name}"
                } else {
                    runCatching {
                        val args = runCatching { JSONObject(call.arguments) }
                            .getOrElse { JSONObject() }
                        tool.call(args)
                    }.getOrElse { e ->
                        "Ошибка инструмента ${tool.functionName}: ${e.message ?: e.javaClass.simpleName}"
                    }
                }.take(12000)
                raw.add(
                    JSONObject()
                        .put("role", "tool")
                        .put("tool_call_id", call.id)
                        .put("content", output)
                )
            }
        }
        return "Не удалось завершить вызов инструментов за 3 раунда."
    }

    fun toolDefinitions(tools: List<AgentTool>): JSONArray = JSONArray().apply {
        tools.forEach { tool ->
            val schema = runCatching { JSONObject(tool.inputSchema) }
                .getOrElse { JSONObject().put("type", "object").put("properties", JSONObject()) }
            put(
                JSONObject()
                    .put("type", "function")
                    .put(
                        "function",
                        JSONObject()
                            .put("name", tool.functionName)
                            .put("description", tool.description.ifBlank { tool.functionName })
                            .put("parameters", schema)
                    )
            )
        }
    }
}
