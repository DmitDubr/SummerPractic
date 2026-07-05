package com.volna.app.notifications

actual object PlatformPushPermissionGateway : PushPermissionGateway {
    actual override suspend fun currentStatus(): PushPermissionStatus =
        PushPermissionStatus.NotDetermined

    actual override suspend fun requestPermission(): PushPermissionStatus =
        PushPermissionStatus.Denied
}
