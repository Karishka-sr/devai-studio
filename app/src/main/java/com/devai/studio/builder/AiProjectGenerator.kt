package com.devai.studio.builder

import android.content.Context
import com.devai.studio.ai.AiClientFactory
import com.devai.studio.ai.AiMessage
import com.devai.studio.util.AiSupport
import org.json.JSONObject

object AiProjectGenerator {

    data class Result(val files: Map<String, String>, val usedAi: Boolean)

    suspend fun generate(ctx: Context, kind: String, desc: String): Result {
        if (desc.isBlank()) return Result(templates(kind, desc), false)
        val cfg = try {
            AiSupport.config(ctx)
        } catch (e: Exception) {
            return Result(templates(kind, desc), false)
        }
        val raw = try {
            val client = AiClientFactory.create(cfg)
            client.chat(
                listOf(AiMessage("system", SYSTEM), AiMessage("user", prompt(kind, desc)))
            ) {}
        } catch (e: Exception) {
            return Result(templates(kind, desc), false)
        }
        val files = parse(raw)
        return if (files.isEmpty()) Result(templates(kind, desc), false) else Result(files, true)
    }

    fun templates(kind: String, desc: String): Map<String, String> = when (kind) {
        "web" -> ProjectGenerator.web("MyApp", null)
        "python" -> ProjectGenerator.python("script", null)
        "kotlin" -> ProjectGenerator.kotlinCli("cli", null)
        else -> ProjectGenerator.android("MyApp", "com.devai.app", null, desc)
    }

    private const val SYSTEM =
        "Ты — генератор проектов. Отвечай ТОЛЬКО валидным JSON без markdown и без пояснений."

    private fun prompt(kind: String, desc: String): String {
        val type = if (kind == "ai") "android" else kind
        return """
            Создай проект типа "$type" по описанию: $desc
            Верни JSON строго такого вида:
            {"files":[{"path":"relative/path.ext","content":"..."}]}
            Правила:
            - Только относительные пути, без ".." и без абсолютных путей.
            - Android: settings.gradle.kts, build.gradle.kts, app/build.gradle.kts, app/src/main/AndroidManifest.xml, app/src/main/java/com/devai/app/MainActivity.kt, app/src/main/res/values/strings.xml, app/src/main/res/values/themes.xml, .github/workflows/build_apk.yml.
            - Web: index.html, style.css, script.js.
            - Python: main.py, requirements.txt.
            - Kotlin CLI: build.gradle.kts, src/main/kotlin/Main.kt.
            - Код должен быть рабочим и компилируемым.
            - Не более 25 файлов.
        """.trimIndent()
    }

    private fun parse(raw: String): Map<String, String> {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return emptyMap()
        val json = try {
            JSONObject(raw.substring(start, end + 1))
        } catch (e: Exception) {
            return emptyMap()
        }
        val arr = json.optJSONArray("files") ?: return emptyMap()
        val out = LinkedHashMap<String, String>()
        for (i in 0 until arr.length()) {
            if (out.size >= 30) break
            val o = arr.optJSONObject(i) ?: continue
            val path = o.optString("path").replace('\\', '/').trimStart('/')
            val content = o.optString("content")
            if (path.isBlank() || path.contains("..") || path.length > 200 || content.isEmpty()) continue
            out[path] = content
        }
        return out
    }
}