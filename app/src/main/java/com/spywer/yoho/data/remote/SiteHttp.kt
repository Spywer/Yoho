package com.spywer.yoho.data.remote

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebSettings
import androidx.core.content.edit
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * UA / Referer как у мобильного Chrome (не desktop — иначе wsdk/капча = «не человек»).
 */
object SiteHttp {

    /** Зеркала сайта — единственный список хостов. */
    val CANDIDATES: List<String> = listOf(
        "reyoho.com",
        "reyoho.ru",
        "yoho.pw",
    )

    private const val PREFS = "site_endpoints"
    private const val KEY_ACTIVE = "active_host"
    private const val KEY_EXCLUDED = "excluded_until"
    private const val COOLDOWN_MS = 10L * 60 * 1000

    private val lock = Any()
    private val excludedUntil = ConcurrentHashMap<String, Long>()

    @Volatile
    private var loaded = false

    @Volatile
    private var activeHostField: String = CANDIDATES.first()

    /** Активное зеркало (после failover может смениться). */
    val HOST: String
        get() {
            ensureLoaded()
            return activeHostField
        }

    val ORIGIN: String
        get() = "https://$HOST"

    val REFERER: String
        get() = "$ORIGIN/"

    /** Номинальный baseUrl для Retrofit (rewrite подставит активный хост). */
    val NOMINAL_BASE: String
        get() = "https://${CANDIDATES.first()}/"

    fun isCandidate(host: String): Boolean =
        CANDIDATES.any { it.equals(host, ignoreCase = true) }

    /**
     * Пометить [failedHost] недоступным на [COOLDOWN_MS], выбрать следующее зеркало.
     * @return новый активный хост или `null`, если переключать некуда / тот же.
     */
    fun failoverFrom(failedHost: String): String? {
        // Фикс: не обрабатываем сторонние хосты как зеркала сайта.
        if (!isCandidate(failedHost)) return null

        synchronized(lock) {
            ensureLoaded()

            val now = System.currentTimeMillis()
            pruneExcludedLocked(now)

            excludedUntil[failedHost.lowercase()] = now + COOLDOWN_MS

            val next = pickNextLocked(after = failedHost, now = now) ?: return null

            if (next.equals(activeHostField, ignoreCase = true)) {
                persistLocked()
                return null
            }

            activeHostField = next
            persistLocked()
            return next
        }
    }

    fun ensureLoaded() {
        if (loaded) return

        synchronized(lock) {
            if (loaded) return

            val ctx = AppContext.getOrNull() ?: return
            val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

            val saved = prefs.getString(KEY_ACTIVE, null)
            if (!saved.isNullOrBlank() && isCandidate(saved)) {
                activeHostField = CANDIDATES.first { it.equals(saved, ignoreCase = true) }
            }

            val rawExcluded = prefs.getString(KEY_EXCLUDED, null)
            if (!rawExcluded.isNullOrBlank()) {
                runCatching {
                    val o = JSONObject(rawExcluded)
                    val now = System.currentTimeMillis()

                    o.keys().forEach { key ->
                        val until = o.optLong(key, 0L)
                        if (until > now && isCandidate(key)) {
                            excludedUntil[key.lowercase()] = until
                        }
                    }
                }
            }

            pruneExcludedLocked(System.currentTimeMillis())

            val now = System.currentTimeMillis()
            if (isExcludedLocked(activeHostField, now)) {
                activeHostField = pickNextLocked(after = activeHostField, now = now)
                    ?: CANDIDATES.first()
                persistLocked()
            }

            loaded = true
        }
    }

    private fun isExcludedLocked(host: String, now: Long): Boolean =
        (excludedUntil[host.lowercase()] ?: 0L) > now

    private fun pruneExcludedLocked(now: Long) {
        excludedUntil.entries.removeIf { it.value <= now }
    }

    private fun pickNextLocked(after: String?, now: Long): String? {
        if (CANDIDATES.isEmpty()) return null

        val start = after
            ?.let { failed -> CANDIDATES.indexOfFirst { it.equals(failed, ignoreCase = true) } }
            ?.takeIf { it >= 0 }
            ?: -1

        for (offset in 1..CANDIDATES.size) {
            val host = CANDIDATES[(start + offset) % CANDIDATES.size]
            if (!isExcludedLocked(host, now)) return host
        }

        // Все в cooldown — снять самый старый exclude и взять его.
        val oldest = excludedUntil.minByOrNull { it.value }?.key
        if (oldest != null) {
            excludedUntil.remove(oldest)
            return CANDIDATES.firstOrNull { it.equals(oldest, ignoreCase = true) } ?: oldest
        }

        return CANDIDATES.first()
    }

    @SuppressLint("UseKtx")
    private fun persistLocked() {
        val ctx = AppContext.getOrNull() ?: return

        val excludedJson = JSONObject()
        excludedUntil.forEach { (host, until) ->
            excludedJson.put(host, until)
        }

        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit(commit = true) {
                putString(KEY_ACTIVE, activeHostField)
                putString(KEY_EXCLUDED, excludedJson.toString())
            }
    }

    @Volatile
    private var cachedUserAgent: String? = null

    /**
     * Дефолтный WebView UA без маркера `; wv` / Version/4.0 —
     * ближе к обычному Chrome на Android. Кэшируется (дорогой [WebSettings.getDefaultUserAgent]).
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun userAgent(context: Context? = null): String {
        cachedUserAgent?.let { return it }

        val ctx = context ?: AppContext.getOrNull()
        if (ctx != null) {
            val raw = runCatching { WebSettings.getDefaultUserAgent(ctx) }.getOrNull()
            if (!raw.isNullOrBlank()) {
                return raw
                    .replace("; wv", "")
                    .replace(";wv", "")
                    .replace("Version/4.0 ", "")
                    .replace(Regex("""\s+"""), " ")
                    .trim()
                    .also { cachedUserAgent = it }
            }
        }

        return FALLBACK_UA
    }

    /** Для OkHttp до готовности Context. */
    val USER_AGENT: String
        get() = userAgent()

    private const val FALLBACK_UA =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    /** Заголовки navigate как у Chrome; для плеера см. [PlayerBrowserProfile.browserHeaders]. */
    fun documentHeaders(context: Context? = null): Map<String, String> =
        PlayerBrowserProfile.browserHeaders(context)
}

/**
 * Достаёт `initialPlayerConfig` из HTML карточки (как SSR на сайте).
 * Без `yoho_session` сервер отдаёт `null` + takenDownReason=login.
 */
object PlayerConfigPageParser {

    fun parse(html: String): PlayerConfigDto? {
        val marker = "\"initialPlayerConfig\": "
        val start = html.indexOf(marker)
        if (start < 0) return null

        var i = start + marker.length
        while (i < html.length && html[i].isWhitespace()) i++

        if (html.startsWith("null", i)) return null
        if (i >= html.length || html[i] != '{') return null

        val json = extractObject(html, i) ?: return null

        val unescaped = json
            .replace("\\u0026", "&")
            .replace("\\/", "/")

        return runCatching {
            YohoGson.instance.fromJson(unescaped, PlayerConfigDto::class.java)
        }.getOrNull()
    }

    fun needsLogin(html: String): Boolean {
        if (html.contains("\"takenDownReason\":\"login\"")) {
            val cfg = parse(html)
            return cfg == null || cfg.toPlayerSources().none { !it.iframeUrl.isNullOrBlank() }
        }
        return false
    }

    private fun extractObject(src: String, openBrace: Int): String? {
        var depth = 0
        var inString = false
        var escape = false

        for (i in openBrace until src.length) {
            val c = src[i]

            if (inString) {
                when {
                    escape -> escape = false
                    c == '\\' -> escape = true
                    c == '"' -> inString = false
                }
                continue
            }

            when (c) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return src.substring(openBrace, i + 1)
                }
            }
        }

        return null
    }
}

fun fetchTitlePageHtml(
    kinopoiskId: Int,
    mediaType: String,
): String? {
    val kind = when {
        mediaType.equals("tv", true) || mediaType.equals("anime", true) -> "series"
        else -> "film"
    }

    // Заголовки UA/Referer/Accept-Language ставит SiteHeadersInterceptor.
    val url = SiteHttp.REFERER + "$kind/$kinopoiskId"

    val accept = SiteHttp.documentHeaders()["Accept"]
        ?: "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"

    val req = Request.Builder()
        .url(url)
        .header("Accept", accept)
        .get()
        .build()

    return runCatching {
        NetworkModule.httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@use null
            resp.body?.string()
        }
    }.getOrNull()
}

/** Быстрая проверка, что JSON объект конфига не пустышка. */
fun PlayerConfigDto.hasPlayableSource(): Boolean =
    toPlayerSources().any { it.hasPlayableContent() }