package com.vertical.app.data

import com.vertical.app.core.network.VerticalApiClient
import com.vertical.app.data.dto.*
import com.vertical.app.domain.model.*
import com.vertical.app.session.SessionRepository
import io.ktor.client.request.*
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import kotlinx.datetime.LocalDate

interface SlotRepository {
    suspend fun listSlots(filters: SlotFilters): Result<List<SlotSummary>>
    suspend fun getSlot(slotId: SlotId): Result<SlotDetail>
}

interface InstructorRepository {
    suspend fun listInstructors(): Result<List<InstructorSummary>>
}

interface ProfileRepository {
    suspend fun getProfile(): Result<ClientProfile>
    suspend fun updateProfile(name: String, phone: String): Result<Pair<ClientProfile, String?>>
}

interface BookingRepository {
    suspend fun createBooking(
        slotId: SlotId,
        contacts: ClientContacts,
        equipment: EquipmentChoice,
    ): Result<CreateBookingResult>
    suspend fun listBookings(): Result<List<BookingSummary>>
    suspend fun getBooking(bookingId: BookingId): Result<Booking>
    suspend fun cancelBooking(bookingId: BookingId): Result<Unit>
    suspend fun leaveWaitlist(bookingId: BookingId): Result<Unit>
}

interface WaitlistRepository {
    suspend fun joinWaitlist(slotId: SlotId, contacts: ClientContacts?): Result<WaitlistEntry>
    suspend fun getWaitlistEntry(waitlistEntryId: String): Result<WaitlistEntry>
    suspend fun deleteWaitlistEntry(waitlistEntryId: String): Result<Unit>
}

interface RatingRepository {
    suspend fun createRating(bookingId: BookingId, instructorId: InstructorId, stars: Int): Result<Unit>
}

interface PushRepository {
    suspend fun registerPushToken(token: String, platform: String): Result<Unit>
}

class KtorSlotRepository(private val api: VerticalApiClient) : SlotRepository {
    override suspend fun listSlots(filters: SlotFilters): Result<List<SlotSummary>> =
        api.send<SlotListResponseDto>("/slots") {
            method = HttpMethod.Get
            filters.dateFrom?.let { parameter("dateFrom", it.toString()) }
            filters.dateTo?.let { parameter("dateTo", it.toString()) }
            filters.instructorIds.forEach { parameter("instructorIds", it.value) }
            filters.timeOfDay?.let { parameter("timeOfDay", it.toApi()) }
            filters.level?.let { parameter("level", it.toApi()) }
        }.map { response -> response.items.map { it.toDomain() } }

    override suspend fun getSlot(slotId: SlotId): Result<SlotDetail> =
        api.send<SlotDetailDto>("/slots/${slotId.value}") {
            method = HttpMethod.Get
        }.map { it.toDomain() }
}

class KtorWaitlistRepository(
    private val api: VerticalApiClient,
    private val sessionRepository: SessionRepository,
) : WaitlistRepository {
    override suspend fun joinWaitlist(slotId: SlotId, contacts: ClientContacts?): Result<WaitlistEntry> =
        api.send<WaitlistEntryDto>("/slots/${slotId.value}/waitlist", authorized = sessionRepository.token() != null) {
            method = HttpMethod.Post
            if (contacts != null) {
                setBody(JoinWaitlistRequestDto(ClientContactsDto(contacts.name, contacts.phone)))
            }
        }.map { dto ->
            dto.sessionToken?.let { sessionRepository.saveToken(it) }
            dto.toDomain()
        }

    override suspend fun getWaitlistEntry(waitlistEntryId: String): Result<WaitlistEntry> =
        api.send<WaitlistEntryDto>("/waitlist/$waitlistEntryId", authorized = true) {
            method = HttpMethod.Get
        }.map { it.toDomain() }

    override suspend fun deleteWaitlistEntry(waitlistEntryId: String): Result<Unit> =
        api.sendUnit("/waitlist/$waitlistEntryId", authorized = true) {
            method = HttpMethod.Delete
        }
}

class KtorInstructorRepository(private val api: VerticalApiClient) : InstructorRepository {
    override suspend fun listInstructors(): Result<List<InstructorSummary>> =
        api.send<InstructorListResponseDto>("/instructors") {
            method = HttpMethod.Get
        }.map { it.items.map { dto -> dto.toDomain() } }
}

class KtorProfileRepository(
    private val api: VerticalApiClient,
    private val sessionRepository: SessionRepository,
) : ProfileRepository {
    override suspend fun getProfile(): Result<ClientProfile> =
        api.send<ClientProfileDto>("/profile", authorized = true) {
            method = HttpMethod.Get
        }.map { it.toDomain() }

    override suspend fun updateProfile(name: String, phone: String): Result<Pair<ClientProfile, String?>> =
        api.send<UpdateProfileResponseDto>("/profile", authorized = sessionRepository.token() != null) {
            method = HttpMethod.Patch
            setBody(UpdateProfileRequestDto(name, phone))
        }.map { dto ->
            dto.sessionToken?.let { sessionRepository.saveToken(it) }
            dto.toDomain() to dto.sessionToken
        }
}

class KtorBookingRepository(
    private val api: VerticalApiClient,
    private val sessionRepository: SessionRepository,
) : BookingRepository {
    override suspend fun createBooking(
        slotId: SlotId,
        contacts: ClientContacts,
        equipment: EquipmentChoice,
    ): Result<CreateBookingResult> =
        api.send<CreateBookingResponseDto>("/bookings", authorized = sessionRepository.token() != null) {
            method = HttpMethod.Post
            setBody(
                CreateBookingRequestDto(
                    slotId = slotId.value,
                    client = ClientContactsDto(contacts.name, contacts.phone),
                    equipment = equipment.toDto(),
                ),
            )
        }.map { dto ->
            dto.sessionToken?.let { sessionRepository.saveToken(it) }
            CreateBookingResult(
                bookingId = BookingId(dto.id),
                totalPrice = dto.totalPrice ?: 0.0,
                sessionToken = dto.sessionToken,
            )
        }

    override suspend fun listBookings(): Result<List<BookingSummary>> =
        api.send<BookingListResponseDto>("/bookings", authorized = true) {
            method = HttpMethod.Get
        }.map { it.items.map { item -> item.toDomain() } }

    override suspend fun getBooking(bookingId: BookingId): Result<Booking> =
        api.send<BookingDto>("/bookings/${bookingId.value}", authorized = true) {
            method = HttpMethod.Get
        }.map { it.toDomain() }

    override suspend fun cancelBooking(bookingId: BookingId): Result<Unit> =
        api.sendUnit("/bookings/${bookingId.value}/cancel", authorized = true) {
            method = HttpMethod.Post
            setBody(mapOf<String, String>())
        }

    override suspend fun leaveWaitlist(bookingId: BookingId): Result<Unit> =
        api.sendUnit("/bookings/${bookingId.value}/leave-waitlist", authorized = true) {
            method = HttpMethod.Post
            setBody(mapOf<String, String>())
        }
}

class KtorRatingRepository(private val api: VerticalApiClient) : RatingRepository {
    override suspend fun createRating(
        bookingId: BookingId,
        instructorId: InstructorId,
        stars: Int,
    ): Result<Unit> =
        api.sendUnit("/ratings", authorized = true) {
            method = HttpMethod.Post
            setBody(
                CreateRatingRequestDto(
                    bookingId = bookingId.value,
                    instructorId = instructorId.value,
                    stars = stars,
                ),
            )
        }
}

class KtorPushRepository(private val api: VerticalApiClient) : PushRepository {
    override suspend fun registerPushToken(token: String, platform: String): Result<Unit> =
        api.sendUnit("/profile/push-token", authorized = true) {
            method = HttpMethod.Post
            setBody(RegisterPushTokenRequestDto(token = token, platform = platform))
        }
}
