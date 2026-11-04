package com.spywer.yoho.data.remote

import com.spywer.yoho.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .cookieJar(SessionCookieJar.get())
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                    .header("User-Agent", SiteHttp.USER_AGENT)
                    .header("Accept-Language", "ru,en;q=0.9")
                // Как fetch/iframe с reyoho.com
                if (original.header("Referer") == null) {
                    builder.header("Referer", SiteHttp.REFERER)
                }
                if (original.header("Origin") == null &&
                    (original.method == "POST" || original.method == "PUT" || original.method == "PATCH")
                ) {
                    builder.header("Origin", SiteHttp.ORIGIN)
                }
                chain.proceed(builder.build())
            }
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
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YohoApi::class.java)
    }
}
