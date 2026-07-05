package com.vertical.app.domain.policy

import com.vertical.app.domain.model.SlotFilters
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

enum class QuickDatePreset(val label: String) {
    Today("Сегодня"),
    Tomorrow("Завтра"),
    Week("7 дней"),
}

object QuickDateFilter {
    fun range(preset: QuickDatePreset, now: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): Pair<LocalDate, LocalDate> {
        val today = now.toLocalDateTime(timeZone).date
        return when (preset) {
            QuickDatePreset.Today -> today to today
            QuickDatePreset.Tomorrow -> {
                val tomorrow = today.plus(DatePeriod(days = 1))
                tomorrow to tomorrow
            }
            QuickDatePreset.Week -> today to today.plus(DatePeriod(days = 6))
        }
    }

    fun activePreset(filters: SlotFilters, now: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): QuickDatePreset? {
        val from = filters.dateFrom ?: return null
        val to = filters.dateTo ?: return null
        val today = now.toLocalDateTime(timeZone).date
        val tomorrow = today.plus(DatePeriod(days = 1))
        val weekEnd = today.plus(DatePeriod(days = 6))

        return when {
            from == today && to == today -> QuickDatePreset.Today
            from == tomorrow && to == tomorrow -> QuickDatePreset.Tomorrow
            from == today && to == weekEnd -> QuickDatePreset.Week
            else -> null
        }
    }
}
