package com.vertical.app.presentation.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vertical.app.core.error.AppFailure
import com.vertical.app.core.error.asAppFailure
import com.vertical.app.core.mvi.MviStore
import com.vertical.app.core.ui.ActionStatus
import com.vertical.app.core.ui.Loadable
import com.vertical.app.data.BookingRepository
import com.vertical.app.data.ProfileRepository
import com.vertical.app.data.SlotRepository
import com.vertical.app.domain.model.*
import com.vertical.app.domain.policy.BookingErrorPolicy
import com.vertical.app.domain.policy.BookingErrorUi
import com.vertical.app.domain.policy.BookingPriceCalculator
import com.vertical.app.domain.policy.PhoneMaskFormatter
import com.vertical.app.domain.policy.PhoneValidator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookingFormState(
    val slot: Loadable<SlotDetail> = Loadable.Initial,
    val name: String = "",
    val phone: String = "",
    val equipmentMode: EquipmentMode = EquipmentMode.Own,
    val rentalShoes: Boolean = false,
    val rentalHarness: Boolean = false,
    val profileLoaded: Boolean = false,
    val actionStatus: ActionStatus = ActionStatus.Idle,
    val bookingError: BookingErrorUi? = null,
    val success: CreateBookingResult? = null,
) {
    val equipment: EquipmentChoice
        get() = EquipmentChoice(equipmentMode, rentalShoes, rentalHarness)

    val totalPreview: Double?
        get() = (slot as? Loadable.Content)?.value?.let {
            BookingPriceCalculator.preview(it.priceBreakdown, equipment)
        }

    val normalizedPhone: String
        get() = PhoneValidator.normalize(phone)

    val canSubmit: Boolean
        get() = slot is Loadable.Content &&
            actionStatus != ActionStatus.Submitting &&
            success == null &&
            validationReason == null

    val validationReason: String?
        get() {
            if (slot !is Loadable.Content) return null
            if (name.trim().length < 2) return "Укажите имя (минимум 2 символа)"
            if (!PhoneValidator.isValid(normalizedPhone)) {
                return "Введите телефон в формате +7XXXXXXXXXX"
            }
            if (equipmentMode == EquipmentMode.Rental && !rentalShoes && !rentalHarness) {
                return "Выберите позиции проката"
            }
            val detail = (slot as Loadable.Content).value
            if (equipmentMode == EquipmentMode.Rental) {
                val rental = detail.rentalAvailability
                if (rentalShoes && rental.shoesAvailable <= 0) return "Скальники для проката закончились"
                if (rentalHarness && rental.harnessAvailable <= 0) {
                    return "Страховочные системы для проката закончились"
                }
            }
            val summary = detail.summary
            if (summary.freeSpots <= 0) return "Места закончились — можно встать в лист ожидания"
            if (!summary.isBookable) return "Запись на этот слот сейчас недоступна"
            return null
        }
}

sealed interface BookingFormIntent {
    data class Open(val slotId: SlotId) : BookingFormIntent
    data class NameChanged(val value: String) : BookingFormIntent
    data class PhoneChanged(val value: String) : BookingFormIntent
    data class SetEquipmentMode(val mode: EquipmentMode) : BookingFormIntent
    data object ToggleShoes : BookingFormIntent
    data object ToggleHarness : BookingFormIntent
    data object Submit : BookingFormIntent
    data object DismissSuccess : BookingFormIntent
    data object ClearError : BookingFormIntent
    data object GoWaitlist : BookingFormIntent
}

class BookingFormStore(
    private val slotRepository: SlotRepository,
    private val bookingRepository: BookingRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel(), MviStore<BookingFormState, BookingFormIntent, SlotId> {
    private val mutableState = MutableStateFlow(BookingFormState())
    private val effects = Channel<SlotId>(Channel.BUFFERED)
    override val state: StateFlow<BookingFormState> = mutableState

    override fun accept(intent: BookingFormIntent) {
        when (intent) {
            is BookingFormIntent.Open -> open(intent.slotId)
            is BookingFormIntent.NameChanged -> mutableState.update { it.copy(name = intent.value) }
            is BookingFormIntent.PhoneChanged -> mutableState.update { it.copy(phone = intent.value) }
            is BookingFormIntent.SetEquipmentMode -> mutableState.update { state ->
                if (intent.mode == EquipmentMode.Own) {
                    state.copy(equipmentMode = EquipmentMode.Own, rentalShoes = false, rentalHarness = false)
                } else {
                    val rental = (state.slot as? Loadable.Content)?.value?.rentalAvailability
                    val shoesOk = (rental?.shoesAvailable ?: 0) > 0
                    val harnessOk = (rental?.harnessAvailable ?: 0) > 0
                    state.copy(
                        equipmentMode = EquipmentMode.Rental,
                        rentalShoes = shoesOk,
                        rentalHarness = !shoesOk && harnessOk,
                    )
                }
            }
            BookingFormIntent.ToggleShoes -> mutableState.update { it.copy(rentalShoes = !it.rentalShoes) }
            BookingFormIntent.ToggleHarness -> mutableState.update { it.copy(rentalHarness = !it.rentalHarness) }
            BookingFormIntent.Submit -> submit()
            BookingFormIntent.DismissSuccess -> mutableState.update { it.copy(success = null) }
            BookingFormIntent.ClearError -> mutableState.update { it.copy(bookingError = null) }
            BookingFormIntent.GoWaitlist -> {
                val slotId = (mutableState.value.slot as? Loadable.Content)?.value?.summary?.id
                if (slotId != null) {
                    viewModelScope.launch {
                        mutableState.update { it.copy(bookingError = null) }
                        effects.send(slotId)
                    }
                }
            }
        }
    }

    override suspend fun effects(): SlotId = effects.receive()

    private fun open(slotId: SlotId) {
        viewModelScope.launch {
            mutableState.update { BookingFormState(slot = Loadable.Loading) }
            profileRepository.getProfile().onSuccess { profile ->
                mutableState.update {
                    it.copy(
                        name = profile.name,
                        phone = PhoneMaskFormatter.format(profile.phone),
                        profileLoaded = true,
                    )
                }
            }
            slotRepository.getSlot(slotId).fold(
                onSuccess = { detail -> mutableState.update { it.copy(slot = Loadable.Content(detail)) } },
                onFailure = { e ->
                    val failure = e.asAppFailure()
                    if (failure != AppFailure.Unauthorized) {
                        mutableState.update { it.copy(slot = Loadable.Error(failure)) }
                    } else {
                        slotRepository.getSlot(slotId).fold(
                            onSuccess = { detail -> mutableState.update { it.copy(slot = Loadable.Content(detail)) } },
                            onFailure = { err -> mutableState.update { it.copy(slot = Loadable.Error(err.asAppFailure())) } },
                        )
                    }
                },
            )
        }
    }

    private fun submit() {
        val state = mutableState.value
        if (state.validationReason != null) return
        val detail = (state.slot as? Loadable.Content)?.value ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(actionStatus = ActionStatus.Submitting, bookingError = null) }
            val refreshed = slotRepository.getSlot(detail.summary.id).getOrNull()
            if (refreshed != null) {
                mutableState.update { it.copy(slot = Loadable.Content(refreshed)) }
                val summary = refreshed.summary
                if (summary.freeSpots <= 0 || !summary.isBookable) {
                    mutableState.update {
                        it.copy(
                            actionStatus = ActionStatus.Idle,
                            bookingError = BookingErrorPolicy.fromFailure(
                                com.vertical.app.core.error.AppFailure.Api(
                                    com.vertical.app.core.error.ApiErrorCode.NoSpots,
                                    if (summary.freeSpots <= 0) {
                                        "Места закончились"
                                    } else {
                                        "Прокат на это время закончился"
                                    },
                                ),
                            ),
                        )
                    }
                    return@launch
                }
            }
            val contacts = ClientContacts(state.name.trim(), state.normalizedPhone)
            bookingRepository.createBooking(detail.summary.id, contacts, state.equipment).fold(
                onSuccess = { result ->
                    mutableState.update {
                        it.copy(actionStatus = ActionStatus.Idle, success = result)
                    }
                },
                onFailure = { e ->
                    mutableState.update {
                        it.copy(
                            actionStatus = ActionStatus.Idle,
                            bookingError = BookingErrorPolicy.fromFailure(e.asAppFailure()),
                        )
                    }
                },
            )
        }
    }
}
