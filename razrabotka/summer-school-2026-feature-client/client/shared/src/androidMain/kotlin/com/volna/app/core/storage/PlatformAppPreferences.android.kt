package com.volna.app.core.storage

import android.content.Context

actual object PlatformAppPreferences : AppPreferences {
    private var preferences: android.content.SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    actual override suspend fun pushPermissionRequested(): Boolean =
        preferences?.getBoolean(KEY_PUSH_PERMISSION_REQUESTED, false) ?: false

    actual override suspend fun setPushPermissionRequested(requested: Boolean) {
        preferences?.edit()?.putBoolean(KEY_PUSH_PERMISSION_REQUESTED, requested)?.apply()
    }

    private const val PREFERENCES_NAME = "volna_app_prefs"
    private const val KEY_PUSH_PERMISSION_REQUESTED = "push_permission_requested"
}
