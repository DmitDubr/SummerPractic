package com.vertical.app.core.storage

import platform.Foundation.NSUserDefaults

actual object PlatformSessionStorage : SessionStorage {
    private val defaults get() = NSUserDefaults.standardUserDefaults

    actual override suspend fun readToken(): String? = defaults.stringForKey(KEY)

    actual override suspend fun writeToken(token: String) {
        defaults.setObject(token, forKey = KEY)
    }

    actual override suspend fun clearToken() {
        defaults.removeObjectForKey(KEY)
    }

    private const val KEY = "vertical_session_token"
}
