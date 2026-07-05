package com.volna.app.core.storage

interface AppPreferences {
    suspend fun pushPermissionRequested(): Boolean
    suspend fun setPushPermissionRequested(requested: Boolean)
}

expect object PlatformAppPreferences : AppPreferences {
    override suspend fun pushPermissionRequested(): Boolean
    override suspend fun setPushPermissionRequested(requested: Boolean)
}
