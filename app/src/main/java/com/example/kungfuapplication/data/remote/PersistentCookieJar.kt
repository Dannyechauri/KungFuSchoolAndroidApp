package com.example.kungfuapplication.data.remote

import android.content.Context
import androidx.core.content.edit
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Guarda la cookie wushu_session del backend para que la sesión sobreviva
 * a reinicios de la app mientras siga activa en el servidor.
 */
class PersistentCookieJar(context: Context) : CookieJar {
    private val prefs = context.getSharedPreferences("kungfu_cookies", Context.MODE_PRIVATE)
    private val cache = mutableMapOf<String, Cookie>()

    init {
        prefs.all.forEach { (key, value) ->
            val host = key.substringBefore('|')
            val raw = value as? String ?: return@forEach
            val url = HttpUrl.Builder().scheme("http").host(host).build()
            Cookie.parse(url, raw)?.let { cache[key] = it }
        }
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        prefs.edit {
            cookies.forEach { cookie ->
                val key = "${cookie.domain}|${cookie.path}|${cookie.name}"
                if (cookie.expiresAt < System.currentTimeMillis()) {
                    cache.remove(key)
                    remove(key)
                } else {
                    cache[key] = cookie
                    putString(key, cookie.toString())
                }
            }
        }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        val expired = cache.filterValues { it.expiresAt < now }.keys
        if (expired.isNotEmpty()) {
            expired.forEach { cache.remove(it) }
            prefs.edit { expired.forEach { remove(it) } }
        }
        return cache.values.filter { it.matches(url) }
    }

    @Synchronized
    fun clear() {
        cache.clear()
        prefs.edit { clear() }
    }
}
