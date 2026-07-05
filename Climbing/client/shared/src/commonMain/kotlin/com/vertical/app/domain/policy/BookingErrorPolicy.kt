package com.vertical.app.domain.policy

import com.vertical.app.core.error.ApiErrorCode
import com.vertical.app.core.error.AppFailure
import com.vertical.app.core.error.userMessage

data class BookingErrorUi(
    val title: String,
    val message: String,
    val showWaitlistCta: Boolean = false,
)

object BookingErrorPolicy {
    fun fromFailure(failure: AppFailure): BookingErrorUi = when (failure) {
        is AppFailure.Api -> fromCode(failure.code, failure.message)
        AppFailure.NetworkUnavailable -> BookingErrorUi(
            title = "Нет сети",
            message = "Проверьте подключение и попробуйте снова",
        )
        AppFailure.Timeout -> BookingErrorUi(
            title = "Таймаут",
            message = "Сервер не ответил вовремя. Попробуйте ещё раз",
        )
        else -> BookingErrorUi(
            title = "Ошибка записи",
            message = "Не удалось оформить запись",
        )
    }

    private fun fromCode(code: ApiErrorCode, serverMessage: String): BookingErrorUi = when (code) {
        ApiErrorCode.NoSpots -> BookingErrorUi(
            title = "Места закончились",
            message = serverMessage.ifBlank { "На эту тренировку нет свободных мест" },
            showWaitlistCta = true,
        )
        ApiErrorCode.OneBookingPerDay -> BookingErrorUi(
            title = "Уже есть запись",
            message = serverMessage.ifBlank { "Можно записаться только на одну тренировку в день" },
        )
        ApiErrorCode.RentalUnavailable -> BookingErrorUi(
            title = "Прокат недоступен",
            message = serverMessage.ifBlank { "Прокатное снаряжение закончилось" },
        )
        ApiErrorCode.SlotCancelled -> BookingErrorUi(
            title = "Тренировка отменена",
            message = serverMessage.ifBlank { "Скалодром отменил эту тренировку" },
        )
        ApiErrorCode.SlotRebookForbidden -> BookingErrorUi(
            title = "Запись недоступна",
            message = serverMessage.ifBlank { "Повторная запись на этот слот невозможна" },
        )
        ApiErrorCode.AlreadyInWaitlist -> BookingErrorUi(
            title = "Уже в очереди",
            message = serverMessage.ifBlank { "Вы уже в листе ожидания на этот слот" },
        )
        ApiErrorCode.ValidationError -> BookingErrorUi(
            title = "Проверьте данные",
            message = serverMessage.ifBlank { "Заполните форму корректно" },
        )
        ApiErrorCode.ServerError -> BookingErrorUi(
            title = "Ошибка сервера",
            message = serverMessage.ifBlank { "Попробуйте позже или обратитесь в скалодром" },
        )
        else -> BookingErrorUi(
            title = "Ошибка записи",
            message = serverMessage.ifBlank { "Не удалось оформить запись" },
        )
    }
}

object BookingStatusLabels {
    fun label(status: com.vertical.app.domain.model.BookingStatus): String = when (status) {
        com.vertical.app.domain.model.BookingStatus.Active -> "Записан"
        com.vertical.app.domain.model.BookingStatus.Waitlist -> "В очереди"
        com.vertical.app.domain.model.BookingStatus.CancelledByClient -> "Отменена вами"
        com.vertical.app.domain.model.BookingStatus.CancelledByGym -> "Отменена скалодромом"
        com.vertical.app.domain.model.BookingStatus.Attended -> "Посещена"
    }
}

object CancelErrorPolicy {
    fun message(failure: AppFailure): String = when (failure) {
        is AppFailure.Api -> when (failure.code) {
            ApiErrorCode.AlreadyCancelled -> "Запись уже отменена"
            ApiErrorCode.CancelTooLate -> failure.message.ifBlank { "Отменить запись уже нельзя" }
            else -> failure.message.ifBlank { "Не удалось отменить запись" }
        }
        else -> failure.userMessage()
    }
}

object EquipmentLabels {
    fun label(equipment: com.vertical.app.domain.model.EquipmentChoice): String = when {
        equipment.mode == com.vertical.app.domain.model.EquipmentMode.Own -> "Своё снаряжение"
        equipment.rentalShoes && equipment.rentalHarness -> "Прокат: скальники и страховка"
        equipment.rentalShoes -> "Прокат: скальники"
        equipment.rentalHarness -> "Прокат: страховка"
        equipment.mode == com.vertical.app.domain.model.EquipmentMode.Rental -> "Прокат"
        else -> "Своё снаряжение"
    }
}

enum class BookingsSegment { Upcoming, Past }

object BookingsSegmentPolicy {
    fun filter(list: List<com.vertical.app.domain.model.BookingSummary>, segment: BookingsSegment) =
        when (segment) {
            BookingsSegment.Upcoming -> list.filter {
                it.status == com.vertical.app.domain.model.BookingStatus.Active ||
                    it.status == com.vertical.app.domain.model.BookingStatus.Waitlist
            }
            BookingsSegment.Past -> list.filter {
                it.status != com.vertical.app.domain.model.BookingStatus.Active &&
                    it.status != com.vertical.app.domain.model.BookingStatus.Waitlist
            }
        }
}
