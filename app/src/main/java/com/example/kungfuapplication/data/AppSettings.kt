package com.example.kungfuapplication.data

import android.content.Context
import androidx.core.content.edit

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("kungfu_settings", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, null) ?: DEFAULT_BASE_URL
        set(value) = prefs.edit { putString(KEY_BASE_URL, normalizeBaseUrl(value)) }

    var lastEmail: String
        get() = prefs.getString(KEY_LAST_EMAIL, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_LAST_EMAIL, value) }

    companion object {
        // 10.0.2.2 es el "localhost" del ordenador visto desde el emulador de Android.
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8080"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_LAST_EMAIL = "last_email"

        fun normalizeBaseUrl(raw: String): String {
            var url = raw.trim().trimEnd('/')
            if (url.isEmpty()) return DEFAULT_BASE_URL
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://$url"
            }
            return url
        }
    }
}
