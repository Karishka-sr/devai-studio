package com.devai.studio.ai

data class AiMessage(
    val role: String,
    val content: String
)

enum class AiFormat {
    OPENAI_COMPAT,
    ANTHROPIC,
    GEMINI,
    LOCAL
}

data class AiConfig(
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val temperature: Double = 0.7,
    val topP: Double = 0.95,
    val maxTokens: Int = 4096,
    val timeoutSeconds: Int = 180,
    val format: AiFormat
)