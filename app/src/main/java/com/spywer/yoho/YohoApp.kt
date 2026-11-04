package com.spywer.yoho

import android.app.Application
import android.webkit.CookieManager
import com.spywer.yoho.data.remote.AppContext
import com.spywer.yoho.data.remote.SessionCookieJar

class YohoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContext.init(this)
        runCatching {
            CookieManager.getInstance().setAcceptCookie(true)
        }
        // Прогрев jar — подтянуть yoho_session с диска до первых запросов.
        SessionCookieJar.get()
    }
}
