package com.devai.studio.ai

import android.content.Context
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.LocalModelStore

class LocalLlmClient(private val ctx: Context) : AiClient {

    override suspend fun chat(messages: List<AiMessage>, onDelta: (String) -> Unit): String {
        val lastUser = messages.lastOrNull { it.role == "user" }?.content.orEmpty()
        ResponseGuard.greetingReply(lastUser)?.let { reply ->
            onDelta(reply)
            return reply
        }

        val model = LocalModelStore.active(ctx)
            ?: throw IllegalStateException("Локальная модель не выбрана. Откройте «Локальные модели».")
        val file = LocalModelStore.file(ctx, model)
        if (!file.exists()) {
            throw IllegalStateException("Файл модели «" + model.name + "» не найден. Скачайте её заново.")
        }
        val maxChars = (AppPrefs.localContextTokens(ctx) * 2.3).toInt()
            .coerceIn(2000, 6000)
        val prompt = LocalPrompt.build(messages, model.promptFormat, maxChars)
        val gate = ResponseGate(onDelta)
        val raw = LocalLlmEngine.generate(ctx, file.absolutePath, prompt) { gate.onChunk(it) }
        gate.finish()
        if (gate.blocked || raw.isBlank() || ResponseGuard.isMeta(raw) || ResponseGuard.isDegenerate(raw)) {
            val reply = ResponseGuard.fallback(lastUser)
            onDelta(reply)
            return reply
        }
        return raw
    }
}
