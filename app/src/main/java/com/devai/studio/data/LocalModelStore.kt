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
            promptFormat = "chatml",
            minRamGb = 2,
            builtIn = true
        ),
        LocalModel(
            id = "qwen2.5-0.5b",
            name = "Qwen2.5 0.5B",
            description = "Баланс скорости и качества (~521 МБ). Хорошо пишет код и короткие ответы.",
            url = "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/main/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            fileName = "qwen2.5-0.5b-q8.task",
            sizeBytes = 546_660_344L,
            license = "Apache-2.0",
            promptFormat = "chatml",
            minRamGb = 3,
            builtIn = true
        ),
        LocalModel(
            id = "tinyllama-1.1b",
            name = "TinyLlama 1.1B Chat",
            description = "Компактная чат-модель (~1.07 ГБ). Англоязычная, среднее качество.",
            url = "https://huggingface.co/litert-community/TinyLlama-1.1B-Chat-v1.0/resolve/main/TinyLlama-1.1B-Chat-v1.0_multi-prefill-seq_q8_ekv1280.task",
            fileName = "tinyllama-1.1b-q8.task",
            sizeBytes = 1_148_331_545L,
            license = "Apache-2.0",
            promptFormat = "zephyr",
            minRamGb = 4,
            builtIn = true,
            premium = true
        ),
        LocalModel(
            id = "qwen2.5-1.5b",
            name = "Qwen2.5 1.5B",
            description = "Лучшее качество в каталоге (~1.49 ГБ). Нужно больше оперативной памяти.",
            url = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            fileName = "qwen2.5-1.5b-q8.task",
            sizeBytes = 1_597_913_616L,
            license = "Apache-2.0",
            promptFormat = "chatml",
            minRamGb = 6,
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
