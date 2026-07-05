package com.vertical.app.core.config

data class AppConfig(
    val appVersion: String = "0.1.0",
)

expect fun defaultApiBaseUrl(): String
