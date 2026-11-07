package com.spywer.yoho.data.remote

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import androidx.core.content.edit

/**
 * Cookie-сессия YoHo: память + SharedPreferences (commit) + WebView CookieManager.
 * Хранит все session/better-auth cookie, чтобы переживать kill процесса и cold start.
 */
class SessionCookieJar private constructor(
    context: Context,
) : CookieJar {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val memory = ConcurrentHashMap<String, Cookie>()

    init {
        restorePersisted()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val now = System.currentTimeMillis()
        var sessionChanged = false
        cookies.forEach { cookie ->
            val key = keyOf(cookie)
            if (cookie.expiresAt <= now) {
                memory.remove(key)
                if (isSessionCookieName(cookie.name)) sessionChanged = true
                syncWebView(url, cookie)
                return@forEach
            }
            val toStore = if (isSessionCookieName(cookie.name)) {
                // Session-cookie без Max-Age иначе слетает — фиксируем ~30 дней.
                copyCookie(cookie, expiresAt = now + DEFAULT_MAX_AGE_MS)
            } else {
                cookie
            }
            memory[keyOf(toStore)] = toStore
            if (isSessionCookieName(toStore.name)) sessionChanged = true
            syncWebView(url, toStore)
        }
        if (sessionChanged) persistAll()
        flushWebView()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        val expiredKeys = memory.entries
            .filter { it.value.expiresAt <= now }
            .map { it.key }
        var sessionExpired = false
        expiredKeys.forEach { key ->
            val name = key.substringBefore('|')
            memory.remove(key)
            if (isSessionCookieName(name)) sessionExpired = true
        }
        if (sessionExpired) persistAll()

        if (memory.none { it.value.matches(url) && isSessionCookieName(it.value.name) }) {
            restorePersisted()
            importFromWebView(url)
        }
        return memory.values.filter { it.matches(url) && it.expiresAt > now }
    }

    fun clear() {
        memory.clear()
        clearPersisted()
        runCatching {
            val cm = CookieManager.getInstance()
            cm.removeAllCookies(null)
            cm.flush()
        }
    }

    /** Есть ли сохранённая session-cookie (на диске или в памяти). */
    fun hasSessionCookie(): Boolean {
        if (memory.values.any { isSessionCookieName(it.name) && it.expiresAt > System.currentTimeMillis() }) {
            return true
        }
        restorePersisted()
        return memory.values.any { isSessionCookieName(it.name) }
    }

    /**
     * Перенос session/better-auth cookie на новое зеркало (host-only)
     * при failover — запросы уже идут на [newHost].
     */
    fun migrateSessionTo(newHost: String) {
        val now = System.currentTimeMillis()
        val sessions = memory.values
            .filter { isSessionCookieName(it.name) && it.expiresAt > now }
            .distinctBy { it.name to it.value }
            .toList()
        if (sessions.isEmpty()) return

        // Убрать копии со старых зеркал, оставить только newHost.
        sessions.forEach { old ->
            memory.keys
                .filter { it.startsWith("${old.name}|") }
                .forEach { memory.remove(it) }
        }
        sessions.forEach { old ->
            val migrated = buildCookie(
                name = old.name,
                value = old.value,
                domain = newHost,
                path = old.path.ifBlank { "/" },
                expiresAt = old.expiresAt,
                secure = true,
                hostOnly = true,
                httpOnly = old.httpOnly,
            )
            memory[keyOf(migrated)] = migrated
            "https://$newHost/".toHttpUrlOrNull()?.let { syncWebView(it, migrated) }
        }
        persistAll()
        flushWebView()
    }

    @SuppressLint("UseKtx")
    private fun persistAll() {
        val now = System.currentTimeMillis()
        val arr = JSONArray()
        memory.values
            .filter { isSessionCookieName(it.name) && it.expiresAt > now }
            .forEach { cookie ->
                arr.put(
                    JSONObject()
                        .put("name", cookie.name)
                        .put("value", cookie.value)
                        .put("domain", cookie.domain)
                        .put("path", cookie.path)
                        .put("expiresAt", cookie.expiresAt)
                        .put("secure", cookie.secure)
                        .put("hostOnly", cookie.hostOnly)
                        .put("httpOnly", cookie.httpOnly),
                )
            }
        // commit() — синхронно на диск (apply может не успеть до kill/переустановки студией).
        prefs.edit(commit = true) {
            putString(KEY_COOKIES_JSON, arr.toString())
        }
    }

    @SuppressLint("UseKtx")
    private fun clearPersisted() {
        prefs.edit(commit = true) { clear() }
    }

    private fun restorePersisted() {
        val raw = prefs.getString(KEY_COOKIES_JSON, null)
        if (!raw.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(raw)
                val now = System.currentTimeMillis()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val expires = o.optLong("expiresAt", 0L)
                    if (expires <= now) continue
                    val name = o.getString("name")
                    val value = o.getString("value")
                    val domain = o.optString("domain").ifBlank { apiHost() ?: return@runCatching }
                    val path = o.optString("path", "/")
                    val secure = o.optBoolean("secure", true)
                    val hostOnly = o.optBoolean("hostOnly", true)
                    val httpOnly = o.optBoolean("httpOnly", true)
                    val cookie = buildCookie(
                        name = name,
                        value = value,
                        domain = domain,
                        path = path,
                        expiresAt = expires,
                        secure = secure,
                        hostOnly = hostOnly,
                        httpOnly = httpOnly,
                    )
                    memory[keyOf(cookie)] = cookie
                    apiBaseUrl()?.let { syncWebView(it, cookie) }
                }
            }
            flushWebView()
            return
        }
        // Legacy single-cookie format
        restoreLegacySingle()
    }

    private fun restoreLegacySingle() {
        val value = prefs.getString(KEY_VALUE, null) ?: return
        if (value.isBlank()) return
        val name = prefs.getString(KEY_NAME, SESSION_COOKIE) ?: SESSION_COOKIE
        val domain = prefs.getString(KEY_DOMAIN, null) ?: apiHost() ?: return
        val path = prefs.getString(KEY_PATH, "/") ?: "/"
        val expires = prefs.getLong(KEY_EXPIRES, System.currentTimeMillis() + DEFAULT_MAX_AGE_MS)
        if (expires <= System.currentTimeMillis()) {
            clearPersisted()
            return
        }
        val secure = prefs.getBoolean(KEY_SECURE, true)
        val hostOnly = prefs.getBoolean(KEY_HOST_ONLY, true)
        val cookie = buildCookie(
            name = name,
            value = value,
            domain = domain,
            path = path,
            expiresAt = expires,
            secure = secure,
            hostOnly = hostOnly,
            httpOnly = true,
        )
        memory[keyOf(cookie)] = cookie
        apiBaseUrl()?.let { syncWebView(it, cookie) }
        flushWebView()
        persistAll()
    }

    private fun importFromWebView(url: HttpUrl) {
        val raw = runCatching { CookieManager.getInstance().getCookie(url.toString()) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: return
        var changed = false
        raw.split(";")
            .map { it.trim() }
            .filter { it.contains('=') }
            .forEach { part ->
                val name = part.substringBefore('=').trim()
                val value = part.substringAfter('=', missingDelimiterValue = "").trim()
                if (!isSessionCookieName(name) || value.isEmpty()) return@forEach
                if (memory.values.any { it.name == name && it.value == value }) return@forEach
                val cookie = buildCookie(
                    name = name,
                    value = value,
                    domain = url.host,
                    path = "/",
                    expiresAt = System.currentTimeMillis() + DEFAULT_MAX_AGE_MS,
                    secure = url.isHttps,
                    hostOnly = true,
                    httpOnly = true,
                )
                memory[keyOf(cookie)] = cookie
                changed = true
            }
        if (changed) persistAll()
    }

    private fun syncWebView(url: HttpUrl, cookie: Cookie) {
        runCatching {
            val cm = CookieManager.getInstance()
            cm.setAcceptCookie(true)
            val maxAge = ((cookie.expiresAt - System.currentTimeMillis()) / 1000L).coerceAtLeast(0)
            val parts = buildList {
                add("${cookie.name}=${cookie.value}")
                add("Path=${cookie.path}")
                if (!cookie.hostOnly) add("Domain=${cookie.domain}")
                if (cookie.secure) add("Secure")
                if (maxAge > 0) add("Max-Age=$maxAge")
            }
            cm.setCookie(url.scheme + "://" + url.host + "/", parts.joinToString("; "))
        }
    }

    private fun flushWebView() {
        runCatching { CookieManager.getInstance().flush() }
    }

    private fun keyOf(cookie: Cookie): String =
        "${cookie.name}|${cookie.domain}|${cookie.path}"

    private fun isSessionCookieName(name: String): Boolean {
        val n = name.lowercase()
        return n == SESSION_COOKIE ||
            n.contains("session") ||
            n.contains("better-auth")
    }

    private fun buildCookie(
        name: String,
        value: String,
        domain: String,
        path: String,
        expiresAt: Long,
        secure: Boolean,
        hostOnly: Boolean,
        httpOnly: Boolean,
    ): Cookie {
        val builder = Cookie.Builder()
            .name(name)
            .value(value)
            .path(path)
            .expiresAt(expiresAt)
        if (hostOnly) {
            builder.hostOnlyDomain(domain)
        } else {
            builder.domain(domain)
        }
        if (secure) builder.secure()
        if (httpOnly) builder.httpOnly()
        return builder.build()
    }

    private fun copyCookie(cookie: Cookie, expiresAt: Long): Cookie =
        buildCookie(
            name = cookie.name,
            value = cookie.value,
            domain = cookie.domain,
            path = cookie.path,
            expiresAt = expiresAt,
            secure = cookie.secure,
            hostOnly = cookie.hostOnly,
            httpOnly = cookie.httpOnly,
        )

    companion object {
        const val PREFS = "yoho_session_cookies"
        private const val SESSION_COOKIE = "yoho_session"
        private const val KEY_COOKIES_JSON = "cookies_json"
        // legacy
        private const val KEY_NAME = "name"
        private const val KEY_VALUE = "value"
        private const val KEY_DOMAIN = "domain"
        private const val KEY_PATH = "path"
        private const val KEY_EXPIRES = "expires"
        private const val KEY_SECURE = "secure"
        private const val KEY_HOST_ONLY = "host_only"
        private const val DEFAULT_MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000

        @Volatile
        private var instance: SessionCookieJar? = null

        fun get(): SessionCookieJar {
            instance?.let { return it }
            synchronized(this) {
                instance?.let { return it }
                val ctx = AppContext.require()
                return SessionCookieJar(ctx).also { instance = it }
            }
        }

        private fun apiBaseUrl(): HttpUrl? =
            SiteHttp.REFERER.toHttpUrlOrNull()

        private fun apiHost(): String? = apiBaseUrl()?.host
    }
}

@SuppressLint("StaticFieldLeak")
object AppContext {
    @Volatile
    private var app: Context? = null

    fun init(context: Context) {
        app = context.applicationContext
    }

    fun getOrNull(): Context? = app

    fun require(): Context =
        app ?: error("AppContext не инициализирован — проверь android:name=.YohoApp в манифесте")
}
