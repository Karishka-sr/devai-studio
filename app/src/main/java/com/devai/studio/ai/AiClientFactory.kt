package com.devai.studio.ai

object AiClientFactory {
    fun create(config: AiConfig): AiClient = when (config.format) {
        AiFormat.OPENAI_COMPAT -> OpenAiCompatClient(config)
        AiFormat.ANTHROPIC -> AnthropicClient(config)
        AiFormat.GEMINI -> GeminiClient(config)
    }
}