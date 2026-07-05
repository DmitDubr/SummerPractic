package com.vertical.app

import com.vertical.app.domain.model.BookingId
import com.vertical.app.domain.model.SlotId
import kotlinx.serialization.Serializable

internal enum class MainTab(val title: String) {
    Schedule("Расписание"),
    Bookings("Мои записи"),
}

@Serializable internal data object ScheduleDestination

@Serializable internal data object BookingsDestination

@Serializable internal data class WaitlistDestination(val slotId: String)

@Serializable internal data class SlotDetailDestination(val slotId: String)

@Serializable internal data class BookingFormDestination(val slotId: String)

@Serializable internal data class BookingDetailDestination(val bookingId: String)

internal fun WaitlistDestination.id(): SlotId = SlotId(slotId)
internal fun SlotDetailDestination.id(): SlotId = SlotId(slotId)
internal fun BookingFormDestination.id(): SlotId = SlotId(slotId)
internal fun BookingDetailDestination.id(): BookingId = BookingId(bookingId)

internal fun MainTab.destination(): Any = when (this) {
    MainTab.Schedule -> ScheduleDestination
    MainTab.Bookings -> BookingsDestination
}
