package com.devai.studio.ai

data class AiMessage(
    val role: String,
    val content: String
)

enum class AiFormat {
    OPENAI_COMPAT,
    ANTHROPIC,
    GEMINI
}

data class AiConfig(
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val temperature: Double = 0.7,
    val maxTokens: Int = 4096,
    val format: AiFormat
)