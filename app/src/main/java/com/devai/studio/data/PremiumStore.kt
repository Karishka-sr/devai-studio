package com.devai.studio.data

import android.content.Context

object PremiumStore {
    private const val PREFS = "devai_premium"
    private const val KEY_PREMIUM = "premium"
    private const val KEY_CODE = "code"

    private val CODES = setOf(
        "DEVAI-PRO",
        "DEVAI-2025",
        "DEVAI-PRO-2025",
        "PREMIUM",
        "DEVAI-2026",
        "DEVAI2026",
        "DEVAI26",
        "DEVAI-TEST",
        "DEVAI",
        "TEST",
        "PROMO",
    )

    fun isPremium(ctx: Context): Boolean = p(ctx).getBoolean(KEY_PREMIUM, false)

    fun activate(ctx: Context, raw: String): Boolean {
        val code = normalize(raw)
        if (code !in CODES) return false
        p(ctx).edit().putBoolean(KEY_PREMIUM, true).putString(KEY_CODE, code).apply()
        return true
    }

    private fun normalize(raw: String): String =
        raw.trim()
            .uppercase()
            .replace(Regex("[^A-Z0-9]+"), "-")
            .trim('-')

    fun deactivate(ctx: Context) {
        p(ctx).edit().remove(KEY_PREMIUM).remove(KEY_CODE).apply()
    }

    fun code(ctx: Context): String = p(ctx).getString(KEY_CODE, "").orEmpty()

    private fun p(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
