package com.devai.studio.util

import android.content.Context
import android.speech.tts.TextToSpeech
import com.devai.studio.data.AppPrefs
import java.util.Locale

/** Озвучивание ответов через системный TTS-движок Android. */
object TtsManager {
    private var engine: TextToSpeech? = null
    private var ready = false
    private var pendingText: String? = null

    fun speak(ctx: Context, text: String) {
        if (text.isBlank()) return
        val app = ctx.applicationContext
        if (engine == null) {
            engine = TextToSpeech(app) { status ->
                ready = status == TextToSpeech.SUCCESS
                if (ready) {
                    val result = engine?.setLanguage(Locale.getDefault())
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        engine?.language = Locale.US
                    }
                    pendingText?.let { queued ->
                        pendingText = null
                        speakNow(app, queued)
                    }
                }
            }
        }
        if (ready) speakNow(app, text) else pendingText = text
    }

    private fun speakNow(ctx: Context, text: String) {
        engine?.setSpeechRate(AppPrefs.ttsRate(ctx).coerceIn(0.5f, 2f))
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "devai-${System.currentTimeMillis()}")
    }

    fun stop() {
        engine?.stop()
    }

    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        ready = false
        pendingText = null
    }
}
