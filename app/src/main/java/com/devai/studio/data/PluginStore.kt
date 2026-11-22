package com.devai.studio.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Плагин DevAI (.devai).
 * Поддерживает Lua (LuaJ) и старый декларативный DPL-формат.
 */
data class Plugin(
    val id: String,
    val name: String,
    val description: String,
    val command: String,
    val prompt: String,
    val version: String,
    val author: String,
    val fileName: String,
    val commands: List<LuaCommand> = emptyList(),
    val tools: List<LuaTool> = emptyList(),
    val lua: Boolean = false,
) {
    fun render(input: String): String =
        prompt.replace("{{input}}", input.trim()).trim()
}

data class PluginToolBinding(
    val plugin: Plugin,
    val tool: LuaTool,
) {
    val functionName: String = PluginStore.functionName(plugin.id, tool.name)
}

object PluginParser {
    fun parse(text: String, fallbackName: String): Plugin? {
        val values = mutableMapOf<String, String>()
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) return@forEach
            val eq = line.indexOf('=')
            if (eq <= 0) return@forEach
            val key = line.substring(0, eq).trim().lowercase()
            val value = line.substring(eq + 1).trim()
                .removeSurrounding("\"")
                .replace("\\n", "\n")
            values[key] = value
        }
        val name = values["name"]?.takeIf { it.isNotBlank() } ?: fallbackName
        val prompt = values["prompt"]?.takeIf { it.isNotBlank() } ?: return null
        val rawId = values["id"]?.takeIf { it.isNotBlank() } ?: name
        val id = rawId.lowercase()
            .replace(Regex("[^a-z0-9а-я_-]+"), "-")
            .trim('-')
            .ifBlank { UUID.randomUUID().toString().take(8) }
        return Plugin(
            id = id,
            name = name,
            description = values["description"].orEmpty(),
            command = values["command"]?.takeIf { it.isNotBlank() } ?: "/$id",
            prompt = prompt,
            version = values["version"]?.takeIf { it.isNotBlank() } ?: "1.0",
            author = values["author"].orEmpty(),
            fileName = "$id.devai",
        )
    }
}

object PluginStore {
    private const val SAMPLE_SUMMARIZER = "plugins/summarizer.devai"
    private const val SAMPLE_REVIEW = "plugins/code-review.devai"
    private const val SAMPLE_LUA = "plugins/lua-tools.devai"

    private fun dir(ctx: Context): File =
        File(ctx.filesDir, "plugins").apply { mkdirs() }

    fun list(ctx: Context): List<Plugin> {
        ensureSamples(ctx)
        return dir(ctx).listFiles { file -> file.isFile && file.name.endsWith(".devai") }
            .orEmpty()
            .mapNotNull { file ->
                runCatching {
                    parse(file.nameWithoutExtension, file.readText(Charsets.UTF_8), file.name)
                }.getOrNull()
            }
            .sortedBy { it.name.lowercase() }
    }

    fun import(ctx: Context, uri: Uri): Plugin {
        val text = ctx.contentResolver.openInputStream(uri)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: error("Не удалось прочитать файл")
        val fallback = "plugin-" + System.currentTimeMillis()
        val plugin = parse(fallback, text, "$fallback.devai")
            ?: error("Плагин не загрузился: проверьте Lua/DPL синтаксис")
        val target = File(dir(ctx), plugin.fileName)
        target.writeText(text, Charsets.UTF_8)
        return plugin
    }

    fun delete(ctx: Context, plugin: Plugin) {
        File(dir(ctx), plugin.fileName).delete()
    }

    fun runCommand(plugin: Plugin, command: LuaCommand?, input: String): String {
        if (command == null) return plugin.render(input)
        return LuaPluginEngine.run(command, input)
    }

    fun callTool(binding: PluginToolBinding, args: org.json.JSONObject): String =
        LuaPluginEngine.run(binding.tool, args)

    fun toolBindings(ctx: Context): List<PluginToolBinding> =
        list(ctx).flatMap { plugin ->
            plugin.tools.map { PluginToolBinding(plugin, it) }
        }

    fun functionName(pluginId: String, toolName: String): String {
        val clean = ("plugin_" + pluginId + "_" + toolName)
            .replace(Regex("[^A-Za-z0-9_]+"), "_")
            .trim('_')
        return clean.take(64)
    }

    private fun parse(fallbackId: String, text: String, fileName: String): Plugin? {
        val lua = if (text.contains("return") || text.contains("function") || text.contains("commands")) {
            LuaPluginEngine.load(text)
        } else {
            null
        }
        if (lua != null) {
            val id = fallbackId.lowercase()
                .replace(Regex("[^a-z0-9а-я_-]+"), "-")
                .trim('-')
                .ifBlank { "plugin-" + UUID.randomUUID().toString().take(8) }
            return Plugin(
                id = id,
                name = lua.name,
                description = lua.description,
                command = lua.commands.firstOrNull()?.name.orEmpty(),
                prompt = lua.commands.firstOrNull()?.prompt.orEmpty(),
                version = lua.version,
                author = lua.author,
                fileName = if (fileName.endsWith(".devai")) fileName else "$id.devai",
                commands = lua.commands,
                tools = lua.tools,
                lua = true,
            )
        }
        return PluginParser.parse(text, fallbackId)
    }

    private fun ensureSamples(ctx: Context) {
        val targetDir = dir(ctx)
        listOf(SAMPLE_SUMMARIZER, SAMPLE_REVIEW, SAMPLE_LUA).forEach { assetName ->
            val name = assetName.substringAfterLast('/')
            val target = File(targetDir, name)
            if (target.exists()) return@forEach
            runCatching {
                ctx.assets.open(assetName).use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }
}
