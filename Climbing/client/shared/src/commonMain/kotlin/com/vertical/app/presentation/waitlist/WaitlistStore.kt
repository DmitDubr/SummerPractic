package com.vertical.app.presentation.waitlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vertical.app.core.error.asAppFailure
import com.vertical.app.core.mvi.MviStore
import com.vertical.app.core.ui.ActionStatus
import com.vertical.app.core.ui.Loadable
import com.vertical.app.data.ProfileRepository
import com.vertical.app.data.SlotRepository
import com.vertical.app.data.WaitlistRepository
import com.vertical.app.domain.model.*
import com.vertical.app.domain.policy.BookingErrorPolicy
import com.vertical.app.domain.policy.PhoneMaskFormatter
import com.vertical.app.domain.policy.PhoneValidator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WaitlistState(
    val slot: Loadable<SlotDetail> = Loadable.Initial,
    val name: String = "",
    val phone: String = "",
    val actionStatus: ActionStatus = ActionStatus.Idle,
    val error: com.vertical.app.domain.policy.BookingErrorUi? = null,
    val success: WaitlistEntry? = null,
) {
    val canSubmit: Boolean
        get() = slot is Loadable.Content &&
            PhoneValidator.isValid(PhoneValidator.normalize(phone)) &&
            name.trim().length >= 2 &&
            actionStatus != ActionStatus.Submitting &&
            success == null
}

sealed interface WaitlistIntent {
    data class Open(val slotId: SlotId) : WaitlistIntent
    data class NameChanged(val value: String) : WaitlistIntent
    data class PhoneChanged(val value: String) : WaitlistIntent
    data object Submit : WaitlistIntent
    data object ClearError : WaitlistIntent
}

class WaitlistStore(
    private val slotRepository: SlotRepository,
    private val waitlistRepository: WaitlistRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel(), MviStore<WaitlistState, WaitlistIntent, Nothing> {
    private val mutableState = MutableStateFlow(WaitlistState())
    private val effects = Channel<Nothing>(Channel.BUFFERED)
    override val state: StateFlow<WaitlistState> = mutableState

    override fun accept(intent: WaitlistIntent) {
        when (intent) {
            is WaitlistIntent.Open -> open(intent.slotId)
            is WaitlistIntent.NameChanged -> mutableState.update { it.copy(name = intent.value) }
            is WaitlistIntent.PhoneChanged -> mutableState.update { it.copy(phone = intent.value) }
            WaitlistIntent.Submit -> submit()
            WaitlistIntent.ClearError -> mutableState.update { it.copy(error = null) }
        }
    }

    override suspend fun effects(): Nothing = effects.receive()

    private fun open(slotId: SlotId) {
        viewModelScope.launch {
            mutableState.update { WaitlistState(slot = Loadable.Loading) }
            profileRepository.getProfile().onSuccess { profile ->
                mutableState.update {
                    it.copy(
                        name = profile.name,
                        phone = PhoneMaskFormatter.format(profile.phone),
                    )
                }
            }
            slotRepository.getSlot(slotId).fold(
                onSuccess = { detail -> mutableState.update { it.copy(slot = Loadable.Content(detail)) } },
                onFailure = { e -> mutableState.update { it.copy(slot = Loadable.Error(e.asAppFailure())) } },
            )
        }
    }

    private fun submit() {
        val state = mutableState.value
        val detail = (state.slot as? Loadable.Content)?.value ?: return
        val contacts = ClientContacts(state.name.trim(), PhoneValidator.normalize(state.phone))
        viewModelScope.launch {
            mutableState.update { it.copy(actionStatus = ActionStatus.Submitting, error = null) }
            waitlistRepository.joinWaitlist(detail.summary.id, contacts).fold(
                onSuccess = { entry ->
                    profileRepository.updateProfile(contacts.name, contacts.phone)
                    mutableState.update { it.copy(actionStatus = ActionStatus.Idle, success = entry) }
                },
                onFailure = { e ->
                    mutableState.update {
                        it.copy(actionStatus = ActionStatus.Idle, error = BookingErrorPolicy.fromFailure(e.asAppFailure()))
                    }
                },
            )
        }
    }
}
