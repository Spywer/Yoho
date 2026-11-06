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
            .addConverterFactory(GsonConverterFactory.create())
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
        if (original.header("Origin") == null &&
            (original.method == "POST" || original.method == "PUT" || original.method == "PATCH")
        ) {
            builder.header("Origin", SiteHttp.ORIGIN)
        }
        return chain.proceed(builder.build())
    }
}

/**
 * Rewrite host → активное зеркало; при IO / 502–504 — cooldown, migrate cookies, retry.
 */
private object SiteFailoverInterceptor : Interceptor {
    private val gatewayCodes = setOf(502, 503, 504)

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = rewriteToActive(chain.request())
        var lastIo: IOException? = null
        val tried = linkedSetOf<String>()

        while (tried.size < SiteHttp.CANDIDATES.size) {
            val host = request.url.host
            tried.add(host.lowercase())

            try {
                val response = chain.proceed(request)
                if (response.code !in gatewayCodes) {
                    return response
                }
                val code = response.code
                response.close()
                lastIo = IOException("gateway $code on $host")
            } catch (e: IOException) {
                lastIo = e
            }

            val next = SiteHttp.failoverFrom(host)
            if (next == null || next.lowercase() in tried) {
                throw lastIo ?: IOException("Зеркало $host недоступно")
            }
            SessionCookieJar.get().migrateSessionTo(next)
            request = rewriteToActive(request, forceHost = next)
        }
        throw lastIo ?: IOException("Все зеркала недоступны")
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
        val newUrl = url.newBuilder().host(host).scheme("https").build()
        return alignSiteHeaders(request.newBuilder().url(newUrl).build(), host)
    }

    private fun alignSiteHeaders(request: Request, host: String): Request {
        val origin = "https://$host"
        val referer = "$origin/"
        val builder = request.newBuilder()
        val currentReferer = request.header("Referer")
        if (currentReferer != null &&
            SiteHttp.CANDIDATES.any { currentReferer.contains(it, ignoreCase = true) }
        ) {
            builder.header("Referer", referer)
        }
        val currentOrigin = request.header("Origin")
        if (currentOrigin != null &&
            SiteHttp.CANDIDATES.any { currentOrigin.contains(it, ignoreCase = true) }
        ) {
            builder.header("Origin", origin)
        }
        return builder.build()
    }
}
