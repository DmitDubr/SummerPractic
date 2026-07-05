package com.vertical.app.core.storage

import android.content.Context
import android.content.SharedPreferences

actual object PlatformSessionStorage : SessionStorage {
    private var preferences: SharedPreferences? = null
    private var fallbackToken: String? = null

    fun initialize(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        fallbackToken?.let { token ->
            preferences?.edit()?.putString(KEY, token)?.apply()
            fallbackToken = null
        }
    }

    actual override suspend fun readToken(): String? =
        preferences?.getString(KEY, null) ?: fallbackToken

    actual override suspend fun writeToken(token: String) {
        val prefs = preferences
        if (prefs == null) fallbackToken = token
        else prefs.edit().putString(KEY, token).apply()
    }

    actual override suspend fun clearToken() {
        fallbackToken = null
        preferences?.edit()?.remove(KEY)?.apply()
    }

    private const val PREFS = "vertical_session"
    private const val KEY = "session_token"
}
