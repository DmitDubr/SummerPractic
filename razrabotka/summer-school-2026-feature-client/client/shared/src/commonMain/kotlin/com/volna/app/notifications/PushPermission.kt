package com.volna.app.notifications

enum class PushPermissionStatus {
    NotDetermined,
    Granted,
    Denied,
}

interface PushPermissionGateway {
    suspend fun currentStatus(): PushPermissionStatus
    suspend fun requestPermission(): PushPermissionStatus
}

expect object PlatformPushPermissionGateway : PushPermissionGateway {
    override suspend fun currentStatus(): PushPermissionStatus
    override suspend fun requestPermission(): PushPermissionStatus
}

fun List<Int>.toReminderLeadText(): String = when {
    isEmpty() -> "заранее"
    size == 1 -> "за ${single()} ч до старта"
    else -> "за ${joinToString(" и ") { "$it" }} ч до старта"
}
