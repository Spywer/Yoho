package com.spywer.yoho.data.remote

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebSettings
import com.google.gson.Gson
import com.spywer.yoho.BuildConfig
import okhttp3.Request

/** UA / Referer как у мобильного Chrome (не desktop — иначе wsdk/капча = «не человек»). */
object SiteHttp {
    const val ORIGIN = "https://reyoho.com"
    const val REFERER = "https://reyoho.com/"

    /**
     * Дефолтный WebView UA без маркера `; wv` / Version/4.0 —
     * ближе к обычному Chrome на Android.
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun userAgent(context: Context? = null): String {
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
            }
        }
        return FALLBACK_UA
    }

    /** Для OkHttp до готовности Context. */
    val USER_AGENT: String get() = userAgent()

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
    private val gson = Gson()

    fun parse(html: String): PlayerConfigDto? {
        val marker = "\"initialPlayerConfig\":"
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
            gson.fromJson(unescaped, PlayerConfigDto::class.java)
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
    val base = BuildConfig.API_BASE_URL.trimEnd('/') + "/"
    val url = base + "$kind/$kinopoiskId"
    val req = Request.Builder()
        .url(url)
        .header("User-Agent", SiteHttp.USER_AGENT)
        .header("Referer", SiteHttp.REFERER)
        .header("Accept", SiteHttp.documentHeaders()["Accept"]!!)
        .header("Accept-Language", "ru,en;q=0.9")
        .get()
        .build()
    return runCatching {
        NetworkModule.httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@use null
            resp.body.string()
        }
    }.getOrNull()
}

/** Быстрая проверка, что JSON объект конфига не пустышка. */
fun PlayerConfigDto.hasPlayableSource(): Boolean =
    toPlayerSources().any { it.hasPlayableContent() }
