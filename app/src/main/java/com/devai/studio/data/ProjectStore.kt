package com.devai.studio.data

import android.content.Context
import com.devai.studio.builder.ZipSaver
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Project(
    val id: String,
    val name: String,
    val type: String,
    val updatedAt: Long
)

object ProjectStore {
    private const val PREFS = "devai_projects"
    private const val KEY = "list"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun root(ctx: Context) = File(ctx.filesDir, "projects").apply { mkdirs() }
    private fun dir(ctx: Context, id: String) = File(root(ctx), id)
    private fun filesDir(ctx: Context, id: String) = File(dir(ctx, id), "files").apply { mkdirs() }

    fun list(ctx: Context): List<Project> {
        val raw = prefs(ctx).getString(KEY, null) ?: return emptyList()
        val out = mutableListOf<Project>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    Project(
                        o.getString("id"),
                        o.getString("name"),
                        o.optString("type", "android"),
                        o.optLong("updatedAt", 0)
                    )
                )
            }
        } catch (_: Exception) {
        }
        return out.sortedByDescending { it.updatedAt }
    }

    private fun saveList(ctx: Context, list: List<Project>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("type", it.type)
                    .put("updatedAt", it.updatedAt)
            )
        }
        prefs(ctx).edit().putString(KEY, arr.toString()).apply()
    }

    fun create(ctx: Context, name: String, type: String): Project {
        val id = "p" + System.currentTimeMillis().toString(36) + (1000..9999).random()
        val p = Project(id, name.ifBlank { "Проект" }, type, System.currentTimeMillis())
        saveList(ctx, list(ctx) + p)
        filesDir(ctx, id)
        return p
    }

    fun delete(ctx: Context, id: String) {
        saveList(ctx, list(ctx).filterNot { it.id == id })
        dir(ctx, id).deleteRecursively()
    }

    fun rename(ctx: Context, id: String, name: String) {
        saveList(ctx, list(ctx).map {
            if (it.id == id) it.copy(name = name, updatedAt = System.currentTimeMillis()) else it
        })
    }

    fun touch(ctx: Context, id: String) {
        saveList(ctx, list(ctx).map {
            if (it.id == id) it.copy(updatedAt = System.currentTimeMillis()) else it
        })
    }

    fun get(ctx: Context, id: String): Project? = list(ctx).firstOrNull { it.id == id }

    fun files(ctx: Context, id: String): List<String> {
        val base = filesDir(ctx, id)
        if (!base.exists()) return emptyList()
        return base.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(base).invariantSeparatorsPath }
            .sorted()
            .toList()
    }

    fun read(ctx: Context, id: String, path: String): String? {
        val f = safeFile(ctx, id, path) ?: return null
        return try {
            f.readText()
        } catch (_: Exception) {
            null
        }
    }

    fun write(ctx: Context, id: String, path: String, content: String): Boolean {
        val f = safeFile(ctx, id, path) ?: return false
        return try {
            f.parentFile?.mkdirs()
            f.writeText(content)
            touch(ctx, id)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun deleteFile(ctx: Context, id: String, path: String): Boolean {
        val f = safeFile(ctx, id, path) ?: return false
        return f.delete()
    }

    fun import(ctx: Context, id: String, files: Map<String, String>): Int {
        var count = 0
        files.forEach { (path, content) -> if (write(ctx, id, path, content)) count++ }
        return count
    }

    fun exportZip(ctx: Context, id: String): ByteArray {
        val map = LinkedHashMap<String, String>()
        files(ctx, id).forEach { path -> read(ctx, id, path)?.let { map[path] = it } }
        return ZipSaver.buildZip(map)
    }

    private fun safeFile(ctx: Context, id: String, path: String): File? {
        val clean = path.replace('\\', '/').trimStart('/')
        if (clean.isBlank() || clean.contains("..")) return null
        val base = filesDir(ctx, id).canonicalFile
        val f = File(base, clean).canonicalFile
        return if (f.path.startsWith(base.path + File.separator)) f else null
    }
}
