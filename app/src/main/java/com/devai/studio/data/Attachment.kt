package com.devai.studio.data

/** Текстовый файл, прикреплённый к сообщению. Содержимое не сохраняется на диск. */
data class Attachment(
    val name: String,
    val content: String,
)
