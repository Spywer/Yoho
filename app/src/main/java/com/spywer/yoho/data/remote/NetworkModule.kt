package com.spywer.yoho.data.remote

import com.spywer.yoho.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object NetworkModule {

    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .cookieJar(SessionCookieJar.get())
            .addInterceptor(SiteHeadersInterceptor)
            .addInterceptor(SiteFailoverInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BASIC
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                },
            )
            .build()
    }

    val api: YohoApi by lazy {
        Retrofit.Builder()
            .baseUrl(SiteHttp.NOMINAL_BASE)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(YohoGson.instance))
            .build()
            .create(YohoApi::class.java)
    }
}

/** UA / Referer / Origin от активного зеркала. */
private object SiteHeadersInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val builder = original.newBuilder()
            .header("User-Agent", SiteHttp.USER_AGENT)
            .header("Accept-Language", "ru,en;q=0.9")

        if (original.header("Referer") == null) {
            builder.header("Referer", SiteHttp.REFERER)
        }

        if (
            original.header("Origin") == null &&
            (original.method == "POST" || original.method == "PUT" || original.method == "PATCH")
        ) {
            builder.header("Origin", SiteHttp.ORIGIN)
        }

        return chain.proceed(builder.build())
    }
}

/**
 * Rewrite host -> активное зеркало.
 *
 * ПЕРВЫЙ ВАРИАНТ:
 * - при 502/503/504 делаем failover;
 * - при обычном IOException НЕ делаем failover, чтобы нестабильный интернет
 *   не переключал зеркала и не переносил/перезаписывал сессию.
 */
private object SiteFailoverInterceptor : Interceptor {

    private val gatewayCodes = setOf(502, 503, 504)

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Фикс: не обрабатываем сторонние хосты как зеркала сайта.
        if (!SiteHttp.isCandidate(original.url.host)) {
            return chain.proceed(original)
        }

        var request = rewriteToActive(original)
        var lastError: IOException? = null
        val tried = linkedSetOf<String>()

        while (tried.size < SiteHttp.CANDIDATES.size) {
            val host = request.url.host
            tried.add(host.lowercase())

            val response = try {
                chain.proceed(request)
            } catch (e: IOException) {
                // Первый вариант: не переключаем зеркало при нестабильном интернете.
                throw e
            }

            if (response.code !in gatewayCodes) {
                return response
            }

            val code = response.code
            response.close()

            lastError = IOException("gateway $code on $host")

            val next = SiteHttp.failoverFrom(host)
            if (next == null || next.lowercase() in tried) {
                throw lastError
            }

            SessionCookieJar.get().migrateSessionTo(
                newHost = next,
                fromHost = host,
            )

            // POST/body one-shot нельзя безопасно повторить — хост уже сменён для следующих запросов.
            if (!canReplay(request)) {
                throw lastError
            }

            request = rewriteToActive(request, forceHost = next)
        }

        throw lastError ?: IOException("Все зеркала недоступны")
    }

    private fun canReplay(request: Request): Boolean {
        val body = request.body ?: return true
        return !body.isOneShot()
    }

    private fun rewriteToActive(
        request: Request,
        forceHost: String? = null,
    ): Request {
        val url = request.url

        if (!SiteHttp.isCandidate(url.host) && forceHost == null) {
            return request
        }

        val host = forceHost ?: SiteHttp.HOST

        if (url.host.equals(host, ignoreCase = true)) {
            return alignSiteHeaders(request, host)
        }

        val newUrl = url.newBuilder()
            .host(host)
            .scheme("https")
            .build()

        return alignSiteHeaders(request.newBuilder().url(newUrl).build(), host)
    }

    private fun alignSiteHeaders(request: Request, host: String): Request {
        val origin = "https://$host"
        val referer = "$origin/"

        val builder = request.newBuilder()

        val currentReferer = request.header("Referer")
        if (
            currentReferer != null &&
            SiteHttp.CANDIDATES.any { currentReferer.contains(it, ignoreCase = true) }
        ) {
            builder.header("Referer", referer)
        }

        val currentOrigin = request.header("Origin")
        if (
            currentOrigin != null &&
            SiteHttp.CANDIDATES.any { currentOrigin.contains(it, ignoreCase = true) }
        ) {
            builder.header("Origin", origin)
        }

        return builder.build()
    }
}