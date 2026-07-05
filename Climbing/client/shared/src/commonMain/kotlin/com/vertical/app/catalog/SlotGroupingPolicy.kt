package com.vertical.app.catalog

import com.vertical.app.domain.model.*
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

object SlotGroupingPolicy {
    fun group(slots: List<SlotSummary>, now: Instant, zone: TimeZone = TimeZone.currentSystemDefault()): List<DayGroup> {
        val today = now.toLocalDateTime(zone).date
        val tomorrow = today.plus(DatePeriod(days = 1))
        return slots
            .sortedBy { it.startAt }
            .groupBy { it.startAt.toLocalDateTime(zone).date }
            .map { (date, daySlots) ->
                val title = when (date) {
                    today -> "Сегодня, ${date.dayOfMonth}.${pad(date.monthNumber)}"
                    tomorrow -> "Завтра, ${date.dayOfMonth}.${pad(date.monthNumber)}"
                    else -> {
                        val dow = when (date.dayOfWeek) {
                            kotlinx.datetime.DayOfWeek.MONDAY -> "Понедельник"
                            kotlinx.datetime.DayOfWeek.TUESDAY -> "Вторник"
                            kotlinx.datetime.DayOfWeek.WEDNESDAY -> "Среда"
                            kotlinx.datetime.DayOfWeek.THURSDAY -> "Четверг"
                            kotlinx.datetime.DayOfWeek.FRIDAY -> "Пятница"
                            kotlinx.datetime.DayOfWeek.SATURDAY -> "Суббота"
                            kotlinx.datetime.DayOfWeek.SUNDAY -> "Воскресенье"
                            else -> ""
                        }
                        "$dow, ${date.dayOfMonth}.${pad(date.monthNumber)}"
                    }
                }
                DayGroup(title, daySlots)
            }
    }

    private fun pad(n: Int) = n.toString().padStart(2, '0')
}
