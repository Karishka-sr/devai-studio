package com.devai.studio.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object AndroidUtils {
    fun toast(ctx: Context, msg: String) {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
    }

    fun openUrl(ctx: Context, url: String) {
        if (url.isBlank()) return
        try {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            toast(ctx, "Не удалось открыть ссылку")
        }
    }

    fun share(ctx: Context, uri: Uri, mime: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            ctx.startActivity(
                Intent.createChooser(intent, "Открыть / Поделиться")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            toast(ctx, "Нет приложения для открытия")
        }
    }
}