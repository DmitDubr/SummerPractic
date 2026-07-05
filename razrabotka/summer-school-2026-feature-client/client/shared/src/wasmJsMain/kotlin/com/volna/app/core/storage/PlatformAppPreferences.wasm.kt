package com.volna.app.core.storage

import kotlinx.browser.localStorage

actual object PlatformAppPreferences : AppPreferences {
    actual override suspend fun pushPermissionRequested(): Boolean =
        localStorage.getItem(KEY_PUSH_PERMISSION_REQUESTED) == "true"

    actual override suspend fun setPushPermissionRequested(requested: Boolean) {
        if (requested) {
            localStorage.setItem(KEY_PUSH_PERMISSION_REQUESTED, "true")
        } else {
            localStorage.removeItem(KEY_PUSH_PERMISSION_REQUESTED)
        }
    }

    private const val KEY_PUSH_PERMISSION_REQUESTED = "volna_push_permission_requested"
}
