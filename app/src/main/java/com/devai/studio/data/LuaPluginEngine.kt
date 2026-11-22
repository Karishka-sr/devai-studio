package com.devai.studio.data

import org.json.JSONArray
import org.json.JSONObject
import org.luaj.vm2.Globals
import org.luaj.vm2.LoadState
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.BaseLib
import org.luaj.vm2.lib.MathLib
import org.luaj.vm2.lib.PackageLib
import org.luaj.vm2.lib.StringLib
import org.luaj.vm2.lib.TableLib

data class LuaCommand(
    val name: String,
    val description: String,
    val prompt: String,
    val run: LuaValue?,
)

data class LuaTool(
    val name: String,
    val description: String,
    val parameters: String,
    val run: LuaValue,
)

data class LuaPlugin(
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val commands: List<LuaCommand>,
    val tools: List<LuaTool>,
)

/**
 * Исполняет .devai-плагины на Lua (LuaJ, MIT).
 * Песочница: только base/table/string/math; io, os, package, require,
 * dofile/loadfile/load и luajava отключены.
 */
object LuaPluginEngine {
    private val blocked = listOf(
        "dofile", "loadfile", "load", "loadstring", "require",
        "package", "io", "os", "debug", "luajava",
    )

    fun load(text: String): LuaPlugin? = loadResult(text).getOrNull()

    fun loadResult(text: String): Result<LuaPlugin> = runCatching {
        val globals = Globals()
        globals.load(BaseLib())
        globals.load(PackageLib())
        globals.load(TableLib())
        globals.load(StringLib())
        globals.load(MathLib())
        LoadState.install(globals)
        LuaC.install(globals)
        blocked.forEach { globals.set(it, LuaValue.NIL) }

        val result = globals.load(text, "plugin").call()
        if (!result.istable()) error("Плагин должен вернуть таблицу")

        val name = result.get("name").optjstring("").ifBlank { "Плагин" }
        val description = result.get("description").optjstring("")
        val version = result.get("version").optjstring("").ifBlank { "1.0" }
        val author = result.get("author").optjstring("")

        val commands = mutableListOf<LuaCommand>()
        val commandTable = result.get("commands")
        if (commandTable.istable()) {
            forEachEntry(commandTable) { key, value ->
                if (!value.istable()) return@forEachEntry
                val commandName = value.get("name").optjstring("")
                    .ifBlank { key.optjstring("") }
                if (commandName.isBlank()) return@forEachEntry
                commands.add(
                    LuaCommand(
                        name = commandName,
                        description = value.get("description").optjstring(""),
                        prompt = value.get("prompt").optjstring(""),
                        run = value.get("run").takeIf { it.isfunction() },
                    )
                )
            }
        }

        val tools = mutableListOf<LuaTool>()
        val toolTable = result.get("tools")
        if (toolTable.istable()) {
            forEachEntry(toolTable) { key, value ->
                if (!value.istable()) return@forEachEntry
                val toolName = value.get("name").optjstring("")
                    .ifBlank { key.optjstring("") }
                val run = value.get("run")
                if (toolName.isBlank() || !run.isfunction()) return@forEachEntry
                val params = value.get("parameters")
                tools.add(
                    LuaTool(
                        name = toolName,
                        description = value.get("description").optjstring(""),
                        parameters = when {
                            params.isstring() -> params.tojstring()
                            params.istable() -> luaToJson(params)
                            else -> "{\"type\":\"object\",\"properties\":{}}"
                        },
                        run = run,
                    )
                )
            }
        }

        LuaPlugin(name, description, version, author, commands, tools)
    }

    fun run(command: LuaCommand, input: String): String = synchronized(this) {
        val fn = command.run
        if (fn != null) {
            runCatching { fn.call(LuaValue.valueOf(input)).tojstring() }
                .getOrElse { "Ошибка плагина: ${it.message}" }
        } else {
            command.prompt.replace("{{input}}", input.trim())
        }
    }

    fun run(tool: LuaTool, args: JSONObject): String = synchronized(this) {
        runCatching {
            val table = LuaTable()
            args.keys().forEach { key ->
                val value = args.opt(key)
                table.set(key, toLua(value))
            }
            tool.run.call(table).tojstring()
        }.getOrElse { "Ошибка инструмента плагина: ${it.message}" }
    }

    private fun forEachEntry(table: LuaValue, action: (LuaValue, LuaValue) -> Unit) {
        if (!table.istable()) return
        val luaTable = table as LuaTable
        val keys = luaTable.keys()
        for (key in keys) action(key, luaTable.get(key))
    }

    private fun toLua(value: Any?): LuaValue = when (value) {
        null, JSONObject.NULL -> LuaValue.NIL
        is Boolean -> LuaValue.valueOf(value)
        is Int -> LuaValue.valueOf(value)
        is Long -> LuaValue.valueOf(value.toDouble())
        is Double -> LuaValue.valueOf(value)
        is Number -> LuaValue.valueOf(value.toDouble())
        is JSONArray -> {
            val table = LuaTable()
            for (i in 0 until value.length()) table.set(i + 1, toLua(value.opt(i)))
            table
        }
        is JSONObject -> {
            val table = LuaTable()
            value.keys().forEach { key -> table.set(key, toLua(value.opt(key))) }
            table
        }
        else -> LuaValue.valueOf(value.toString())
    }

    private fun luaToJson(value: LuaValue): String {
        if (!value.istable()) return JSONObject.quote(value.tojstring())
        val table = value as LuaTable
        val keys = table.keys()
        val arrayLike = keys.isNotEmpty() && keys.all { it.isint() && it.toint() >= 1 }
        return if (arrayLike) {
            val arr = JSONArray()
            keys.sortedBy { it.toint() }.forEach { arr.put(luaToJson(table.get(it))) }
            arr.toString()
        } else {
            val obj = JSONObject()
            keys.forEach { key ->
                obj.put(key.tojstring(), luaToJson(table.get(key)))
            }
            obj.toString()
        }
    }
}
