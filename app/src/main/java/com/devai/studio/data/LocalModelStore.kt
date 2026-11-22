package com.devai.studio.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class LocalModel(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val fileName: String,
    val sizeBytes: Long,
    val license: String,
    val promptFormat: String,
    val minRamGb: Int,
    val builtIn: Boolean = false,
    val premium: Boolean = false
)

object LocalModelStore {

    private const val PREFS = "devai_local_models"
    private const val KEY_CUSTOM = "custom"
    private const val KEY_ACTIVE = "active"

    val builtIn: List<LocalModel> = listOf(
        LocalModel(
            id = "smollm-135m",
            name = "SmolLM 135M",
            description = "Самая лёгкая модель (~159 МБ). Быстрые ответы для простых задач, работает даже на слабых телефонах.",
            url = "https://huggingface.co/litert-community/SmolLM-135M-Instruct/resolve/main/SmolLM-135M-Instruct_multi-prefill-seq_q8_ekv1280.task",
            fileName = "smollm-135m-q8.task",
            sizeBytes = 166_754_726L,
            license = "Apache-2.0",
            promptFormat = "auto",
            minRamGb = 2,
            builtIn = true
        ),
        LocalModel(
            id = "lfm2.5-350m",
            name = "LFM2.5 350M",
            description = "Очень быстрая модель (~248 МБ) от Liquid AI. Хороша для коротких ответов и черновиков.",
            url = "https://huggingface.co/litert-community/LFM2.5-350M/resolve/main/LFM2.5-350M_int4.litertlm",
            fileName = "lfm2.5-350m-int4.litertlm",
            sizeBytes = 247_808_000L,
            license = "LFM Open License v1.0",
            promptFormat = "auto",
            minRamGb = 2,
            builtIn = true
        ),
        LocalModel(
            id = "smollm2-360m",
            name = "SmolLM2 360M",
            description = "Обновлённый SmolLM (~374 МБ). Заметно лучше SmolLM 135M при том же классе скорости.",
            url = "https://huggingface.co/litert-community/SmolLM2-360M-Instruct/resolve/main/SmolLM2_360M_instruct.litertlm",
            fileName = "smollm2-360m-int8.litertlm",
            sizeBytes = 373_719_040L,
            license = "Apache-2.0",
            promptFormat = "auto",
            minRamGb = 2,
            builtIn = true
        ),
        LocalModel(
            id = "granite-4.0-h-350m",
            name = "Granite 4.0 H 350M",
            description = "Компактная модель IBM (~436 МБ) с хорошим следованием инструкциям.",
            url = "https://huggingface.co/litert-community/granite-4.0-h-350m/resolve/main/granite-4.0-h-350m_int8.litertlm",
            fileName = "granite-4.0-h-350m-int8.litertlm",
            sizeBytes = 436_060_160L,
            license = "Apache-2.0",
            promptFormat = "auto",
            minRamGb = 3,
            builtIn = true
        ),
        LocalModel(
            id = "qwen2.5-0.5b",
            name = "Qwen2.5 0.5B",
            description = "Баланс скорости и качества (~547 МБ). Хорошо пишет код и короткие ответы.",
            url = "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            fileName = "qwen2.5-0.5b-q8.task",
            sizeBytes = 546_660_344L,
            license = "Apache-2.0",
            promptFormat = "auto",
            minRamGb = 3,
            builtIn = true
        ),
        LocalModel(
            id = "qwen3-0.6b",
            name = "Qwen3 0.6B",
            description = "Свежий Qwen3 (~614 МБ) с режимом рассуждений. Заметно умнее Qwen2.5 0.5B.",
            url = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B.litertlm",
            fileName = "qwen3-0.6b.litertlm",
            sizeBytes = 613_722_264L,
            license = "Apache-2.0",
            promptFormat = "auto",
            minRamGb = 3,
            builtIn = true
        ),
        LocalModel(
            id = "lfm2.5-1.2b",
            name = "LFM2.5 1.2B Instruct",
            description = "Быстрая модель (~736 МБ) с хорошим русским и кодом. Удачный выбор для повседневного чата.",
            url = "https://huggingface.co/litert-community/LFM2.5-1.2B-Instruct/resolve/main/LFM2.5-1.2B-Instruct_int4.litertlm",
            fileName = "lfm2.5-1.2b-int4.litertlm",
            sizeBytes = 736_015_744L,
            license = "LFM Open License v1.0",
            promptFormat = "auto",
            minRamGb = 4,
            builtIn = true,
            premium = true
        ),
        LocalModel(
            id = "qwen2.5-coder-1.5b",
            name = "Qwen2.5 Coder 1.5B",
            description = "Специализированная модель для кода (~1.12 ГБ). Лучше обычных моделей на Kotlin, Java и Python.",
            url = "https://huggingface.co/litert-community/Qwen2.5-Coder-1.5B-Instruct/resolve/main/Qwen2.5-Coder-1.5B-Instruct_int4.litertlm",
            fileName = "qwen2.5-coder-1.5b-int4.litertlm",
            sizeBytes = 1_118_076_928L,
            license = "Apache-2.0",
            promptFormat = "auto",
            minRamGb = 6,
            builtIn = true,
            premium = true
        ),
        LocalModel(
            id = "tinyllama-1.1b",
            name = "TinyLlama 1.1B Chat",
            description = "Компактная англоязычная модель (~1.07 ГБ). Для русского лучше выбрать Qwen или LFM.",
            url = "https://huggingface.co/litert-community/TinyLlama-1.1B-Chat-v1.0/resolve/main/TinyLlama-1.1B-Chat-v1.0_multi-prefill-seq_q8_ekv1280.task",
            fileName = "tinyllama-1.1b-q8.task",
            sizeBytes = 1_148_331_545L,
            license = "Apache-2.0",
            promptFormat = "auto",
            minRamGb = 4,
            builtIn = true,
            premium = true
        ),
        LocalModel(
            id = "qwen2.5-1.5b",
            name = "Qwen2.5 1.5B",
            description = "Проверенная модель (~1.49 ГБ) с хорошим русским. Нужно больше оперативной памяти.",
            url = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            fileName = "qwen2.5-1.5b-q8.task",
            sizeBytes = 1_597_913_616L,
            license = "Apache-2.0",
            promptFormat = "auto",
            minRamGb = 6,
            builtIn = true,
            premium = true
        ),
        LocalModel(
            id = "qwen3-1.7b",
            name = "Qwen3 1.7B",
            description = "Сильная модель (~1.75 ГБ) для кода и рассуждений. Заметный шаг вперёд по качеству.",
            url = "https://huggingface.co/litert-community/Qwen3-1.7B/resolve/main/Qwen3_1.7B.litertlm",
            fileName = "qwen3-1.7b.litertlm",
            sizeBytes = 1_754_890_240L,
            license = "Apache-2.0",
            promptFormat = "auto",
            minRamGb = 6,
            builtIn = true,
            premium = true
        ),
        LocalModel(
            id = "deepseek-r1-1.5b",
            name = "DeepSeek R1 Distill 1.5B",
            description = "Рассуждающая модель для кода и сложных задач (~1.86 ГБ).",
            url = "https://huggingface.co/litert-community/DeepSeek-R1-Distill-Qwen-1.5B/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv1280.task",
            fileName = "deepseek-r1-1.5b-q8.task",
            sizeBytes = 1_861_094_737L,
            license = "MIT",
            promptFormat = "auto",
            minRamGb = 6,
            builtIn = true,
            premium = true
        ),
        LocalModel(
            id = "smollm3-3b",
            name = "SmolLM3 3B",
            description = "Крупная модель (~2.0 ГБ) с рассуждениями и хорошим кодом. Нужен телефон с 8 ГБ ОЗУ.",
            url = "https://huggingface.co/litert-community/SmolLM3-3B/resolve/main/SmolLM3-3B_q4_block32_ekv4096.litertlm",
            fileName = "smollm3-3b-q4.litertlm",
            sizeBytes = 2_004_664_320L,
            license = "Apache-2.0",
            promptFormat = "auto",
            minRamGb = 8,
            builtIn = true,
            premium = true
        ),
        LocalModel(
            id = "phi4-mini",
            name = "Phi-4 Mini Instruct",
            description = "Сильная компактная модель Microsoft (~3.94 ГБ). Нужен мощный телефон.",
            url = "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/main/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv1280.task",
            fileName = "phi4-mini-q8.task",
            sizeBytes = 3_944_275_882L,
            license = "MIT",
            promptFormat = "auto",
            minRamGb = 12,
            builtIn = true,
            premium = true
        )
    )
    fun modelsDir(ctx: Context): File {
        val dir = ctx.getExternalFilesDir("models") ?: File(ctx.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun file(ctx: Context, m: LocalModel): File = File(modelsDir(ctx), m.fileName)

    fun partFile(ctx: Context, m: LocalModel): File = File(modelsDir(ctx), m.fileName + ".part")

    fun catalog(ctx: Context): List<LocalModel> = builtIn + custom(ctx)

    fun custom(ctx: Context): List<LocalModel> {
        val raw = prefs(ctx).getString(KEY_CUSTOM, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                LocalModel(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    description = o.optString("description"),
                    url = o.optString("url"),
                    fileName = o.optString("fileName"),
                    sizeBytes = o.optLong("sizeBytes"),
                    license = o.optString("license", "См. источник"),
                    promptFormat = o.optString("format", "generic"),
                    minRamGb = o.optInt("minRamGb", 4),
                    builtIn = false,
                    premium = o.optBoolean("premium", false)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveCustom(ctx: Context, list: List<LocalModel>) {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(
                JSONObject()
                    .put("id", m.id)
                    .put("name", m.name)
                    .put("description", m.description)
                    .put("url", m.url)
                    .put("fileName", m.fileName)
                    .put("sizeBytes", m.sizeBytes)
                    .put("license", m.license)
                    .put("format", m.promptFormat)
                    .put("minRamGb", m.minRamGb)
                    .put("premium", m.premium)
            )
        }
        prefs(ctx).edit().putString(KEY_CUSTOM, arr.toString()).apply()
    }

    fun addCustom(
        ctx: Context,
        name: String,
        url: String,
        sizeBytes: Long,
        format: String,
        license: String = "См. источник"
    ): LocalModel {
        val id = "c" + System.currentTimeMillis().toString(36) + (100..999).random()
        val clean = name.replace(Regex("\\s+"), " ").trim().ifBlank { "Локальная модель" }.take(60)
        val m = LocalModel(
            id = id,
            name = clean,
            description = if (url.isBlank()) "Импортированный файл модели" else "Скачано по ссылке",
            url = url,
            fileName = "c_" + id + "_" + safeName(clean) + ".task",
            sizeBytes = sizeBytes,
            license = license,
            promptFormat = format,
            minRamGb = 4
        )
        saveCustom(ctx, custom(ctx) + m)
        return m
    }

    fun addImported(ctx: Context, name: String, fileName: String, sizeBytes: Long, format: String): LocalModel {
        val id = "c" + System.currentTimeMillis().toString(36) + (100..999).random()
        val m = LocalModel(
            id = id,
            name = name.replace(Regex("\\s+"), " ").trim().ifBlank { "Импортированная модель" }.take(60),
            description = "Импортированный файл модели",
            url = "",
            fileName = fileName,
            sizeBytes = sizeBytes,
            license = "См. источник",
            promptFormat = format,
            minRamGb = 4
        )
        saveCustom(ctx, custom(ctx) + m)
        return m
    }

    fun isInstalled(ctx: Context, m: LocalModel): Boolean {
        val f = file(ctx, m)
        if (!f.exists() || f.length() <= 0) return false
        return m.sizeBytes <= 0 || f.length() >= m.sizeBytes * 98 / 100
    }

    fun installed(ctx: Context): List<LocalModel> = catalog(ctx).filter { isInstalled(ctx, it) }

    fun delete(ctx: Context, m: LocalModel) {
        file(ctx, m).delete()
        partFile(ctx, m).delete()
        if (!m.builtIn) saveCustom(ctx, custom(ctx).filter { it.id != m.id })
        if (activeId(ctx) == m.id) setActive(ctx, "")
    }

    fun activeId(ctx: Context): String = prefs(ctx).getString(KEY_ACTIVE, "").orEmpty()

    fun setActive(ctx: Context, id: String) {
        prefs(ctx).edit().putString(KEY_ACTIVE, id).apply()
    }

    fun active(ctx: Context): LocalModel? {
        val id = activeId(ctx)
        if (id.isBlank()) return null
        return catalog(ctx).firstOrNull { it.id == id && isInstalled(ctx, it) }
    }

    fun activeName(ctx: Context): String = active(ctx)?.name.orEmpty()

    fun activePath(ctx: Context): String? = active(ctx)?.let { file(ctx, it).absolutePath }

    fun freeSpace(ctx: Context): Long = modelsDir(ctx).usableSpace

    private fun safeName(s: String): String =
        s.replace(Regex("[^A-Za-z0-9._-]"), "_").take(40).ifBlank { "model" }

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
