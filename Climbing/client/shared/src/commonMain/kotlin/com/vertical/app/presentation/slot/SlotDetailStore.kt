package com.vertical.app.presentation.slot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vertical.app.core.error.asAppFailure
import com.vertical.app.core.mvi.MviStore
import com.vertical.app.core.ui.Loadable
import com.vertical.app.data.SlotRepository
import com.vertical.app.domain.model.SlotDetail
import com.vertical.app.domain.model.SlotId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SlotDetailState(
    val slot: Loadable<SlotDetail> = Loadable.Initial,
    val slotId: SlotId? = null,
)

sealed interface SlotDetailIntent {
    data class Open(val slotId: SlotId) : SlotDetailIntent
    data object Retry : SlotDetailIntent
}

class SlotDetailStore(
    private val slotRepository: SlotRepository,
) : ViewModel(), MviStore<SlotDetailState, SlotDetailIntent, Nothing> {
    private val mutableState = MutableStateFlow(SlotDetailState())
    private val effects = Channel<Nothing>(Channel.BUFFERED)
    override val state: StateFlow<SlotDetailState> = mutableState

    override fun accept(intent: SlotDetailIntent) {
        when (intent) {
            is SlotDetailIntent.Open -> load(intent.slotId)
            SlotDetailIntent.Retry -> mutableState.value.slotId?.let { load(it) }
        }
    }

    override suspend fun effects(): Nothing = effects.receive()

    private fun load(slotId: SlotId) {
        viewModelScope.launch {
            mutableState.update { it.copy(slotId = slotId, slot = Loadable.Loading) }
            slotRepository.getSlot(slotId).fold(
                onSuccess = { detail -> mutableState.update { it.copy(slot = Loadable.Content(detail)) } },
                onFailure = { e -> mutableState.update { it.copy(slot = Loadable.Error(e.asAppFailure())) } },
            )
        }
    }
}
