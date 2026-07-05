package com.volna.app.notifications

actual object PlatformPushPermissionGateway : PushPermissionGateway {
    actual override suspend fun currentStatus(): PushPermissionStatus =
        PushPermissionStatus.NotDetermined

    actual override suspend fun requestPermission(): PushPermissionStatus {
        // Web Notification API wiring is platform-specific; in-app prompt on BS-002
        // records the one-time choice until native permission adapters are added.
        return PushPermissionStatus.Granted
    }
}
