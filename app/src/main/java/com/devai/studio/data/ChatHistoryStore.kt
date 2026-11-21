package com.devai.studio.data

import android.content.Context
import com.devai.studio.ai.AiMessage
import org.json.JSONArray
import org.json.JSONObject

object ChatHistoryStore {
    private const val PREFS = "devai_chat"
    private const val KEY = "history"

    fun load(ctx: Context): MutableList<AiMessage> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return mutableListOf()
        val list = mutableListOf<AiMessage>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(AiMessage(o.optString("role"), o.optString("content")))
            }
        } catch (_: Exception) {
        }
        return list
    }

    fun save(ctx: Context, messages: List<AiMessage>) {
        val arr = JSONArray()
        messages.forEach {
            arr.put(JSONObject().put("role", it.role).put("content", it.content))
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}