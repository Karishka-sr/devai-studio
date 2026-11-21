package com.devai.studio.builder

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipSaver {

    fun buildZip(files: Map<String, String>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            files.forEach { (path, content) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return bos.toByteArray()
    }

    fun save(ctx: Context, fileName: String, bytes: ByteArray): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToDownloads(ctx, fileName, bytes) ?: saveToAppDir(ctx, fileName, bytes)
            } else {
                saveToAppDir(ctx, fileName, bytes)
            }
        } catch (_: Exception) {
            saveToAppDir(ctx, fileName, bytes)
        }
    }

    private fun saveToDownloads(ctx: Context, fileName: String, bytes: ByteArray): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val resolver = ctx.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { it.write(bytes) }
        return uri
    }

    private fun saveToAppDir(ctx: Context, fileName: String, bytes: ByteArray): Uri? {
        val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: ctx.filesDir
        val file = File(dir, fileName)
        file.writeBytes(bytes)
        return FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
    }
}