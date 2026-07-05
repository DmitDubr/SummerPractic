package com.vertical.app.session

import com.vertical.app.core.storage.SessionStorage

interface SessionRepository {
    suspend fun token(): String?
    suspend fun saveToken(token: String)
    suspend fun clearToken()
}

class DefaultSessionRepository(
    private val storage: SessionStorage,
) : SessionRepository {
    override suspend fun token(): String? = storage.readToken()
    override suspend fun saveToken(token: String) = storage.writeToken(token)
    override suspend fun clearToken() = storage.clearToken()
}
