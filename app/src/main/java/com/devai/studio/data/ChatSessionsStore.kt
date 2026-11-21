package com.devai.studio.data

import android.content.Context
import com.devai.studio.ai.AiMessage
import org.json.JSONArray
import org.json.JSONObject

data class ChatSession(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messages: MutableList<AiMessage>
)

object ChatSessionsStore {
    private const val PREFS = "devai_sessions"
    private const val KEY_LIST = "sessions"
    private const val KEY_CURRENT = "current"
    private const val DEFAULT_TITLE = "Новый диалог"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun parse(raw: String?): MutableList<ChatSession> {
        val out = mutableListOf<ChatSession>()
        if (raw.isNullOrBlank()) return out
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val msgs = mutableListOf<AiMessage>()
                val ma = o.optJSONArray("messages")
                if (ma != null) {
                    for (j in 0 until ma.length()) {
                        val m = ma.getJSONObject(j)
                        msgs.add(AiMessage(m.optString("role"), m.optString("content")))
                    }
                }
                out.add(
                    ChatSession(
                        o.optString("id"),
                        o.optString("title", DEFAULT_TITLE),
                        o.optLong("updatedAt", 0),
                        msgs
                    )
                )
            }
        } catch (_: Exception) {
        }
        return out
    }

    private fun serialize(list: List<ChatSession>): String {
        val arr = JSONArray()
        list.forEach { s ->
            val msgs = JSONArray()
            s.messages.forEach { m ->
                msgs.put(JSONObject().put("role", m.role).put("content", m.content))
            }
            arr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("title", s.title)
                    .put("updatedAt", s.updatedAt)
                    .put("messages", msgs)
            )
        }
        return arr.toString()
    }

    private fun saveAll(ctx: Context, list: List<ChatSession>) {
        prefs(ctx).edit().putString(KEY_LIST, serialize(list)).apply()
    }

    fun list(ctx: Context): List<ChatSession> =
        parse(prefs(ctx).getString(KEY_LIST, null)).sortedByDescending { it.updatedAt }

    fun currentId(ctx: Context): String? = prefs(ctx).getString(KEY_CURRENT, null)

    fun setCurrent(ctx: Context, id: String) =
        prefs(ctx).edit().putString(KEY_CURRENT, id).apply()

    fun create(ctx: Context, title: String = DEFAULT_TITLE): ChatSession {
        val s = ChatSession(
            "s" + System.currentTimeMillis().toString(36) + (100..999).random(),
            title,
            System.currentTimeMillis(),
            mutableListOf()
        )
        saveAll(ctx, list(ctx) + s)
        setCurrent(ctx, s.id)
        return s
    }

    fun getOrCreateCurrent(ctx: Context): ChatSession {
        val all = list(ctx)
        val cur = currentId(ctx)
        all.firstOrNull { it.id == cur }?.let { return it }

        val old = ChatHistoryStore.load(ctx)
        if (old.isNotEmpty()) {
            val s = ChatSession(
                "s" + System.currentTimeMillis().toString(36),
                titleFrom(old),
                System.currentTimeMillis(),
                old
            )
            saveAll(ctx, all + s)
            setCurrent(ctx, s.id)
            return s
        }
        if (all.isNotEmpty()) {
            setCurrent(ctx, all.first().id)
            return all.first()
        }
        return create(ctx)
    }

    fun updateMessages(ctx: Context, id: String, messages: List<AiMessage>) {
        saveAll(ctx, list(ctx).map { s ->
            if (s.id != id) s
            else {
                val title = if (s.title == DEFAULT_TITLE) titleFrom(messages) else s.title
                ChatSession(s.id, title, System.currentTimeMillis(), messages.toMutableList())
            }
        })
    }

    fun rename(ctx: Context, id: String, title: String) {
        saveAll(ctx, list(ctx).map { if (it.id == id) it.copy(title = title) else it })
    }

    fun delete(ctx: Context, id: String) {
        saveAll(ctx, list(ctx).filterNot { it.id == id })
        if (currentId(ctx) == id) prefs(ctx).edit().remove(KEY_CURRENT).apply()
    }

    private fun titleFrom(messages: List<AiMessage>): String {
        val first = messages.firstOrNull { it.role == "user" }?.content ?: return DEFAULT_TITLE
        val line = first.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        return if (line.isBlank()) DEFAULT_TITLE else line.take(40)
    }
}
