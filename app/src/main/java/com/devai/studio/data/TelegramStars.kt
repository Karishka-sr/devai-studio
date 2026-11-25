package com.devai.studio.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.devai.studio.BuildConfig

object TelegramStars {
    private val botUsername = BuildConfig.TELEGRAM_BOT_USERNAME.trim().removePrefix("@")
    val USERNAME = botUsername.ifBlank { "devAI_Studio_bot" }
    val URL = "https://t.me/$USERNAME"

    fun orderId(plan: PremiumPlan): String =
        "DVS-${plan.id.uppercase()}-${System.currentTimeMillis().toString(36).uppercase()}"

    fun orderText(plan: PremiumPlan, orderId: String): String = buildString {
        appendLine("👑 DevAI Studio Premium")
        appendLine("Тариф: ${plan.title}")
        appendLine("Срок: ${plan.duration}")
        appendLine("Цена: ${plan.stars} ⭐")
        appendLine("Заказ: $orderId")
        appendLine()
        appendLine("Оплата: Telegram Stars")
        append("После оплаты пришлите этот заказ — вам выдадут код активации.")
    }

    fun open(ctx: Context, plan: PremiumPlan, orderId: String): Boolean = runCatching {
        val target = if (botUsername.isNotBlank()) {
            "$URL?start=buy_${plan.id}"
        } else {
            "$URL?text=${Uri.encode(orderText(plan, orderId))}"
        }
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(target))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.isSuccess

    fun copy(ctx: Context, plan: PremiumPlan, orderId: String) {
        val clipboard = ctx.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("DevAI order", orderText(plan, orderId)))
    }
}
