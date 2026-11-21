package com.devai.studio.ai

import android.content.Context
import com.devai.studio.data.LocalModelStore

class LocalLlmClient(private val ctx: Context) : AiClient {

    override suspend fun chat(messages: List<AiMessage>, onDelta: (String) -> Unit): String {
        val model = LocalModelStore.active(ctx)
            ?: throw IllegalStateException("Локальная модель не выбрана. Откройте «Локальные модели».")
        val file = LocalModelStore.file(ctx, model)
        if (!file.exists()) {
            throw IllegalStateException("Файл модели «" + model.name + "» не найден. Скачайте её заново.")
        }
        val prompt = LocalPrompt.build(messages, model.promptFormat)
        return LocalLlmEngine.generate(ctx, file.absolutePath, prompt, onDelta)
    }
}
