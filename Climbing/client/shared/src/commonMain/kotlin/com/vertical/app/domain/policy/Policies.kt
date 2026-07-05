package com.vertical.app.domain.policy

import com.vertical.app.domain.model.*

enum class SlotCta { Book, Waitlist, Disabled }

object SlotAvailabilityPolicy {
    fun cta(slot: SlotSummary): SlotCta = when {
        slot.status == SlotStatus.Cancelled -> SlotCta.Disabled
        slot.status == SlotStatus.Unavailable -> SlotCta.Disabled
        slot.freeSpots <= 0 -> SlotCta.Waitlist
        slot.isBookable && slot.status == SlotStatus.Open -> SlotCta.Book
        slot.freeSpots > 0 -> SlotCta.Disabled
        else -> SlotCta.Disabled
    }

    fun spotsLabel(freeSpots: Int, capacity: Int): String = when {
        freeSpots <= 0 -> "Мест нет"
        freeSpots <= 2 -> "Осталось $freeSpots из $capacity"
        else -> "Свободно $freeSpots из $capacity"
    }

    fun spotsShortLabel(freeSpots: Int, capacity: Int, price: MoneyRub?): String {
        val priceSuffix = price?.let { " · ${it.toInt()} ₽" }.orEmpty()
        return when {
            freeSpots <= 0 -> "Мест нет"
            freeSpots <= 2 -> "Осталось $freeSpots из $capacity$priceSuffix"
            else -> "Свободно $freeSpots из $capacity$priceSuffix"
        }
    }

    fun disabledReason(slot: SlotSummary, rentalBookable: Boolean = true): String? = when {
        slot.status == SlotStatus.Cancelled -> "Тренировка отменена"
        slot.status == SlotStatus.Unavailable -> rentalUnavailableMessage()
        slot.freeSpots > 0 && !slot.isBookable -> rentalUnavailableMessage()
        slot.freeSpots > 0 && !rentalBookable -> rentalUnavailableMessage()
        slot.freeSpots > 0 && slot.status != SlotStatus.Open -> "Запись недоступна"
        else -> null
    }

    private fun rentalUnavailableMessage() =
        "Прокат на это время закончился. Запись недоступна"
}

object BookingPriceCalculator {
    fun preview(breakdown: PriceBreakdown, equipment: EquipmentChoice): MoneyRub {
        var total = breakdown.trainingPrice
        if (equipment.mode == EquipmentMode.Rental) {
            if (equipment.rentalShoes) total += breakdown.shoesRentalPrice
            if (equipment.rentalHarness) total += breakdown.harnessRentalPrice
        }
        return total
    }
}

object PhoneValidator {
    private val phoneRegex = Regex("^\\+7\\d{10}$")

    fun isValid(phone: String): Boolean = phoneRegex.matches(phone)

    fun normalize(input: String): String {
        val digits = input.filter { it.isDigit() }
        return when {
            digits.length == 11 && digits.startsWith("7") -> "+$digits"
            digits.length == 11 && digits.startsWith("8") -> "+7${digits.drop(1)}"
            digits.length == 10 -> "+7$digits"
            else -> input.trim()
        }
    }
}

object SlotFilterPolicy {
    fun filterBadgeCount(filters: SlotFilters): Int {
        var count = 0
        if (!isDefaultDateRange(filters)) count++
        if (filters.instructorIds.isNotEmpty()) count++
        if (filters.timeOfDay != null) count++
        if (filters.level != null) count++
        return count
    }

    fun isDefaultDateRange(filters: SlotFilters): Boolean =
        filters.dateFrom == null && filters.dateTo == null

    fun dateRangeChipLabel(filters: SlotFilters): String {
        if (isDefaultDateRange(filters)) return "7 дней"
        val from = filters.dateFrom?.toString().orEmpty()
        val to = filters.dateTo?.toString().orEmpty()
        return if (from.isNotEmpty() && to.isNotEmpty()) "$from – $to" else "Период"
    }
}
