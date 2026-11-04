package com.spywer.yoho.data.remote

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.io.FilterInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Прокси WebView без [X-Requested-With].
 * Для VideoSeed/kinoserial — только HTTP/1.1 (HTTP/2 = PROTOCOL_ERROR / reset).
 */
object PlayerWebViewHttp {
    private val cookieJar = WebViewCookieJar

    private val client: OkHttpClient by lazy {
        baseBuilder().build()
    }

    /** kinoserial ломает HTTP/2 у WebView/OkHttp/Cronet. */
    private val http1Client: OkHttpClient by lazy {
        baseBuilder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .build()
    }

    private fun baseBuilder(): OkHttpClient.Builder =
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .cookieJar(cookieJar)

    fun intercept(request: WebResourceRequest, forceHttp1: Boolean = false): WebResourceResponse? {
        if (!request.method.equals("GET", true) && !request.method.equals("HEAD", true)) {
            return null
        }
        val url = request.url?.toString() ?: return null
        val httpUrl = url.toHttpUrlOrNull() ?: return null
        if (httpUrl.scheme != "http" && httpUrl.scheme != "https") return null

        val videoSeedHost = isVideoSeedHost(httpUrl.host)
        val useHttp1 = forceHttp1 || videoSeedHost
        // Для VideoSeed медиа тоже через HTTP/1.1; иначе .m3u8/.ts снова PROTOCOL_ERROR
        if (!useHttp1 && isMediaPath(httpUrl.encodedPath)) return null

        return runCatching {
            val builder = Request.Builder().url(httpUrl).method(request.method, null)
            request.requestHeaders?.forEach { (key, value) ->
                if (key.equals("X-Requested-With", true)) return@forEach
                if (key.equals("Accept-Encoding", true)) return@forEach
                builder.header(key, value)
            }
            val browser = PlayerBrowserProfile.browserHeaders()
            builder.header("User-Agent", browser.getValue("User-Agent"))
            browser["Sec-CH-UA"]?.let { builder.header("Sec-CH-UA", it) }
            browser["Sec-CH-UA-Mobile"]?.let { builder.header("Sec-CH-UA-Mobile", it) }
            browser["Sec-CH-UA-Platform"]?.let { builder.header("Sec-CH-UA-Platform", it) }
            browser["Accept-Language"]?.let { builder.header("Accept-Language", it) }
            if (request.requestHeaders?.keys?.none { it.equals("Referer", true) } != false) {
                builder.header("Referer", SiteHttp.REFERER)
            }

            val http = if (useHttp1) http1Client else client
            val response = http.newCall(builder.build()).execute()
            val body = response.body
            if (body == null) {
                response.close()
                return@runCatching null
            }
            val contentType = response.header("Content-Type").orEmpty()
            val mime = contentType.substringBefore(';').trim()
                .ifBlank { guessMime(httpUrl.encodedPath) }
            val charset = contentType.substringAfter("charset=", "").substringBefore(';').trim()
                .takeIf { it.isNotBlank() && !isMediaPath(httpUrl.encodedPath) }

            val outHeaders = LinkedHashMap<String, String>()
            for (name in listOf(
                "Content-Type", "Cache-Control", "Accept-Ranges", "Content-Range",
                "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials",
            )) {
                response.header(name)?.let { outHeaders[name] = it }
            }

            // Закрываем OkHttp Response вместе с потоком WebView.
            val stream: InputStream = object : FilterInputStream(body.byteStream()) {
                override fun close() {
                    try {
                        super.close()
                    } finally {
                        response.close()
                    }
                }
            }

            WebResourceResponse(
                mime,
                charset,
                response.code,
                response.message.ifBlank { "OK" },
                outHeaders,
                stream,
            )
        }.getOrNull()
    }

    fun isVideoSeedHost(host: String?): Boolean {
        val h = host?.lowercase() ?: return false
        return h.contains("kinoserial") ||
            h.contains("kinolot") ||
            h.contains("videoseed")
    }

    private fun isMediaPath(path: String): Boolean {
        val p = path.lowercase()
        return p.endsWith(".m3u8") ||
            p.endsWith(".ts") ||
            p.endsWith(".mp4") ||
            p.endsWith(".m4s") ||
            p.endsWith(".aac") ||
            p.endsWith(".mp3") ||
            p.contains("/video/") && (p.endsWith(".bin") || p.contains("segment"))
    }

    private fun guessMime(path: String): String = when {
        path.endsWith(".m3u8", true) -> "application/vnd.apple.mpegurl"
        path.endsWith(".ts", true) -> "video/mp2t"
        path.endsWith(".mp4", true) -> "video/mp4"
        path.endsWith(".js", true) -> "application/javascript"
        path.endsWith(".css", true) -> "text/css"
        path.endsWith(".json", true) -> "application/json"
        path.endsWith(".png", true) -> "image/png"
        path.endsWith(".jpg", true) || path.endsWith(".jpeg", true) -> "image/jpeg"
        path.endsWith(".webp", true) -> "image/webp"
        path.endsWith(".svg", true) -> "image/svg+xml"
        path.endsWith(".woff2", true) -> "font/woff2"
        else -> "text/html"
    }

    private object WebViewCookieJar : CookieJar {
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val raw = CookieManager.getInstance().getCookie(url.toString()) ?: return emptyList()
            return raw.split(';').mapNotNull { part ->
                Cookie.parse(url, part.trim())
            }
        }

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val cm = CookieManager.getInstance()
            cookies.forEach { cm.setCookie(url.toString(), it.toString()) }
            cm.flush()
        }
    }
}
