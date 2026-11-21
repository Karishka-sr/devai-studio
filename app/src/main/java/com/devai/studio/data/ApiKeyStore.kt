package com.devai.studio.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Хранилище API-ключей. Использует шифрование через Android Keystore
 * (EncryptedSharedPreferences). При недоступности — откат на обычные prefs.
 */
object ApiKeyStore {
    private const val PREFS = "devai_keys"
    private const val FALLBACK = "devai_keys_fallback"

    @Volatile
    private var prefs: SharedPreferences? = null

    private fun prefs(ctx: Context): SharedPreferences {
        prefs?.let { return it }
        synchronized(this) {
            prefs?.let { return it }
            val created = try {
                val masterKey = MasterKey.Builder(ctx)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    ctx,
                    PREFS,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (_: Exception) {
                ctx.getSharedPreferences(FALLBACK, Context.MODE_PRIVATE)
            }
            prefs = created
            return created
        }
    }

    fun save(ctx: Context, providerId: String, key: String) {
        prefs(ctx).edit().putString(providerId, key).apply()
    }

    fun get(ctx: Context, providerId: String): String? = prefs(ctx).getString(providerId, null)

    fun has(ctx: Context, providerId: String): Boolean = !get(ctx, providerId).isNullOrBlank()

    fun clear(ctx: Context, providerId: String) {
        prefs(ctx).edit().remove(providerId).apply()
    }

    fun clearAll(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }
}