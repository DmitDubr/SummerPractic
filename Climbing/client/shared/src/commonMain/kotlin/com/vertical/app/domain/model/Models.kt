package com.vertical.app.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class SlotId(val value: String)

@Serializable
data class BookingId(val value: String)

@Serializable
data class InstructorId(val value: String)

typealias MoneyRub = Double

enum class SlotStatus { Open, Full, Cancelled, Unavailable }

enum class BookingStatus {
    Active,
    CancelledByClient,
    CancelledByGym,
    Attended,
    Waitlist,
}

enum class EquipmentMode { Own, Rental }

enum class TrainingLevel { Beginner, Intermediate, Advanced }

enum class TimeOfDay { Morning, Afternoon, Evening }

data class InstructorSummary(
    val id: InstructorId,
    val name: String,
    val rating: Double?,
)

data class SlotSummary(
    val id: SlotId,
    val startAt: Instant,
    val format: String,
    val zone: String,
    val instructor: InstructorSummary,
    val freeSpots: Int,
    val capacity: Int,
    val price: MoneyRub?,
    val status: SlotStatus,
    val isBookable: Boolean,
)

data class PriceBreakdown(
    val trainingPrice: MoneyRub,
    val shoesRentalPrice: MoneyRub,
    val harnessRentalPrice: MoneyRub,
    val totalPrice: MoneyRub = trainingPrice,
)

data class RentalAvailability(
    val shoesAvailable: Int,
    val harnessAvailable: Int,
    val isBookable: Boolean,
)

data class GymInfo(
    val name: String,
    val address: String,
)

data class SlotDetail(
    val summary: SlotSummary,
    val endAt: Instant,
    val durationMinutes: Int,
    val priceBreakdown: PriceBreakdown,
    val rentalAvailability: RentalAvailability,
    val gym: GymInfo,
)

data class ClientProfile(
    val id: String,
    val name: String,
    val phone: String,
    val isComplete: Boolean,
    val isRegularClient: Boolean,
)

data class ClientContacts(
    val name: String,
    val phone: String,
)

data class EquipmentChoice(
    val mode: EquipmentMode,
    val rentalShoes: Boolean = false,
    val rentalHarness: Boolean = false,
)

data class BookingSummary(
    val id: BookingId,
    val status: BookingStatus,
    val totalPrice: MoneyRub,
    val slotStartAt: Instant,
    val formatName: String,
    val zoneName: String,
    val instructorName: String,
    val waitlistPosition: Int? = null,
)

data class Booking(
    val id: BookingId,
    val slotId: SlotId,
    val status: BookingStatus,
    val equipment: EquipmentChoice,
    val totalPrice: MoneyRub,
    val createdAt: Instant,
    val cancelledAt: Instant?,
    val slotStartsAt: Instant?,
    val slotEndsAt: Instant? = null,
    val formatName: String? = null,
    val zoneName: String? = null,
    val instructorName: String? = null,
    val instructorId: InstructorId? = null,
    val gym: GymInfo? = null,
    val cancellationReason: String? = null,
    val waitlistPosition: Int? = null,
)

data class SlotFilters(
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null,
    val instructorIds: Set<InstructorId> = emptySet(),
    val timeOfDay: TimeOfDay? = null,
    val level: TrainingLevel? = null,
)

data class DayGroup(
    val title: String,
    val slots: List<SlotSummary>,
)

data class WaitlistEntry(
    val id: String,
    val slotId: SlotId,
    val position: Int,
    val status: String,
)

data class CreateBookingResult(
    val bookingId: BookingId,
    val totalPrice: MoneyRub,
    val sessionToken: String?,
)
