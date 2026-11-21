package com.devai.studio.ai

import android.content.Context

object AiClientFactory {
    fun create(config: AiConfig, ctx: Context): AiClient = when (config.format) {
        AiFormat.OPENAI_COMPAT -> OpenAiCompatClient(config)
        AiFormat.ANTHROPIC -> AnthropicClient(config)
        AiFormat.GEMINI -> GeminiClient(config)
        AiFormat.LOCAL -> LocalLlmClient(ctx.applicationContext)
    }
}
