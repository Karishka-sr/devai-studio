package com.devai.studio.data

import android.content.Context
import com.devai.studio.BuildConfig
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class AppUpdate(
    val version: String,
    val pageUrl: String,
    val apkUrl: String,
)

/**
 * Проверяет последний релиз Android-приложения на GitHub.
 * Результат кешируется, чтобы не тратить лимит GitHub API при каждом запуске.
 */
object UpdateChecker {
    private val REPOS = listOf(
        "Karishka-sr/devai-studio",
        "Karishka-sr/devai-studio-pc",
    )
    private const val PREFS = "devai_update"
    private const val KEY_LAST = "last_check"
    private const val KEY_VERSION = "version"
    private const val KEY_PAGE = "page"
    private const val KEY_APK = "apk"
    private const val KEY_LAST_GITHUB = "last_github_check"
    private const val KEY_DISMISSED = "dismissed_version"
    private const val INTERVAL_MS = 30 * 1000L
    private const val GITHUB_INTERVAL_MS = 5 * 60 * 1000L
    private const val SITE_JSON =
        "https://karishka-sr.github.io/devai-studio/data/latest.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun check(ctx: Context, force: Boolean = false): AppUpdate? =
        withContext(Dispatchers.IO) {
            val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()

            if (!force && now - prefs.getLong(KEY_LAST, 0L) < INTERVAL_MS) {
                return@withContext cached(ctx)
            }

            // The website JSON is cheap to poll and is refreshed by the release
            // workflow. GitHub API is queried less often (and on force/resume)
            // to stay well inside the unauthenticated rate limit.
            val website = runCatching { fetchWebsite() }.getOrNull()
            val githubDue = force || now - prefs.getLong(KEY_LAST_GITHUB, 0L) >= GITHUB_INTERVAL_MS
            val github = if (githubDue) {
                prefs.edit().putLong(KEY_LAST_GITHUB, now).apply()
                runCatching { fetchLatest() }.getOrNull()
            } else {
                null
            }

            var best = website
            if (github != null && (best == null || isNewer(github.version, best.version))) {
                best = github
            }

            prefs.edit().putLong(KEY_LAST, now).apply()
            if (best != null) {
                prefs.edit()
                    .putString(KEY_VERSION, best.version)
                    .putString(KEY_PAGE, best.pageUrl)
                    .putString(KEY_APK, best.apkUrl)
                    .apply()
            }
            best
        }

    fun dismissedVersion(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_DISMISSED, "")
            .orEmpty()

    fun dismiss(ctx: Context, version: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DISMISSED, version)
            .apply()
    }

    fun cached(ctx: Context): AppUpdate? {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val version = prefs.getString(KEY_VERSION, null) ?: return null
        if (!isNewer(version, BuildConfig.VERSION_NAME)) return null
        return AppUpdate(
            version = version,
            pageUrl = prefs.getString(KEY_PAGE, "").orEmpty(),
            apkUrl = prefs.getString(KEY_APK, "").orEmpty(),
        )
    }

    fun isNewer(remote: String, current: String): Boolean {
        val r = parseVersion(remote)
        val c = parseVersion(current)
        for (i in 0 until maxOf(r.size, c.size)) {
            val left = r.getOrElse(i) { 0 }
            val right = c.getOrElse(i) { 0 }
            if (left != right) return left > right
        }
        return false
    }

    private fun fetchWebsite(): AppUpdate? {
        val request = Request.Builder()
            .url(SITE_JSON + "?t=" + System.currentTimeMillis())
            .header("Accept", "application/json")
            .header("Cache-Control", "no-cache")
            .header("User-Agent", "DevAI-Studio")
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val json = JSONObject(resp.body?.string().orEmpty())
            val version = json.optString("version").trim()
                .ifBlank { json.optString("tag").trim().removePrefix("v") }
            if (version.isBlank() || !isNewer(version, BuildConfig.VERSION_NAME)) return null
            val apk = json.optString("apk_url").trim()
            val page = json.optString("page_url").trim()
            if (apk.isBlank() && page.isBlank()) return null
            return AppUpdate(
                version = version.removePrefix("v").removePrefix("V"),
                pageUrl = page,
                apkUrl = apk.ifBlank { page },
            )
        }
    }

    private fun fetchLatest(): AppUpdate? {
        var best: AppUpdate? = null
        for (repo in REPOS) {
            val update = fetchRepo(repo) ?: continue
            if (best == null || isNewer(update.version, best.version)) best = update
        }
        return best
    }

    private fun fetchRepo(repo: String): AppUpdate? {
        val fromApi = fetchRepoApi(repo)
        if (fromApi != null) return fromApi
        return fetchRepoAtom(repo)
    }

    private fun fetchRepoApi(repo: String): AppUpdate? {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repo/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "DevAI-Studio")
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val json = JSONObject(resp.body?.string().orEmpty())
            val tag = json.optString("tag_name").trim()
            if (tag.isBlank() || !isNewer(tag, BuildConfig.VERSION_NAME)) return null
            val apk = json.optJSONArray("assets")
                ?.let { assets ->
                    (0 until assets.length())
                        .mapNotNull { assets.optJSONObject(it) }
                        .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                        ?.optString("browser_download_url")
                }
                .orEmpty()
            if (apk.isBlank()) return null
            return AppUpdate(
                version = tag.removePrefix("v").removePrefix("V"),
                pageUrl = json.optString("html_url").trim(),
                apkUrl = apk,
            )
        }
    }

    private fun fetchRepoAtom(repo: String): AppUpdate? {
        val xml = runCatching { getText("https://github.com/$repo/releases.atom") }.getOrNull()
            ?: return null
        val entry = Regex("<entry>(.*?)</entry>", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.value ?: return null
        val page = Regex("<link rel=\"alternate\"[^>]*href=\"([^\"]+)\"")
            .find(entry)?.groupValues?.get(1) ?: return null
        val tag = page.substringAfterLast('/')
        if (tag.isBlank() || !isNewer(tag, BuildConfig.VERSION_NAME)) return null
        val assets = runCatching {
            getText("https://github.com/$repo/releases/expanded_assets/$tag")
        }.getOrNull() ?: return null
        val path = Regex("href=\"([^\"]+\\.apk)\"")
            .find(assets)?.groupValues?.get(1) ?: return null
        val apk = if (path.startsWith("http")) path else "https://github.com$path"
        return AppUpdate(
            version = tag.removePrefix("v").removePrefix("V"),
            pageUrl = page,
            apkUrl = apk,
        )
    }

    private fun getText(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "DevAI-Studio")
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            return resp.body?.string().orEmpty()
        }
    }

    private fun parseVersion(value: String): List<Int> =
        value.trim()
            .removePrefix("v")
            .removePrefix("V")
            .split('.', '-', '+', ' ')
            .mapNotNull { part -> part.takeWhile(Char::isDigit).toIntOrNull() }
}
