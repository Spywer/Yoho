package com.spywer.yoho.data.remote

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.UserAgentMetadata
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/**
 * Делает player WebView ближе к обычному Chrome на Android:
 * UA без `; wv`, Client Hints Chrome (не Android WebView), лёгкая JS-маскировка.
 *
 * [apply] идемпотентен — повторный вызов не дублирует document-start скрипты
 * (иначе ломаются Vibix/rendex и др.).
 */
object PlayerBrowserProfile {
    private const val TAG_APPLIED = "yoho_browser_profile_v2"

    /** Масштаб текста WebView в fullscreen (100 = как в браузере). */
    const val PLAYER_TEXT_ZOOM = 145

    /** CSS zoom оболочки embed в fullscreen. */
    const val PLAYER_CSS_ZOOM = "1.35"

    /** Sec-CH / Accept для navigate и OkHttp-прокси. */
    fun browserHeaders(context: android.content.Context? = null): Map<String, String> {
        val ua = SiteHttp.userAgent(context)
        val major = chromeMajor(ua)
        return mapOf(
            "User-Agent" to ua,
            "Referer" to SiteHttp.REFERER,
            "Accept" to
                "text/html,application/xhtml+xml,application/xml;q=0.9," +
                "image/avif,image/webp,image/apng,*/*;q=0.8," +
                "application/signed-exchange;v=b3;q=0.7",
            "Accept-Language" to "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7",
            "Upgrade-Insecure-Requests" to "1",
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "none",
            "Sec-Fetch-User" to "?1",
            "Sec-CH-UA" to secChUa(major),
            "Sec-CH-UA-Mobile" to "?1",
            "Sec-CH-UA-Platform" to "\"Android\"",
            "X-Requested-With" to "",
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun apply(webView: WebView) {
        if (webView.getTag(TAG_KEY) == TAG_APPLIED) {
            // Только освежить UA (на случай смены контекста)
            webView.settings.userAgentString = SiteHttp.userAgent(webView.context)
            return
        }
        val ctx = webView.context
        val settings = webView.settings
        val ua = SiteHttp.userAgent(ctx)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.loadsImagesAutomatically = true
        settings.blockNetworkImage = false
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.setGeolocationEnabled(true)
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        // textZoom поднимаем только в fullscreen (см. applyPlayerContentZoom).
        settings.textZoom = 100
        settings.userAgentString = ua

        if (WebViewFeature.isFeatureSupported(WebViewFeature.OFF_SCREEN_PRERASTER)) {
            WebSettingsCompat.setOffscreenPreRaster(settings, true)
        }
        applyChromeUserAgentMetadata(settings, ua)
        installBrowserCamouflage(webView)
        webView.setTag(TAG_KEY, TAG_APPLIED)
    }

    private fun applyChromeUserAgentMetadata(settings: WebSettings, ua: String) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) return
        val major = chromeMajor(ua)
        val full = "$major.0.0.0"
        val brands = listOf(
            brand("Not A(Brand", "99", "99.0.0.0"),
            brand("Chromium", major, full),
            brand("Google Chrome", major, full),
        )
        val meta = UserAgentMetadata.Builder()
            .setBrandVersionList(brands)
            .setFullVersion(full)
            .setPlatform("Android")
            .setPlatformVersion(Build.VERSION.RELEASE ?: "14")
            .setArchitecture("arm")
            .setModel(Build.MODEL?.takeIf { it.isNotBlank() } ?: "Pixel 7")
            .setMobile(true)
            .setBitness(64)
            .build()
        WebSettingsCompat.setUserAgentMetadata(settings, meta)
    }

    private fun brand(name: String, major: String, full: String): UserAgentMetadata.BrandVersion =
        UserAgentMetadata.BrandVersion.Builder()
            .setBrand(name)
            .setMajorVersion(major)
            .setFullVersion(full)
            .build()

    private fun chromeMajor(ua: String): String =
        Regex("""Chrome/(\d+)""").find(ua)?.groupValues?.getOrNull(1) ?: "131"

    private fun secChUa(major: String): String =
        "\"Not A(Brand\";v=\"99\", \"Chromium\";v=\"$major\", \"Google Chrome\";v=\"$major\""

    private fun installBrowserCamouflage(webView: WebView) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return
        WebViewCompat.addDocumentStartJavaScript(
            webView,
            BROWSER_CAMOUFLAGE_JS,
            setOf("*"),
        )
    }

    /**
     * Только безопасные правки: не трогаем plugins/maxTouchPoints —
     * фейковые plugins ломали Vibix/rendex.
     */
    private val BROWSER_CAMOUFLAGE_JS = """
(function(){
  try {
    Object.defineProperty(navigator, 'webdriver', { get: function(){ return undefined; }, configurable: true });
  } catch(e) {}
  try {
    if (!window.chrome) {
      window.chrome = { runtime: {} };
    } else if (!window.chrome.runtime) {
      window.chrome.runtime = {};
    }
  } catch(e) {}
  try {
    if (!navigator.languages || navigator.languages.length === 0) {
      Object.defineProperty(navigator, 'languages', {
        get: function(){ return ['ru-RU','ru','en-US','en']; },
        configurable: true
      });
    }
  } catch(e) {}
})();
""".trimIndent()

    /** Стабильный id для View.setTag(int) — не пересекается с Android framework. */
    private val TAG_KEY = 0x59F10B01
}
