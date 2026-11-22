package com.devai.studio.util

import android.app.Activity
import com.devai.studio.R
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.PremiumStore
import com.google.android.material.color.DynamicColors

data class AccentOption(
    val id: String,
    val label: String,
    val light: Long,
    val dark: Long,
)

object ThemeManager {
    const val M3 = "m3"
    const val EXPRESSIVE = "expressive"
    const val RETRO = "retro"

    val all = listOf(M3, EXPRESSIVE, RETRO)

    val accents = listOf(
        AccentOption("default", "По умолчанию", 0, 0),
        AccentOption("violet", "Фиолетовый", 0xFF7C4DFF, 0xFFD0BCFF),
        AccentOption("blue", "Синий", 0xFF2563EB, 0xFF93C5FD),
        AccentOption("cyan", "Голубой", 0xFF0891B2, 0xFF67E8F9),
        AccentOption("teal", "Бирюзовый", 0xFF0D9488, 0xFF5EEAD4),
        AccentOption("green", "Зелёный", 0xFF059669, 0xFF6EE7B7),
        AccentOption("orange", "Оранжевый", 0xFFEA580C, 0xFFFDBA74),
        AccentOption("amber", "Янтарный", 0xFFD97706, 0xFFFCD34D),
        AccentOption("pink", "Розовый", 0xFFDB2777, 0xFFF9A8D4),
        AccentOption("red", "Красный", 0xFFDC2626, 0xFFFCA5A5),
    )

    fun apply(activity: Activity) {
        activity.setTheme(styleRes(current(activity)))
    }

    fun applyDynamicIfAvailable(activity: Activity) {
        if (current(activity) == M3 && AppPrefs.dynamicColor(activity)) {
            runCatching { DynamicColors.applyToActivityIfAvailable(activity) }
        }
    }

    private fun current(activity: Activity): String {
        val id = AppPrefs.theme(activity)
        return if (isPremium(id) && !PremiumStore.isPremium(activity)) M3 else id
    }

    fun styleRes(id: String): Int = when (id) {
        EXPRESSIVE -> R.style.Theme_DevAIStudio_Expressive
        RETRO -> R.style.Theme_DevAIStudio_Retro
        else -> R.style.Theme_DevAIStudio
    }

    fun isPremium(id: String): Boolean = id != M3

    fun labelRes(id: String): Int = when (id) {
        EXPRESSIVE -> R.string.theme_expressive
        RETRO -> R.string.theme_retro
        else -> R.string.theme_m3
    }
}
