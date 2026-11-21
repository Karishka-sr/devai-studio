package com.devai.studio.ai

interface AiClient {
    /** Отправляет историю сообщений, потоково отдавая фрагменты ответа через [onDelta]. Возвращает полный текст. */
    suspend fun chat(messages: List<AiMessage>, onDelta: (String) -> Unit): String
}