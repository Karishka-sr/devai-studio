package com.devai.studio.util

import android.content.Context
import com.devai.studio.ai.AiConfig
import com.devai.studio.ai.AiFormat
import com.devai.studio.data.ApiKeyStore
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.LocalModelStore
import com.devai.studio.data.ProviderCatalog
import com.devai.studio.data.ProviderInfo

object AiSupport {
    private val noKeyProviders = setOf("ollama", "lmstudio", "llama-cpp", "jan", "custom", "local")

    fun provider(ctx: Context): ProviderInfo? = ProviderCatalog.find(AppPrefs.activeProvider(ctx))

    fun model(ctx: Context, p: ProviderInfo): String {
        val saved = AppPrefs.modelFor(ctx, p.id)?.takeIf { it.isNotBlank() }
        if (saved != null && (p.models.isEmpty() || saved in p.models)) return saved
        return p.models.firstOrNull().orEmpty()
    }

    /** Собирает конфиг для активного провайдера или бросает IllegalStateException с понятным текстом. */
    fun config(ctx: Context): AiConfig {
        val p = provider(ctx)
            ?: throw IllegalStateException("Сначала выберите провайдера во вкладке «ИИ».")

        if (p.format == AiFormat.LOCAL) {
            val model = LocalModelStore.active(ctx)
                ?: throw IllegalStateException(
                    "Локальная модель не выбрана. Откройте «ИИ» → «Локальные модели»."
                )
            return AiConfig(
                apiKey = "",
                baseUrl = LocalModelStore.file(ctx, model).absolutePath,
                model = model.name,
                temperature = AppPrefs.temperature(ctx).toDouble(),
                topP = AppPrefs.topP(ctx).toDouble(),
                maxTokens = AppPrefs.maxTokens(ctx),
                timeoutSeconds = AppPrefs.timeoutSeconds(ctx),
                format = AiFormat.LOCAL
            )
        }

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
            topP = AppPrefs.topP(ctx).toDouble(),
            maxTokens = AppPrefs.maxTokens(ctx),
            timeoutSeconds = AppPrefs.timeoutSeconds(ctx),
            format = p.format
        )
    }
}