package com.devai.studio.data

import android.app.Activity
import android.content.Context
import com.devai.studio.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

/**
 * Real Firebase Phone Auth. Requires FIREBASE_API_KEY, FIREBASE_APP_ID and
 * FIREBASE_PROJECT_ID in local.properties; without them the UI stays disabled.
 */
object PhoneAuthManager {
    data class Session(val phone: String, val uid: String)

    private const val APP_NAME = "devai-phone-auth"
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun isConfigured(): Boolean =
        BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
            BuildConfig.FIREBASE_APP_ID.isNotBlank() &&
            BuildConfig.FIREBASE_PROJECT_ID.isNotBlank()

    private fun auth(context: Context): FirebaseAuth {
        val existing = FirebaseApp.getApps(context).firstOrNull { it.name == APP_NAME }
        val app = existing ?: FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .build(),
            APP_NAME,
        ) ?: error("Firebase не инициализировался")
        return FirebaseAuth.getInstance(app)
    }

    fun start(
        activity: Activity,
        rawPhone: String,
        onCodeSent: (String) -> Unit,
        onError: (String) -> Unit,
        onSignedIn: (Session) -> Unit = {},
    ) {
        if (!isConfigured()) {
            onError("SMS-регистрация не настроена. Добавьте Firebase-ключи в local.properties.")
            return
        }
        val phone = PhoneNumbers.normalize(rawPhone)
        if (phone == null) {
            onError("Введите номер в формате +7 999 123-45-67")
            return
        }
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signIn(activity, credential, onError, onSignedIn)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                onError(humanError(e))
            }

            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = id
                resendToken = token
                onCodeSent(PhoneNumbers.pretty(phone))
            }
        }
        val builder = PhoneAuthOptions.newBuilder(auth(activity))
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
        resendToken?.let { builder.setForceResendingToken(it) }
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    fun confirm(
        activity: Activity,
        code: String,
        onSuccess: (Session) -> Unit,
        onError: (String) -> Unit,
    ) {
        val id = verificationId
        if (id.isNullOrBlank()) {
            onError("Сначала запросите код по SMS")
            return
        }
        if (code.filter { it.isDigit() }.length != 6) {
            onError("Код состоит из 6 цифр")
            return
        }
        signIn(activity, PhoneAuthProvider.getCredential(id, code.trim()), onError, onSuccess)
    }

    private fun signIn(
        context: Context,
        credential: PhoneAuthCredential,
        onError: (String) -> Unit,
        onSuccess: (Session) -> Unit = {},
    ) {
        auth(context).signInWithCredential(credential).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                onError(humanError(task.exception))
                return@addOnCompleteListener
            }
            val user = task.result?.user
            val session = Session(user?.phoneNumber.orEmpty(), user?.uid.orEmpty())
            if (session.phone.isBlank() || session.uid.isBlank()) {
                onError("Не удалось получить номер из Firebase")
                return@addOnCompleteListener
            }
            ProfileStore.saveSession(context, session.phone, session.uid)
            onSuccess(session)
        }
    }

    fun signOut(context: Context) {
        runCatching { auth(context).signOut() }
        verificationId = null
        resendToken = null
        ProfileStore.signOut(context)
    }

    private fun humanError(error: Throwable?): String {
        val code = (error as? FirebaseAuthException)?.errorCode.orEmpty()
        return when (code) {
            "invalid-phone-number" -> "Неверный номер телефона"
            "too-many-requests" -> "Слишком много попыток. Подождите немного."
            "quota-exceeded" -> "Лимит SMS исчерпан. Попробуйте позже."
            "invalid-verification-code" -> "Неверный код из SMS"
            "session-expired" -> "Код устарел. Запросите новый."
            "missing-activity" -> "Не удалось открыть проверку. Перезапустите приложение."
            "app-not-authorized" -> "Firebase не разрешает этот пакет или подпись"
            "billing-not-enabled" -> "В Firebase не включена оплата SMS"
            else -> error?.message ?: "Ошибка проверки номера"
        }
    }
}
