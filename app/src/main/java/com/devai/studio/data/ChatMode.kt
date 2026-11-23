package com.devai.studio.data

enum class ChatMode(
    val id: String,
    val title: String,
    val subtitle: String,
) {
    CHAT(
        id = "chat",
        title = "Обычный чат",
        subtitle = "Свободный диалог: вопросы, тексты, идеи и объяснения.",
    ),
    DEV(
        id = "dev",
        title = "Разработка",
        subtitle = "Код, архитектура, ошибки и 50 языков программирования.",
    );

    companion object {
        fun fromId(id: String?): ChatMode = entries.firstOrNull { it.id == id } ?: CHAT
    }
}
