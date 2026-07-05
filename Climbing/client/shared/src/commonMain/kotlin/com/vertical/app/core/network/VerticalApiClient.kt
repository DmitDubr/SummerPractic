package com.vertical.app.core.network

import com.vertical.app.core.error.AppFailure
import com.vertical.app.core.error.AppFailureException
import com.vertical.app.session.SessionRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class VerticalApiClient(
    private val sessionRepository: SessionRepository,
    private val baseUrl: String,
    private val httpClient: HttpClient = defaultHttpClient(),
) {
    internal suspend inline fun <reified T> send(
        path: String,
        authorized: Boolean = false,
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): Result<T> = execute(path, authorized, block) { response -> response.body() }

    suspend fun sendUnit(
        path: String,
        authorized: Boolean = false,
        block: HttpRequestBuilder.() -> Unit = {},
    ): Result<Unit> = execute(path, authorized, block) { }

    internal suspend fun <T> execute(
        path: String,
        authorized: Boolean,
        block: HttpRequestBuilder.() -> Unit,
        parse: suspend (HttpResponse) -> T,
    ): Result<T> = try {
        val response = httpClient.request(baseUrl.trimEnd('/') + path) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            if (authorized) {
                sessionRepository.token()?.let { bearerAuth(it) }
            }
            block()
        }
        when {
            response.status == HttpStatusCode.Unauthorized -> {
                sessionRepository.clearToken()
                Result.failure(AppFailureException(AppFailure.Unauthorized))
            }
            response.status.value in 200..299 -> Result.success(parse(response))
            else -> Result.failure(AppFailureException(response.readFailure()))
        }
    } catch (failure: Throwable) {
        Result.failure(failure.toAppFailureException())
    }

    private suspend fun HttpResponse.readFailure(): AppFailure {
        val text = bodyAsText()
        return runCatching {
            json.decodeFromString<ApiErrorDto>(text).toFailure()
        }.getOrNull() ?: AppFailure.Unknown
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun defaultHttpClient(): HttpClient = HttpClient {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
            install(Logging) {
                level = LogLevel.INFO
                sanitizeHeader { it == HttpHeaders.Authorization }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 20_000
                connectTimeoutMillis = 10_000
            }
        }
    }
}

private fun Throwable.toAppFailureException(): AppFailureException = when (this) {
    is AppFailureException -> this
    is TimeoutCancellationException -> AppFailureException(AppFailure.Timeout)
    is IOException -> AppFailureException(AppFailure.NetworkUnavailable)
    is SerializationException -> AppFailureException(AppFailure.Unknown)
    else -> AppFailureException(AppFailure.Unknown)
}
