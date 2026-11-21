package com.devai.studio.util

import android.content.Context
import com.devai.studio.ai.AiConfig
import com.devai.studio.data.ApiKeyStore
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.ProviderCatalog
import com.devai.studio.data.ProviderInfo

object AiSupport {
    private val noKeyProviders = setOf("ollama", "lmstudio", "llama-cpp", "jan", "custom")

    fun provider(ctx: Context): ProviderInfo? = ProviderCatalog.find(AppPrefs.activeProvider(ctx))

    fun model(ctx: Context, p: ProviderInfo): String =
        AppPrefs.modelFor(ctx, p.id)?.takeIf { it.isNotBlank() } ?: p.models.firstOrNull().orEmpty()

    /** Собирает конфиг для активного провайдера или бросает IllegalStateException с понятным текстом. */
    fun config(ctx: Context): AiConfig {
        val p = provider(ctx)
            ?: throw IllegalStateException("Сначала выберите провайдера во вкладке «ИИ».")
        val key = ApiKeyStore.get(ctx, p.id)
        val isCustom = p.id == "custom"
        val base = (if (isCustom) AppPrefs.customBase(ctx) else p.baseUrl).trim()
        val model = (if (isCustom) AppPrefs.customModel(ctx) else model(ctx, p)).trim()

        if (base.isBlank()) throw IllegalStateException("Укажите base URL в Настройках.")
        if (model.isBlank()) throw IllegalStateException("Укажите модель в Настройках.")
        if (p.id !in noKeyProviders && key.isNullOrBlank()) {
            throw IllegalStateException("Вставьте API-ключ для «" + p.name + "» во вкладке «ИИ».")
        }

        return AiConfig(
            apiKey = key.orEmpty(),
            baseUrl = base,
            model = model,
            temperature = AppPrefs.temperature(ctx).toDouble(),
            maxTokens = AppPrefs.maxTokens(ctx),
            format = p.format
        )
    }
}