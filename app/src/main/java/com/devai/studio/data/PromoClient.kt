package com.devai.studio.data

import android.content.Context
import android.provider.Settings
import com.devai.studio.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class PromoActivation(
    val plan: PremiumPlan? = null,
    val code: String = "",
    val error: String? = null,
)

object PromoClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun activate(context: Context, rawCode: String): PromoActivation = withContext(Dispatchers.IO) {
        val code = rawCode.trim().uppercase()
        if (code.isBlank()) return@withContext PromoActivation(error = "Введите код.")
        val url = BuildConfig.PROMO_API_URL.trim()
        if (url.isBlank()) return@withContext PromoActivation(error = "Сервер кодов не настроен.")

        val payload = JSONObject()
            .put("code", code)
            .put("device_id", deviceId(context))
            .toString()
            .toRequestBody(jsonType)
        val request = Request.Builder().url(url).post(payload).build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(body) }.getOrNull()
                if (!response.isSuccessful || json?.optBoolean("ok") != true) {
                    val message = json?.optJSONObject("error")?.optString("message")
                        ?: if (response.code == 404) "Код не найден." else "Не удалось активировать код."
                    return@use PromoActivation(error = message)
                }
                val plan = PremiumPlan.entries.firstOrNull { it.id == json?.optString("plan").orEmpty() }
                if (plan == null) {
                    PromoActivation(error = "Сервер вернул неизвестный тариф.")
                } else {
                    PromoActivation(plan = plan, code = json?.optString("code", code) ?: code)
                }
            }
        }.getOrElse {
            PromoActivation(error = "Нет связи с сервером кодов. Попробуй позже.")
        }
    }

    private fun deviceId(context: Context): String {
        val raw = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        val digest = MessageDigest.getInstance("SHA-256").digest(("devai|$raw").toByteArray())
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
