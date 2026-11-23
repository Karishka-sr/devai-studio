package com.devai.studio.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Nickname and verified phone live in encrypted prefs. */
object ProfileStore {
    private const val PREFS = "devai_profile"
    private const val FALLBACK = "devai_profile_fallback"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_PHONE = "phone"
    private const val KEY_UID = "uid"

    @Volatile
    private var prefs: SharedPreferences? = null

    private fun p(ctx: Context): SharedPreferences {
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
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            } catch (_: Exception) {
                ctx.getSharedPreferences(FALLBACK, Context.MODE_PRIVATE)
            }
            prefs = created
            return created
        }
    }

    fun nickname(ctx: Context): String = p(ctx).getString(KEY_NICKNAME, "Miko") ?: "Miko"

    fun setNickname(ctx: Context, value: String) {
        p(ctx).edit().putString(KEY_NICKNAME, value.trim()).apply()
    }

    fun phone(ctx: Context): String = p(ctx).getString(KEY_PHONE, "") ?: ""

    fun uid(ctx: Context): String = p(ctx).getString(KEY_UID, "") ?: ""

    fun isVerified(ctx: Context): Boolean = phone(ctx).isNotBlank() && uid(ctx).isNotBlank()

    fun saveSession(ctx: Context, phone: String, uid: String) {
        p(ctx).edit().putString(KEY_PHONE, phone).putString(KEY_UID, uid).apply()
    }

    fun signOut(ctx: Context) {
        p(ctx).edit().remove(KEY_PHONE).remove(KEY_UID).apply()
    }
}
