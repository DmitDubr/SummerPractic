package com.vertical.app.data.dto

import com.vertical.app.domain.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class SlotListResponseDto(
    val items: List<SlotSummaryDto> = emptyList(),
    val meta: PaginationMetaDto? = null,
)

@Serializable
data class PaginationMetaDto(
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
)

@Serializable
data class SlotSummaryDto(
    val id: String,
    val startAt: Instant,
    val format: String? = null,
    val zone: String? = null,
    val instructor: InstructorSummaryDto,
    val freeSpots: Int,
    val capacity: Int,
    val price: Double? = null,
    val status: String,
    val isBookable: Boolean,
)

@Serializable
data class InstructorSummaryDto(
    val id: String,
    val name: String,
    val rating: Double? = null,
)

@Serializable
data class SlotDetailDto(
    val id: String,
    val startAt: Instant,
    val endAt: Instant,
    val durationMinutes: Int = 0,
    val format: String? = null,
    val zone: String? = null,
    val instructor: InstructorSummaryDto,
    val freeSpots: Int,
    val capacity: Int,
    val price: Double? = null,
    val status: String,
    val isBookable: Boolean,
    val priceBreakdown: PriceBreakdownDto? = null,
    val rentalAvailability: RentalAvailabilityDto? = null,
    val gym: GymInfoDto? = null,
)

@Serializable
data class PriceBreakdownDto(
    val trainingPrice: Double = 0.0,
    val shoesRentalPrice: Double = 0.0,
    val harnessRentalPrice: Double = 0.0,
    val totalPrice: Double = 0.0,
)

@Serializable
data class RentalAvailabilityDto(
    val shoesAvailable: Int = 0,
    val harnessAvailable: Int = 0,
    val isBookable: Boolean = true,
)

@Serializable
data class GymInfoDto(
    val name: String = "",
    val address: String = "",
)

@Serializable
data class InstructorListResponseDto(
    val items: List<InstructorSummaryDto> = emptyList(),
)

@Serializable
data class ClientProfileDto(
    val id: String? = null,
    val name: String,
    val phone: String,
    val isComplete: Boolean = false,
    val isRegularClient: Boolean = false,
)

@Serializable
data class UpdateProfileRequestDto(
    val name: String,
    val phone: String,
)

@Serializable
data class UpdateProfileResponseDto(
    val id: String? = null,
    val name: String,
    val phone: String,
    val isComplete: Boolean = false,
    val isRegularClient: Boolean = false,
    val sessionToken: String? = null,
)

@Serializable
data class CreateBookingRequestDto(
    val slotId: String,
    val client: ClientContactsDto,
    val equipment: EquipmentChoiceDto,
)

@Serializable
data class ClientContactsDto(
    val name: String,
    val phone: String,
)

@Serializable
data class EquipmentChoiceDto(
    val mode: String,
    val rentalShoes: Boolean = false,
    val rentalHarness: Boolean = false,
)

@Serializable
data class JoinWaitlistRequestDto(
    val client: ClientContactsDto? = null,
)

@Serializable
data class WaitlistEntryDto(
    val id: String,
    val slotId: String,
    val position: Int,
    val status: String,
    val sessionToken: String? = null,
)

fun WaitlistEntryDto.toDomain(): WaitlistEntry = WaitlistEntry(
    id = id,
    slotId = SlotId(slotId),
    position = position,
    status = status,
)

@Serializable
data class CreateBookingResponseDto(
    val id: String,
    val slotId: String? = null,
    val status: String? = null,
    val totalPrice: Double? = null,
    val sessionToken: String? = null,
)

@Serializable
data class BookingDto(
    val id: String,
    val slotId: String? = null,
    val status: String,
    val equipment: EquipmentChoiceDto? = null,
    val totalPrice: Double = 0.0,
    val createdAt: Instant? = null,
    val cancelledAt: Instant? = null,
    val cancellationReason: String? = null,
    val waitlistPosition: Int? = null,
    val slot: BookingSlotDto? = null,
    val gym: GymInfoDto? = null,
)

@Serializable
data class BookingSlotDto(
    val startsAt: Instant? = null,
    val endsAt: Instant? = null,
    val format: BookingFormatDto? = null,
    val zone: BookingSummaryZoneDto? = null,
    val instructor: BookingDetailInstructorDto? = null,
)

@Serializable
data class BookingDetailInstructorDto(
    val id: String? = null,
    val name: String? = null,
    @SerialName("fullName") val fullName: String? = null,
)

@Serializable
data class BookingFormatDto(val name: String? = null)

@Serializable
data class BookingListResponseDto(
    val items: List<BookingSummaryDto> = emptyList(),
)

@Serializable
data class BookingSummaryDto(
    val id: String,
    val status: String,
    val totalPrice: Double = 0.0,
    val waitlistPosition: Int? = null,
    val slot: BookingSummarySlotDto? = null,
)

@Serializable
data class BookingSummarySlotDto(
    val startsAt: Instant? = null,
    val format: BookingSummaryFormatDto? = null,
    val zone: BookingSummaryZoneDto? = null,
    val instructor: BookingSummaryInstructorDto? = null,
)

@Serializable
data class BookingSummaryFormatDto(val name: String? = null)
@Serializable
data class BookingSummaryZoneDto(val name: String? = null)
@Serializable
data class BookingSummaryInstructorDto(@SerialName("fullName") val fullName: String? = null)

@Serializable
data class CancelBookingResponseDto(
    val id: String,
    val status: String,
    val cancelledAt: Instant? = null,
)

@Serializable
data class CreateRatingRequestDto(
    val bookingId: String,
    val instructorId: String,
    val stars: Int,
)

@Serializable
data class RegisterPushTokenRequestDto(
    val token: String,
    val platform: String,
)

fun SlotSummaryDto.toDomain(): SlotSummary = SlotSummary(
    id = SlotId(id),
    startAt = startAt,
    format = format.orEmpty(),
    zone = zone.orEmpty(),
    instructor = instructor.toDomain(),
    freeSpots = freeSpots,
    capacity = capacity,
    price = price,
    status = status.toSlotStatus(),
    isBookable = isBookable,
)

fun InstructorSummaryDto.toDomain(): InstructorSummary = InstructorSummary(
    id = InstructorId(id),
    name = name,
    rating = rating,
)

fun SlotDetailDto.toDomain(): SlotDetail {
    val summary = SlotSummary(
        id = SlotId(id),
        startAt = startAt,
        format = format.orEmpty(),
        zone = zone.orEmpty(),
        instructor = instructor.toDomain(),
        freeSpots = freeSpots,
        capacity = capacity,
        price = price,
        status = status.toSlotStatus(),
        isBookable = isBookable,
    )
    val pb = priceBreakdown ?: PriceBreakdownDto(trainingPrice = price ?: 0.0)
    return SlotDetail(
        summary = summary,
        endAt = endAt,
        durationMinutes = durationMinutes,
        priceBreakdown = PriceBreakdown(
            trainingPrice = pb.trainingPrice,
            shoesRentalPrice = pb.shoesRentalPrice,
            harnessRentalPrice = pb.harnessRentalPrice,
            totalPrice = pb.totalPrice,
        ),
        rentalAvailability = rentalAvailability?.let {
            RentalAvailability(it.shoesAvailable, it.harnessAvailable, it.isBookable)
        } ?: RentalAvailability(0, 0, false),
        gym = gym?.let { GymInfo(it.name, it.address) } ?: GymInfo("", ""),
    )
}

fun UpdateProfileResponseDto.toDomain(): ClientProfile = ClientProfile(
    id = id.orEmpty(),
    name = name,
    phone = phone,
    isComplete = isComplete,
    isRegularClient = isRegularClient,
)

fun ClientProfileDto.toDomain(): ClientProfile = ClientProfile(
    id = id.orEmpty(),
    name = name,
    phone = phone,
    isComplete = isComplete,
    isRegularClient = isRegularClient,
)

fun BookingSummaryDto.toDomain(): BookingSummary = BookingSummary(
    id = BookingId(id),
    status = status.toBookingStatus(),
    totalPrice = totalPrice,
    slotStartAt = slot?.startsAt ?: Instant.fromEpochMilliseconds(0),
    formatName = slot?.format?.name.orEmpty(),
    zoneName = slot?.zone?.name.orEmpty(),
    instructorName = slot?.instructor?.fullName.orEmpty(),
    waitlistPosition = waitlistPosition,
)

fun BookingDto.toDomain(): Booking {
    val instructor = slot?.instructor
    return Booking(
        id = BookingId(id),
        slotId = SlotId(slotId.orEmpty()),
        status = status.toBookingStatus(),
        equipment = equipment?.toDomain() ?: EquipmentChoice(EquipmentMode.Own),
        totalPrice = totalPrice,
        createdAt = createdAt ?: Instant.fromEpochMilliseconds(0),
        cancelledAt = cancelledAt,
        slotStartsAt = slot?.startsAt,
        slotEndsAt = slot?.endsAt,
        formatName = slot?.format?.name,
        zoneName = slot?.zone?.name,
        instructorName = instructor?.name ?: instructor?.fullName,
        instructorId = instructor?.id?.let { InstructorId(it) },
        gym = gym?.let { GymInfo(it.name.orEmpty(), it.address.orEmpty()) },
        cancellationReason = cancellationReason,
        waitlistPosition = waitlistPosition,
    )
}

fun EquipmentChoiceDto.toDomain(): EquipmentChoice = EquipmentChoice(
    mode = if (mode == "RENTAL") EquipmentMode.Rental else EquipmentMode.Own,
    rentalShoes = rentalShoes,
    rentalHarness = rentalHarness,
)

fun EquipmentChoice.toDto(): EquipmentChoiceDto = EquipmentChoiceDto(
    mode = if (mode == EquipmentMode.Rental) "RENTAL" else "OWN",
    rentalShoes = rentalShoes,
    rentalHarness = rentalHarness,
)

fun TrainingLevel.toApi(): String = when (this) {
    TrainingLevel.Beginner -> "beginner"
    TrainingLevel.Intermediate -> "intermediate"
    TrainingLevel.Advanced -> "advanced"
}

fun TimeOfDay.toApi(): String = when (this) {
    TimeOfDay.Morning -> "morning"
    TimeOfDay.Afternoon -> "afternoon"
    TimeOfDay.Evening -> "evening"
}

private fun String.toSlotStatus(): SlotStatus = when (uppercase()) {
    "OPEN" -> SlotStatus.Open
    "FULL" -> SlotStatus.Full
    "CANCELLED" -> SlotStatus.Cancelled
    "UNAVAILABLE" -> SlotStatus.Unavailable
    else -> SlotStatus.Unavailable
}

private fun String.toBookingStatus(): BookingStatus = when (this) {
    "ACTIVE" -> BookingStatus.Active
    "CANCELLED_BY_CLIENT" -> BookingStatus.CancelledByClient
    "CANCELLED_BY_GYM" -> BookingStatus.CancelledByGym
    "ATTENDED" -> BookingStatus.Attended
    "WAITLIST" -> BookingStatus.Waitlist
    else -> BookingStatus.Active
}
