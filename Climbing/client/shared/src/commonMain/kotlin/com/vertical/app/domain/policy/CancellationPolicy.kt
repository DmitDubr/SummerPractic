package com.vertical.app.domain.policy

import kotlinx.datetime.Instant

object CancellationPolicy {
    fun canCancel(status: com.vertical.app.domain.model.BookingStatus, slotStartsAt: Instant?, now: Instant): Boolean =
        status == com.vertical.app.domain.model.BookingStatus.Active &&
            slotStartsAt != null &&
            slotStartsAt > now

    fun canLeaveWaitlist(status: com.vertical.app.domain.model.BookingStatus): Boolean =
        status == com.vertical.app.domain.model.BookingStatus.Waitlist

    fun isLateCancel(slotStartsAt: Instant, now: Instant): Boolean {
        val minutes = (slotStartsAt - now).inWholeMinutes
        return minutes in 1 until 60
    }

    fun isEarlyCancel(slotStartsAt: Instant, now: Instant): Boolean {
        val minutes = (slotStartsAt - now).inWholeMinutes
        return minutes >= 60
    }
}
