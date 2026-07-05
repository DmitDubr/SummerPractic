package com.vertical.app.core.storage

expect object PlatformSessionStorage : SessionStorage {
    override suspend fun readToken(): String?
    override suspend fun writeToken(token: String)
    override suspend fun clearToken()
}

interface SessionStorage {
    suspend fun readToken(): String?
    suspend fun writeToken(token: String)
    suspend fun clearToken()
}
