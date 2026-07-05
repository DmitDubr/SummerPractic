package com.vertical.app.core.error

enum class ApiErrorCode {
    ValidationError,
    NoSpots,
    OneBookingPerDay,
    SlotCancelled,
    RentalUnavailable,
    SlotRebookForbidden,
    AlreadyCancelled,
    CancelTooLate,
    BookingNotAttended,
    AlreadyRated,
    AlreadyInWaitlist,
    WaitlistNotFound,
    ServerError,
    Unknown,
}

sealed interface AppFailure {
    data object Unauthorized : AppFailure
    data object NetworkUnavailable : AppFailure
    data object Timeout : AppFailure
    data object Unknown : AppFailure
    data class Api(
        val code: ApiErrorCode,
        val message: String,
    ) : AppFailure
}

class AppFailureException(val failure: AppFailure) : RuntimeException(failure.toString())

fun Throwable.asAppFailure(): AppFailure = when (this) {
    is AppFailureException -> failure
    else -> AppFailure.Unknown
}

fun AppFailure.userMessage(): String = when (this) {
    is AppFailure.Api -> message
    AppFailure.NetworkUnavailable -> "Нет подключения к интернету"
    AppFailure.Timeout -> "Сервер не ответил вовремя"
    AppFailure.Unauthorized -> "Требуется авторизация"
    AppFailure.Unknown -> "Не удалось загрузить данные"
}
