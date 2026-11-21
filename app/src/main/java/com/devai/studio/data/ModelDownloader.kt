package com.devai.studio.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

object ModelDownloader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun download(
        ctx: Context,
        model: LocalModel,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (model.url.isBlank()) throw IOException("У модели нет ссылки для скачивания")

        val dir = LocalModelStore.modelsDir(ctx)
        val dest = LocalModelStore.file(ctx, model)
        val part = LocalModelStore.partFile(ctx, model)

        if (dest.exists() && LocalModelStore.isInstalled(ctx, model)) return@withContext dest

        var existing = if (part.exists()) part.length() else 0L
        val need = if (model.sizeBytes > 0) (model.sizeBytes - existing).coerceAtLeast(0) else 0L
        if (need > 0 && dir.usableSpace < need + 64L * 1024 * 1024) {
            throw IOException("Недостаточно места: нужно ещё " + mb(need + 64L * 1024 * 1024))
        }

        val request = Request.Builder()
            .url(model.url)
            .apply { if (existing > 0) header("Range", "bytes=$existing-") }
            .build()

        client.newCall(request).execute().use { resp ->
            if (resp.code == 416 && existing > 0) {
                part.delete()
                existing = 0
                throw IOException("Докачка невозможна, начните заново")
            }
            if (!resp.isSuccessful) throw IOException("HTTP " + resp.code)
            val body = resp.body ?: throw IOException("Пустой ответ сервера")
            val append = resp.code == 206 && existing > 0
            if (!append) existing = 0

            val length = body.contentLength()
            val total = if (length > 0) existing + length else model.sizeBytes
            var done = existing

            FileOutputStream(part, append).use { out ->
                body.byteStream().use { input ->
                    val buf = ByteArray(128 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        onProgress(done, total)
                    }
                }
                out.flush()
            }
        }

        if (model.sizeBytes > 0 && part.length() < model.sizeBytes * 98 / 100) {
            throw IOException("Файл скачан не полностью")
        }
        if (dest.exists()) dest.delete()
        if (!part.renameTo(dest)) throw IOException("Не удалось сохранить модель")
        onProgress(dest.length(), dest.length())
        dest
    }

    fun mb(bytes: Long): String = String.format("%.0f МБ", bytes / 1024.0 / 1024.0)
}
