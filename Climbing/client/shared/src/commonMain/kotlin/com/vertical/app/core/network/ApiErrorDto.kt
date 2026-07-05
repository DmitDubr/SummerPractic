package com.vertical.app.core.network

import com.vertical.app.core.error.ApiErrorCode
import com.vertical.app.core.error.AppFailure
import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorDto(
    val code: String,
    val message: String,
)

fun ApiErrorDto.toFailure(): AppFailure.Api = AppFailure.Api(
    code = code.toApiErrorCode(),
    message = message,
)

private fun String.toApiErrorCode(): ApiErrorCode = when (this) {
    "VALIDATION_ERROR" -> ApiErrorCode.ValidationError
    "NO_SPOTS" -> ApiErrorCode.NoSpots
    "ONE_BOOKING_PER_DAY" -> ApiErrorCode.OneBookingPerDay
    "SLOT_CANCELLED" -> ApiErrorCode.SlotCancelled
    "RENTAL_UNAVAILABLE" -> ApiErrorCode.RentalUnavailable
    "SLOT_REBOOK_FORBIDDEN" -> ApiErrorCode.SlotRebookForbidden
    "ALREADY_CANCELLED" -> ApiErrorCode.AlreadyCancelled
    "CANCEL_TOO_LATE" -> ApiErrorCode.CancelTooLate
    "BOOKING_NOT_ATTENDED" -> ApiErrorCode.BookingNotAttended
    "ALREADY_RATED" -> ApiErrorCode.AlreadyRated
    "ALREADY_IN_WAITLIST" -> ApiErrorCode.AlreadyInWaitlist
    "WAITLIST_NOT_FOUND" -> ApiErrorCode.WaitlistNotFound
    "SERVER_ERROR" -> ApiErrorCode.ServerError
    else -> ApiErrorCode.Unknown
}
