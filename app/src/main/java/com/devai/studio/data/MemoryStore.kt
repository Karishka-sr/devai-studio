package com.devai.studio.data

import android.content.Context
import org.json.JSONArray

object MemoryStore {
    private const val PREFS = "devai_memory"
    private const val KEY_ITEMS = "items"
    private const val MAX_ITEMS = 40
    private const val MAX_LENGTH = 280

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun list(ctx: Context): List<String> {
        val raw = prefs(ctx).getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
        }.getOrDefault(emptyList())
    }

    fun add(ctx: Context, fact: String): Boolean {
        val clean = fact.trim().replace(Regex("\\s+"), " ").take(MAX_LENGTH)
        if (clean.isBlank()) return false
        val current = list(ctx).toMutableList()
        if (current.any { it.equals(clean, ignoreCase = true) }) return false
        current.add(0, clean)
        save(ctx, current.take(MAX_ITEMS))
        return true
    }

    fun remove(ctx: Context, fact: String) {
        save(ctx, list(ctx).filterNot { it == fact })
    }

    fun clear(ctx: Context) {
        prefs(ctx).edit().remove(KEY_ITEMS).apply()
    }

    fun promptText(ctx: Context): String {
        val items = list(ctx)
        if (items.isEmpty()) return ""
        return items.joinToString("\n") { "- $it" }
    }

    private fun save(ctx: Context, items: List<String>) {
        prefs(ctx).edit().putString(KEY_ITEMS, JSONArray(items).toString()).apply()
    }
}
