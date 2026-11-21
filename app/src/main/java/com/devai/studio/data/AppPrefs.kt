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

    const val DEFAULT_SYSTEM_PROMPT =
        "Ты — DevAI, полезный ИИ-ассистент разработчика. Отвечай по делу, давай рабочие примеры кода."

    private fun p(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

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