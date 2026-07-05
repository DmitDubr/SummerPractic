package com.vertical.app.presentation.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vertical.app.core.error.AppFailure
import com.vertical.app.core.error.asAppFailure
import com.vertical.app.core.mvi.MviStore
import com.vertical.app.core.ui.EmptyReason
import com.vertical.app.core.ui.Loadable
import com.vertical.app.data.BookingRepository
import com.vertical.app.domain.model.Booking
import com.vertical.app.domain.model.BookingId
import com.vertical.app.domain.model.BookingSummary
import com.vertical.app.domain.policy.BookingsSegment
import com.vertical.app.domain.policy.BookingsSegmentPolicy
import com.vertical.app.domain.policy.CancelErrorPolicy
import com.vertical.app.domain.policy.CancellationPolicy
import com.vertical.app.session.SessionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookingListState(
    val items: Loadable<List<BookingSummary>> = Loadable.Initial,
    val hasSession: Boolean = false,
    val segment: BookingsSegment = BookingsSegment.Upcoming,
)

sealed interface BookingListIntent {
    data object Load : BookingListIntent
    data object Refresh : BookingListIntent
    data class SelectSegment(val segment: BookingsSegment) : BookingListIntent
}

class BookingListStore(
    private val bookingRepository: BookingRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel(), MviStore<BookingListState, BookingListIntent, Nothing> {
    private val mutableState = MutableStateFlow(BookingListState())
    private val effects = Channel<Nothing>(Channel.BUFFERED)
    private var cachedList: List<BookingSummary> = emptyList()

    override val state: StateFlow<BookingListState> = mutableState

    init { accept(BookingListIntent.Load) }

    override fun accept(intent: BookingListIntent) {
        when (intent) {
            BookingListIntent.Load, BookingListIntent.Refresh -> load()
            is BookingListIntent.SelectSegment -> {
                mutableState.update { it.copy(segment = intent.segment) }
                applySegment(intent.segment)
            }
        }
    }

    override suspend fun effects(): Nothing = effects.receive()

    private fun applySegment(segment: BookingsSegment) {
        val filtered = BookingsSegmentPolicy.filter(cachedList, segment)
        mutableState.update {
            it.copy(
                items = if (filtered.isEmpty()) {
                    Loadable.Empty(
                        if (segment == BookingsSegment.Past) EmptyReason.NoPastBookings
                        else EmptyReason.NoBookings,
                    )
                } else {
                    Loadable.Content(filtered)
                },
            )
        }
    }

    private fun load() {
        viewModelScope.launch {
            val token = sessionRepository.token()
            if (token.isNullOrBlank()) {
                mutableState.update {
                    it.copy(hasSession = false, items = Loadable.Empty(EmptyReason.NoSession))
                }
                return@launch
            }
            mutableState.update { it.copy(hasSession = true, items = Loadable.Loading) }
            bookingRepository.listBookings().fold(
                onSuccess = { list ->
                    cachedList = list
                    applySegment(mutableState.value.segment)
                },
                onFailure = { e ->
                    val failure = e.asAppFailure()
                    if (failure == AppFailure.Unauthorized) {
                        mutableState.update {
                            it.copy(hasSession = false, items = Loadable.Empty(EmptyReason.NoSession))
                        }
                    } else {
                        mutableState.update { it.copy(items = Loadable.Error(failure)) }
                    }
                },
            )
        }
    }
}

data class BookingDetailState(
    val booking: Loadable<Booking> = Loadable.Initial,
    val showCancelConfirm: Boolean = false,
    val cancelWarning: String? = null,
    val cancelling: Boolean = false,
    val cancelError: String? = null,
    val leavingWaitlist: Boolean = false,
)

sealed interface BookingDetailIntent {
    data class Open(val bookingId: BookingId) : BookingDetailIntent
    data class ShowCancel(val warning: String? = null) : BookingDetailIntent
    data object DismissCancel : BookingDetailIntent
    data object ConfirmCancel : BookingDetailIntent
    data object DismissCancelError : BookingDetailIntent
    data object ConfirmLeaveWaitlist : BookingDetailIntent
}

sealed interface BookingDetailEffect {
    data object Cancelled : BookingDetailEffect
    data object LeftWaitlist : BookingDetailEffect
}

class BookingDetailStore(
    private val bookingRepository: BookingRepository,
) : ViewModel(), MviStore<BookingDetailState, BookingDetailIntent, BookingDetailEffect> {
    private val mutableState = MutableStateFlow(BookingDetailState())
    private val effects = Channel<BookingDetailEffect>(Channel.BUFFERED)
    override val state: StateFlow<BookingDetailState> = mutableState

    override fun accept(intent: BookingDetailIntent) {
        when (intent) {
            is BookingDetailIntent.Open -> load(intent.bookingId)
            is BookingDetailIntent.ShowCancel -> mutableState.update {
                it.copy(showCancelConfirm = true, cancelWarning = intent.warning, cancelError = null)
            }
            BookingDetailIntent.DismissCancel -> mutableState.update { it.copy(showCancelConfirm = false) }
            BookingDetailIntent.ConfirmCancel -> cancel()
            BookingDetailIntent.DismissCancelError -> mutableState.update { it.copy(cancelError = null) }
            BookingDetailIntent.ConfirmLeaveWaitlist -> leaveWaitlist()
        }
    }

    override suspend fun effects(): BookingDetailEffect = effects.receive()

    private fun load(bookingId: BookingId) {
        viewModelScope.launch {
            mutableState.update { it.copy(booking = Loadable.Loading) }
            bookingRepository.getBooking(bookingId).fold(
                onSuccess = { booking -> mutableState.update { it.copy(booking = Loadable.Content(booking)) } },
                onFailure = { e -> mutableState.update { it.copy(booking = Loadable.Error(e.asAppFailure())) } },
            )
        }
    }

    private fun cancel() {
        val booking = (mutableState.value.booking as? Loadable.Content)?.value ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(cancelling = true, cancelError = null) }
            bookingRepository.cancelBooking(booking.id).fold(
                onSuccess = {
                    mutableState.update { it.copy(showCancelConfirm = false, cancelling = false) }
                    effects.send(BookingDetailEffect.Cancelled)
                },
                onFailure = { e ->
                    mutableState.update {
                        it.copy(
                            cancelling = false,
                            showCancelConfirm = false,
                            cancelError = CancelErrorPolicy.message(e.asAppFailure()),
                        )
                    }
                },
            )
        }
    }

    private fun leaveWaitlist() {
        val booking = (mutableState.value.booking as? Loadable.Content)?.value ?: return
        if (!CancellationPolicy.canLeaveWaitlist(booking.status)) return
        viewModelScope.launch {
            mutableState.update { it.copy(leavingWaitlist = true) }
            bookingRepository.leaveWaitlist(booking.id).fold(
                onSuccess = {
                    mutableState.update { it.copy(leavingWaitlist = false) }
                    effects.send(BookingDetailEffect.LeftWaitlist)
                },
                onFailure = { e ->
                    mutableState.update {
                        it.copy(
                            leavingWaitlist = false,
                            cancelError = CancelErrorPolicy.message(e.asAppFailure()),
                        )
                    }
                },
            )
        }
    }
}
