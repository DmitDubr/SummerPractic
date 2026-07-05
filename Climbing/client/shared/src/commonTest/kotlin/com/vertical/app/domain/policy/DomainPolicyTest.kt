package com.vertical.app.domain.policy

import com.vertical.app.domain.model.EquipmentChoice
import com.vertical.app.domain.model.EquipmentMode
import com.vertical.app.domain.model.PriceBreakdown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DomainPolicyTest {
    @Test
    fun bookingPriceOwnOnly() {
        val pb = PriceBreakdown(1200.0, 200.0, 300.0)
        val eq = EquipmentChoice(EquipmentMode.Own)
        assertEquals(1200.0, BookingPriceCalculator.preview(pb, eq))
    }

    @Test
    fun bookingPriceRentalBoth() {
        val pb = PriceBreakdown(1200.0, 200.0, 300.0)
        val eq = EquipmentChoice(EquipmentMode.Rental, rentalShoes = true, rentalHarness = true)
        assertEquals(1700.0, BookingPriceCalculator.preview(pb, eq))
    }

    @Test
    fun phoneValidation() {
        assertTrue(PhoneValidator.isValid("+79001234567"))
        assertEquals("+79001234567", PhoneValidator.normalize("8 900 123 45 67"))
        assertEquals("+79001234567", PhoneValidator.normalize("+7 (900) 123-45-67"))
    }

    @Test
    fun phoneMaskFormatting() {
        assertEquals("", PhoneMaskFormatter.format(""))
        assertEquals("+7 (900", PhoneMaskFormatter.format("900"))
        assertEquals("+7 (900) 123-45-67", PhoneMaskFormatter.format("+79001234567"))
        assertEquals("+7 (900) 123-45-67", PhoneMaskFormatter.format("89001234567"))
    }

    @Test
    fun spotsShortLabelShowsFreeCapacity() {
        assertEquals("Мест нет", SlotAvailabilityPolicy.spotsShortLabel(0, 8, 1200.0))
        assertEquals("Осталось 2 из 8 · 1200 ₽", SlotAvailabilityPolicy.spotsShortLabel(2, 8, 1200.0))
        assertEquals("Свободно 3 из 8 · 1200 ₽", SlotAvailabilityPolicy.spotsShortLabel(3, 8, 1200.0))
    }
}
