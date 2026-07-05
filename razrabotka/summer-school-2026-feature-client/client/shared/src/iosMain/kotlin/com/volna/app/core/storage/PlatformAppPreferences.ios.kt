package com.volna.app.core.storage

import platform.Foundation.NSUserDefaults

actual object PlatformAppPreferences : AppPreferences {
    private val defaults: NSUserDefaults
        get() = NSUserDefaults.standardUserDefaults

    actual override suspend fun pushPermissionRequested(): Boolean =
        defaults.boolForKey(KEY_PUSH_PERMISSION_REQUESTED)

    actual override suspend fun setPushPermissionRequested(requested: Boolean) {
        defaults.setBool(requested, forKey = KEY_PUSH_PERMISSION_REQUESTED)
    }

    private const val KEY_PUSH_PERMISSION_REQUESTED = "volna_push_permission_requested"
}
