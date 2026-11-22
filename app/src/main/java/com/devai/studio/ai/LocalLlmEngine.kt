package com.devai.studio.ai

import android.content.Context
import com.devai.studio.data.AppPrefs
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.InputData
import com.google.ai.edge.litertlm.ResponseCallback
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.Session
import com.google.ai.edge.litertlm.SessionConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

object LocalLlmEngine {

    private const val MIN_CONTEXT = 512
    private const val MAX_CONTEXT = 1280
    private const val TIMEOUT_MS = 15 * 60 * 1000L

    private val mutex = Mutex()
    private var engine: Engine? = null
    private var session: Session? = null
    private var loadedKey: String? = null

    @Synchronized
    private fun ensure(ctx: Context, path: String): Session {
        val contextTokens = AppPrefs.localContextTokens(ctx).coerceIn(MIN_CONTEXT, MAX_CONTEXT)
        val topK = AppPrefs.localTopK(ctx).coerceIn(1, 100)
        val topP = AppPrefs.topP(ctx).toDouble().coerceIn(0.05, 1.0)
        val temperature = AppPrefs.temperature(ctx).toDouble().coerceIn(0.0, 1.0)
        val key = "$path|$contextTokens|$topK|$topP|$temperature"

        val current = session
        if (current != null && loadedKey == key) return current
        closeLocked()
        // Text-only .task models have no vision/audio encoder. Enabling these
        // backends makes LiteRT-LM look for TF_LITE_VISION_ENCODER and fail.
        val config = EngineConfig(
            modelPath = path,
            backend = Backend.CPU,
            maxNumTokens = contextTokens,
            cacheDir = ctx.cacheDir.absolutePath
        )
        val created = Engine(config)
        created.initialize()
        engine = created
        session = created.createSession(
            SessionConfig(
                SamplerConfig(
                    topK = topK,
                    topP = topP,
                    temperature = temperature,
                    seed = 0
                )
            )
        )
        loadedKey = key
        return session!!
    }

    @Synchronized
    fun close() = closeLocked()

    private fun closeLocked() {
        runCatching { session?.close() }
        runCatching { engine?.close() }
        session = null
        engine = null
        loadedKey = null
    }

    suspend fun generate(
        ctx: Context,
        path: String,
        prompt: String,
        onDelta: (String) -> Unit
    ): String = mutex.withLock {
        withContext(Dispatchers.IO) {
            val s = ensure(ctx.applicationContext, path)
            val sb = StringBuilder()
            val done = CompletableDeferred<String>()
            val callback = object : ResponseCallback {
                override fun onNext(chunk: String) {
                    if (chunk.isEmpty()) return
                    synchronized(sb) { sb.append(chunk) }
                    onDelta(chunk)
                }

                override fun onDone() {
                    done.complete(sb.toString())
                }

                override fun onError(error: Throwable) {
                    done.completeExceptionally(error)
                }
            }
            try {
                s.generateContentStream(listOf(InputData.Text(prompt)), callback)
                withTimeout(TIMEOUT_MS) { done.await() }
            } catch (e: Exception) {
                runCatching { s.cancelProcess() }
                throw e
            }
        }
    }
}
