package com.devai.studio.data

import com.devai.studio.ai.AiFormat

data class ProviderInfo(
    val id: String,
    val name: String,
    val category: Category,
    val description: String,
    val format: AiFormat,
    val baseUrl: String,
    val models: List<String>,
    val getKeyUrl: String,
    val website: String,
    val freeNote: String = ""
)