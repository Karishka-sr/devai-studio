package com.devai.studio.data

import android.content.Context

object AppPrefs {
    private const val PREFS = "devai_prefs"
    private const val KEY_PROVIDER = "active_provider"
    private const val KEY_MODEL = "active_model"
    private const val KEY_TEMP = "temperature"
    private const val KEY_MAX_TOKENS = "max_tokens"
    private const val KEY_CUSTOM_BASE = "custom_base"
    private const val KEY_CUSTOM_MODEL = "custom_model"
    private const val KEY_SYSTEM_PROMPT = "system_prompt"
    private const val KEY_THEME = "theme"
    private const val KEY_TOP_P = "top_p"
    private const val KEY_HISTORY_LIMIT = "history_limit"
    private const val KEY_TIMEOUT = "timeout_seconds"
    private const val KEY_LOCAL_CONTEXT = "local_context_tokens"
    private const val KEY_LOCAL_TOP_K = "local_top_k"
    private const val KEY_CHAT_FONT = "chat_font_size"
    private const val KEY_CODE_WRAP = "code_wrap"
    private const val KEY_SEND_ON_ENTER = "send_on_enter"
    private const val KEY_AUTO_SCROLL = "auto_scroll"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color"

    const val DEFAULT_SYSTEM_PROMPT =
        "Ты — DevAI, полезный ИИ-ассистент разработчика. Отвечай по делу, давай рабочие примеры кода."

    private fun p(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun theme(ctx: Context): String = p(ctx).getString(KEY_THEME, "m3") ?: "m3"
    fun setTheme(ctx: Context, id: String) = p(ctx).edit().putString(KEY_THEME, id).apply()

    fun activeProvider(ctx: Context): String? = p(ctx).getString(KEY_PROVIDER, null)
    fun setActiveProvider(ctx: Context, id: String) = p(ctx).edit().putString(KEY_PROVIDER, id).apply()

    fun activeModel(ctx: Context): String? = p(ctx).getString(KEY_MODEL, null)
    fun setActiveModel(ctx: Context, model: String) = p(ctx).edit().putString(KEY_MODEL, model).apply()

    fun modelFor(ctx: Context, providerId: String): String? =
        p(ctx).getString("model_" + providerId, null)

    fun setModelFor(ctx: Context, providerId: String, model: String) =
        p(ctx).edit().putString("model_" + providerId, model).apply()

    fun temperature(ctx: Context): Float = p(ctx).getFloat(KEY_TEMP, 0.7f)
    fun setTemperature(ctx: Context, v: Float) = p(ctx).edit().putFloat(KEY_TEMP, v).apply()

    fun maxTokens(ctx: Context): Int = p(ctx).getInt(KEY_MAX_TOKENS, 4096)
    fun setMaxTokens(ctx: Context, v: Int) = p(ctx).edit().putInt(KEY_MAX_TOKENS, v).apply()

    fun customBase(ctx: Context): String = p(ctx).getString(KEY_CUSTOM_BASE, "") ?: ""
    fun setCustomBase(ctx: Context, v: String) = p(ctx).edit().putString(KEY_CUSTOM_BASE, v).apply()

    fun customModel(ctx: Context): String = p(ctx).getString(KEY_CUSTOM_MODEL, "") ?: ""
    fun setCustomModel(ctx: Context, v: String) = p(ctx).edit().putString(KEY_CUSTOM_MODEL, v).apply()

    fun systemPrompt(ctx: Context): String {
        val v = p(ctx).getString(KEY_SYSTEM_PROMPT, null)
        return if (v.isNullOrBlank()) DEFAULT_SYSTEM_PROMPT else v
    }

    fun setSystemPrompt(ctx: Context, v: String) =
        p(ctx).edit().putString(KEY_SYSTEM_PROMPT, v).apply()

    fun topP(ctx: Context): Float = p(ctx).getFloat(KEY_TOP_P, 0.95f)
    fun setTopP(ctx: Context, v: Float) = p(ctx).edit().putFloat(KEY_TOP_P, v).apply()

    fun historyLimit(ctx: Context): Int = p(ctx).getInt(KEY_HISTORY_LIMIT, 40)
    fun setHistoryLimit(ctx: Context, v: Int) = p(ctx).edit().putInt(KEY_HISTORY_LIMIT, v).apply()

    fun timeoutSeconds(ctx: Context): Int = p(ctx).getInt(KEY_TIMEOUT, 180)
    fun setTimeoutSeconds(ctx: Context, v: Int) = p(ctx).edit().putInt(KEY_TIMEOUT, v).apply()

    fun localContextTokens(ctx: Context): Int = p(ctx).getInt(KEY_LOCAL_CONTEXT, 1280)
    fun setLocalContextTokens(ctx: Context, v: Int) =
        p(ctx).edit().putInt(KEY_LOCAL_CONTEXT, v).apply()

    fun localTopK(ctx: Context): Int = p(ctx).getInt(KEY_LOCAL_TOP_K, 40)
    fun setLocalTopK(ctx: Context, v: Int) = p(ctx).edit().putInt(KEY_LOCAL_TOP_K, v).apply()

    fun chatFontSize(ctx: Context): Float = p(ctx).getFloat(KEY_CHAT_FONT, 15f)
    fun setChatFontSize(ctx: Context, v: Float) = p(ctx).edit().putFloat(KEY_CHAT_FONT, v).apply()

    fun codeWrap(ctx: Context): Boolean = p(ctx).getBoolean(KEY_CODE_WRAP, false)
    fun setCodeWrap(ctx: Context, v: Boolean) = p(ctx).edit().putBoolean(KEY_CODE_WRAP, v).apply()

    fun sendOnEnter(ctx: Context): Boolean = p(ctx).getBoolean(KEY_SEND_ON_ENTER, true)
    fun setSendOnEnter(ctx: Context, v: Boolean) =
        p(ctx).edit().putBoolean(KEY_SEND_ON_ENTER, v).apply()

    fun autoScroll(ctx: Context): Boolean = p(ctx).getBoolean(KEY_AUTO_SCROLL, true)
    fun setAutoScroll(ctx: Context, v: Boolean) = p(ctx).edit().putBoolean(KEY_AUTO_SCROLL, v).apply()

    fun dynamicColor(ctx: Context): Boolean = p(ctx).getBoolean(KEY_DYNAMIC_COLOR, true)
    fun setDynamicColor(ctx: Context, v: Boolean) =
        p(ctx).edit().putBoolean(KEY_DYNAMIC_COLOR, v).apply()

    fun reset(ctx: Context) {
        p(ctx).edit().clear().apply()
    }

    fun exportJson(ctx: Context): String = org.json.JSONObject().apply {
        put("temperature", temperature(ctx))
        put("topP", topP(ctx))
        put("maxTokens", maxTokens(ctx))
        put("historyLimit", historyLimit(ctx))
        put("timeoutSeconds", timeoutSeconds(ctx))
        put("localContextTokens", localContextTokens(ctx))
        put("localTopK", localTopK(ctx))
        put("chatFontSize", chatFontSize(ctx))
        put("codeWrap", codeWrap(ctx))
        put("sendOnEnter", sendOnEnter(ctx))
        put("autoScroll", autoScroll(ctx))
        put("dynamicColor", dynamicColor(ctx))
        put("theme", theme(ctx))
        put("systemPrompt", systemPrompt(ctx))
        put("provider", activeProvider(ctx).orEmpty())
        put("model", activeModel(ctx).orEmpty())
    }.toString(2)

    fun modelsFor(ctx: Context, providerId: String): List<String> {
        val raw = p(ctx).getString("models_" + providerId, null) ?: return emptyList()
        val out = mutableListOf<String>()
        try {
            val arr = org.json.JSONArray(raw)
            for (i in 0 until arr.length()) {
                arr.optString(i).takeIf { it.isNotBlank() }?.let { out.add(it) }
            }
        } catch (_: Exception) {
        }
        return out
    }

    fun setModelsFor(ctx: Context, providerId: String, models: List<String>) {
        p(ctx).edit()
            .putString("models_" + providerId, org.json.JSONArray(models).toString())
            .apply()
    }
}