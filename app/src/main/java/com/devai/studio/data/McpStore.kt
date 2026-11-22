package com.devai.studio.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class McpServer(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true,
)

data class McpTool(
    val serverId: String,
    val serverName: String,
    val name: String,
    val description: String,
    val inputSchema: String,
    val functionName: String,
)

object McpStore {
    private const val PREFS = "devai_mcp"
    private const val KEY_SERVERS = "servers"
    private const val KEY_TOOLS = "tools"

    private fun p(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun tokenKey(serverId: String): String = "mcp_$serverId"

    fun listServers(ctx: Context): List<McpServer> {
        val raw = p(ctx).getString(KEY_SERVERS, null) ?: return emptyList()
        val out = mutableListOf<McpServer>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                out.add(
                    McpServer(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        url = o.optString("url"),
                        enabled = o.optBoolean("enabled", true),
                    )
                )
            }
        } catch (_: Exception) {
        }
        return out
    }

    fun saveServer(ctx: Context, server: McpServer): McpServer {
        val saved = server.copy(id = server.id.ifBlank { UUID.randomUUID().toString() })
        val all = listServers(ctx).filterNot { it.id == saved.id } + saved
        writeServers(ctx, all)
        return saved
    }

    fun deleteServer(ctx: Context, id: String) {
        writeServers(ctx, listServers(ctx).filterNot { it.id == id })
        clearTools(ctx)
    }

    fun setEnabled(ctx: Context, id: String, enabled: Boolean) {
        writeServers(ctx, listServers(ctx).map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    fun findServer(ctx: Context, id: String): McpServer? =
        listServers(ctx).firstOrNull { it.id == id }

    fun tools(ctx: Context): List<McpTool> {
        val raw = p(ctx).getString(KEY_TOOLS, null) ?: return emptyList()
        val out = mutableListOf<McpTool>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                out.add(
                    McpTool(
                        serverId = o.optString("serverId"),
                        serverName = o.optString("serverName"),
                        name = o.optString("name"),
                        description = o.optString("description"),
                        inputSchema = o.optString("inputSchema"),
                        functionName = o.optString("functionName"),
                    )
                )
            }
        } catch (_: Exception) {
        }
        return out
    }

    fun saveTools(ctx: Context, tools: List<McpTool>) {
        val arr = JSONArray()
        tools.forEach {
            arr.put(
                JSONObject()
                    .put("serverId", it.serverId)
                    .put("serverName", it.serverName)
                    .put("name", it.name)
                    .put("description", it.description)
                    .put("inputSchema", it.inputSchema)
                    .put("functionName", it.functionName)
            )
        }
        p(ctx).edit().putString(KEY_TOOLS, arr.toString()).apply()
    }

    fun clearTools(ctx: Context) {
        p(ctx).edit().remove(KEY_TOOLS).apply()
    }

    fun functionName(serverId: String, toolName: String): String {
        val clean = ("mcp_" + serverId + "_" + toolName)
            .replace(Regex("[^A-Za-z0-9_]+"), "_")
            .trim('_')
        return clean.take(64)
    }

    private fun writeServers(ctx: Context, servers: List<McpServer>) {
        val arr = JSONArray()
        servers.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("url", it.url)
                    .put("enabled", it.enabled)
            )
        }
        p(ctx).edit().putString(KEY_SERVERS, arr.toString()).apply()
    }
}
