package com.devai.studio.ai

import android.content.Context
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

    private const val MAX_TOKENS = 1280
    private const val TIMEOUT_MS = 15 * 60 * 1000L

    private val mutex = Mutex()
    private var engine: Engine? = null
    private var session: Session? = null
    private var loadedPath: String? = null

    @Synchronized
    private fun ensure(ctx: Context, path: String): Session {
        val current = session
        if (current != null && loadedPath == path) return current
        closeLocked()
        // Text-only .task models have no vision/audio encoder. Enabling these
        // backends makes LiteRT-LM look for TF_LITE_VISION_ENCODER and fail.
        val config = EngineConfig(
            modelPath = path,
            backend = Backend.CPU,
            maxNumTokens = MAX_TOKENS,
            cacheDir = ctx.cacheDir.absolutePath
        )
        val created = Engine(config)
        created.initialize()
        engine = created
        session = created.createSession(
            SessionConfig(SamplerConfig(topK = 40, topP = 0.95, temperature = 0.7, seed = 0))
        )
        loadedPath = path
        return session!!
    }

    @Synchronized
    fun close() = closeLocked()

    private fun closeLocked() {
        runCatching { session?.close() }
        runCatching { engine?.close() }
        session = null
        engine = null
        loadedPath = null
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
