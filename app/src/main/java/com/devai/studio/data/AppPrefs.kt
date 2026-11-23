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
    private const val KEY_CHAT_MODE = "chat_mode"
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
    private const val KEY_TTS_ENABLED = "tts_enabled"
    private const val KEY_TTS_RATE = "tts_rate"
    private const val KEY_SEARCH_ENABLED = "search_enabled"
    private const val KEY_MCP_ENABLED = "mcp_enabled"
    private const val KEY_ACCENT = "accent_color"
    private const val KEY_CHAT_FONT_FAMILY = "chat_font_family"
    private const val KEY_BUBBLE_WIDTH = "bubble_width_dp"
    private const val KEY_SHOW_AVATARS = "show_avatars"
    private const val KEY_COMPACT_CHAT = "compact_chat"
    private const val KEY_HAPTIC = "haptic_feedback"
    private const val KEY_CODE_FONT_SIZE = "code_font_size"
    private const val KEY_CONFIRM_DELETE = "confirm_delete_chat"
    private const val KEY_RESPONSE_STYLE = "response_style"
    private const val KEY_RESPONSE_LANGUAGE = "response_language"
    private const val KEY_CODE_LANGUAGE = "code_language"
    private const val KEY_CHAT_WALLPAPER = "chat_wallpaper"
    private const val KEY_CHAT_WALLPAPER_DIM = "chat_wallpaper_dim"

    const val DEFAULT_SYSTEM_PROMPT =
        "Ты — DevAI, полезный ИИ-ассистент разработчика. Отвечай по делу, давай рабочие примеры кода. " +
            "Всегда отвечай на русском языке, если пользователь не попросил другой язык."

    const val DEV_SYSTEM_PROMPT =
        "Ты — DevAI, senior-разработчик. Помогай с кодом, архитектурой, ошибками и UI. " +
            "Сначала уточняй требования, если задача неоднозначна. Давай готовые примеры кода " +
            "и указывай, куда их вставлять. Всегда отвечай на русском языке."

    private const val LEGACY_DEFAULT_SYSTEM_PROMPT =
        "Ты — DevAI, полезный ИИ-ассистент разработчика. Отвечай по делу, давай рабочие примеры кода."
    private const val LEGACY_DEV_SYSTEM_PROMPT =
        "Ты — DevAI, senior-разработчик. Помогай с кодом, архитектурой, ошибками и UI. " +
            "Сначала уточняй требования, если задача неоднозначна. Давай готовые примеры кода " +
            "и указывай, куда их вставлять."

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
        return if (v.isNullOrBlank() || v == LEGACY_DEFAULT_SYSTEM_PROMPT) {
            DEFAULT_SYSTEM_PROMPT
        } else {
            v
        }
    }

    fun setSystemPrompt(ctx: Context, v: String) =
        p(ctx).edit().putString(KEY_SYSTEM_PROMPT, v).apply()

    fun chatMode(ctx: Context): String = p(ctx).getString(KEY_CHAT_MODE, ChatMode.CHAT.id)
        ?: ChatMode.CHAT.id

    fun setChatMode(ctx: Context, id: String) =
        p(ctx).edit().putString(KEY_CHAT_MODE, id).apply()

    fun systemPromptForMode(ctx: Context, mode: ChatMode): String {
        val custom = p(ctx).getString(KEY_SYSTEM_PROMPT, null)
        if (!custom.isNullOrBlank() &&
            custom != DEFAULT_SYSTEM_PROMPT &&
            custom != LEGACY_DEFAULT_SYSTEM_PROMPT
        ) {
            return custom
        }
        return if (mode == ChatMode.DEV) DEV_SYSTEM_PROMPT else DEFAULT_SYSTEM_PROMPT
    }

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

    fun ttsEnabled(ctx: Context): Boolean = p(ctx).getBoolean(KEY_TTS_ENABLED, false)
    fun setTtsEnabled(ctx: Context, v: Boolean) =
        p(ctx).edit().putBoolean(KEY_TTS_ENABLED, v).apply()

    fun ttsRate(ctx: Context): Float = p(ctx).getFloat(KEY_TTS_RATE, 1f)
    fun setTtsRate(ctx: Context, v: Float) = p(ctx).edit().putFloat(KEY_TTS_RATE, v).apply()

    fun searchEnabled(ctx: Context): Boolean = p(ctx).getBoolean(KEY_SEARCH_ENABLED, false)
    fun setSearchEnabled(ctx: Context, v: Boolean) =
        p(ctx).edit().putBoolean(KEY_SEARCH_ENABLED, v).apply()

    fun mcpEnabled(ctx: Context): Boolean = p(ctx).getBoolean(KEY_MCP_ENABLED, true)
    fun setMcpEnabled(ctx: Context, v: Boolean) =
        p(ctx).edit().putBoolean(KEY_MCP_ENABLED, v).apply()

    fun accent(ctx: Context): String = p(ctx).getString(KEY_ACCENT, "default") ?: "default"
    fun setAccent(ctx: Context, v: String) = p(ctx).edit().putString(KEY_ACCENT, v).apply()

    fun chatFontFamily(ctx: Context): String =
        p(ctx).getString(KEY_CHAT_FONT_FAMILY, "sans") ?: "sans"
    fun setChatFontFamily(ctx: Context, v: String) =
        p(ctx).edit().putString(KEY_CHAT_FONT_FAMILY, v).apply()

    fun bubbleWidth(ctx: Context): Int = p(ctx).getInt(KEY_BUBBLE_WIDTH, 320)
    fun setBubbleWidth(ctx: Context, v: Int) =
        p(ctx).edit().putInt(KEY_BUBBLE_WIDTH, v.coerceIn(220, 460)).apply()

    fun showAvatars(ctx: Context): Boolean = p(ctx).getBoolean(KEY_SHOW_AVATARS, true)
    fun setShowAvatars(ctx: Context, v: Boolean) =
        p(ctx).edit().putBoolean(KEY_SHOW_AVATARS, v).apply()

    fun compactChat(ctx: Context): Boolean = p(ctx).getBoolean(KEY_COMPACT_CHAT, false)
    fun setCompactChat(ctx: Context, v: Boolean) =
        p(ctx).edit().putBoolean(KEY_COMPACT_CHAT, v).apply()

    fun hapticFeedback(ctx: Context): Boolean = p(ctx).getBoolean(KEY_HAPTIC, true)
    fun setHapticFeedback(ctx: Context, v: Boolean) =
        p(ctx).edit().putBoolean(KEY_HAPTIC, v).apply()

    fun codeFontSize(ctx: Context): Float = p(ctx).getFloat(KEY_CODE_FONT_SIZE, 13f)
    fun setCodeFontSize(ctx: Context, v: Float) =
        p(ctx).edit().putFloat(KEY_CODE_FONT_SIZE, v.coerceIn(10f, 18f)).apply()

    fun confirmDelete(ctx: Context): Boolean = p(ctx).getBoolean(KEY_CONFIRM_DELETE, true)
    fun setConfirmDelete(ctx: Context, v: Boolean) =
        p(ctx).edit().putBoolean(KEY_CONFIRM_DELETE, v).apply()

    fun responseStyle(ctx: Context): String =
        p(ctx).getString(KEY_RESPONSE_STYLE, "balanced") ?: "balanced"
    fun setResponseStyle(ctx: Context, v: String) =
        p(ctx).edit().putString(KEY_RESPONSE_STYLE, v).apply()

    fun responseLanguage(ctx: Context): String =
        p(ctx).getString(KEY_RESPONSE_LANGUAGE, "ru") ?: "ru"
    fun setResponseLanguage(ctx: Context, v: String) =
        p(ctx).edit().putString(KEY_RESPONSE_LANGUAGE, v).apply()

    fun codeLanguage(ctx: Context): String =
        p(ctx).getString(KEY_CODE_LANGUAGE, CodeLanguages.DEFAULT_ID) ?: CodeLanguages.DEFAULT_ID
    fun setCodeLanguage(ctx: Context, v: String) =
        p(ctx).edit().putString(KEY_CODE_LANGUAGE, v).apply()

    fun chatWallpaper(ctx: Context): String =
        p(ctx).getString(KEY_CHAT_WALLPAPER, "none") ?: "none"
    fun setChatWallpaper(ctx: Context, v: String) =
        p(ctx).edit().putString(KEY_CHAT_WALLPAPER, v).apply()

    fun chatWallpaperDim(ctx: Context): Float =
        p(ctx).getFloat(KEY_CHAT_WALLPAPER_DIM, 0.55f).coerceIn(0f, 0.85f)
    fun setChatWallpaperDim(ctx: Context, v: Float) =
        p(ctx).edit().putFloat(KEY_CHAT_WALLPAPER_DIM, v.coerceIn(0f, 0.85f)).apply()

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
        put("ttsEnabled", ttsEnabled(ctx))
        put("ttsRate", ttsRate(ctx))
        put("searchEnabled", searchEnabled(ctx))
        put("mcpEnabled", mcpEnabled(ctx))
        put("accent", accent(ctx))
        put("chatFontFamily", chatFontFamily(ctx))
        put("bubbleWidth", bubbleWidth(ctx))
        put("showAvatars", showAvatars(ctx))
        put("compactChat", compactChat(ctx))
        put("hapticFeedback", hapticFeedback(ctx))
        put("codeFontSize", codeFontSize(ctx))
        put("confirmDelete", confirmDelete(ctx))
        put("responseStyle", responseStyle(ctx))
        put("responseLanguage", responseLanguage(ctx))
        put("codeLanguage", codeLanguage(ctx))
        put("chatWallpaper", chatWallpaper(ctx))
        put("chatWallpaperDim", chatWallpaperDim(ctx))
        put("theme", theme(ctx))
        put("chatMode", chatMode(ctx))
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