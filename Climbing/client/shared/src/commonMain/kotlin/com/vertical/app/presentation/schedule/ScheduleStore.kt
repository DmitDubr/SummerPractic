package com.vertical.app.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vertical.app.catalog.SlotGroupingPolicy
import com.vertical.app.core.error.AppFailure
import com.vertical.app.core.error.asAppFailure
import com.vertical.app.core.mvi.MviStore
import com.vertical.app.core.ui.EmptyReason
import com.vertical.app.core.ui.Loadable
import com.vertical.app.data.InstructorRepository
import com.vertical.app.data.SlotRepository
import com.vertical.app.domain.model.*
import com.vertical.app.domain.policy.QuickDateFilter
import com.vertical.app.domain.policy.QuickDatePreset
import com.vertical.app.domain.policy.SlotFilterPolicy
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalDate

data class ScheduleState(
    val groups: Loadable<List<DayGroup>> = Loadable.Initial,
    val filters: SlotFilters = SlotFilters(),
    val filterBadge: Int = 0,
    val filtersVisible: Boolean = false,
    val dateFilterVisible: Boolean = false,
    val draftFilters: SlotFilters = SlotFilters(),
    val instructors: Loadable<List<InstructorSummary>> = Loadable.Initial,
)

sealed interface ScheduleIntent {
    data object Load : ScheduleIntent
    data object Refresh : ScheduleIntent
    data object OpenFilters : ScheduleIntent
    data object CloseFilters : ScheduleIntent
    data object OpenDateFilter : ScheduleIntent
    data object CloseDateFilter : ScheduleIntent
    data object ApplyFilters : ScheduleIntent
    data object ApplyDateFilter : ScheduleIntent
    data object ResetFilters : ScheduleIntent
    data object ResetDateFilter : ScheduleIntent
    data class SetDraftDateFrom(val value: LocalDate?) : ScheduleIntent
    data class SetDraftDateTo(val value: LocalDate?) : ScheduleIntent
    data class ToggleInstructor(val id: InstructorId) : ScheduleIntent
    data class SetTimeOfDay(val value: TimeOfDay?) : ScheduleIntent
    data class SetLevel(val value: TrainingLevel?) : ScheduleIntent
    data class ApplyQuickDatePreset(val preset: QuickDatePreset) : ScheduleIntent
}

class ScheduleStore(
    private val slotRepository: SlotRepository,
    private val instructorRepository: InstructorRepository,
) : ViewModel(), MviStore<ScheduleState, ScheduleIntent, Nothing> {
    private val mutableState = MutableStateFlow(ScheduleState())
    private val effects = Channel<Nothing>(Channel.BUFFERED)
    override val state: StateFlow<ScheduleState> = mutableState

    init { accept(ScheduleIntent.Load) }

    override fun accept(intent: ScheduleIntent) {
        when (intent) {
            ScheduleIntent.Load, ScheduleIntent.Refresh -> load(refresh = intent == ScheduleIntent.Refresh)
            ScheduleIntent.OpenFilters -> {
                mutableState.update { it.copy(filtersVisible = true, draftFilters = it.filters) }
                loadInstructors()
            }
            ScheduleIntent.CloseFilters -> mutableState.update { it.copy(filtersVisible = false) }
            ScheduleIntent.OpenDateFilter -> {
                mutableState.update { it.copy(dateFilterVisible = true, draftFilters = it.filters) }
            }
            ScheduleIntent.CloseDateFilter -> mutableState.update { it.copy(dateFilterVisible = false) }
            ScheduleIntent.ApplyFilters -> {
                mutableState.update {
                    it.copy(
                        filters = it.draftFilters,
                        filtersVisible = false,
                        filterBadge = SlotFilterPolicy.filterBadgeCount(it.draftFilters),
                        groups = Loadable.Initial,
                    )
                }
                load(refresh = false)
            }
            ScheduleIntent.ApplyDateFilter -> {
                mutableState.update {
                    it.copy(
                        filters = it.draftFilters,
                        dateFilterVisible = false,
                        filterBadge = SlotFilterPolicy.filterBadgeCount(it.draftFilters),
                        groups = Loadable.Initial,
                    )
                }
                load(refresh = false)
            }
            ScheduleIntent.ResetFilters -> mutableState.update {
                it.copy(draftFilters = it.draftFilters.copy(instructorIds = emptySet(), timeOfDay = null, level = null))
            }
            ScheduleIntent.ResetDateFilter -> {
                val cleared = mutableState.value.filters.copy(dateFrom = null, dateTo = null)
                mutableState.update {
                    it.copy(
                        draftFilters = it.draftFilters.copy(dateFrom = null, dateTo = null),
                        filters = cleared,
                        dateFilterVisible = false,
                        filterBadge = SlotFilterPolicy.filterBadgeCount(cleared),
                        groups = Loadable.Initial,
                    )
                }
                load(refresh = false)
            }
            is ScheduleIntent.ToggleInstructor -> mutableState.update { s ->
                val ids = s.draftFilters.instructorIds.toMutableSet()
                if (intent.id in ids) ids.remove(intent.id) else ids.add(intent.id)
                s.copy(draftFilters = s.draftFilters.copy(instructorIds = ids))
            }
            is ScheduleIntent.SetTimeOfDay -> mutableState.update {
                it.copy(draftFilters = it.draftFilters.copy(timeOfDay = intent.value))
            }
            is ScheduleIntent.SetLevel -> mutableState.update {
                it.copy(draftFilters = it.draftFilters.copy(level = intent.value))
            }
            is ScheduleIntent.SetDraftDateFrom -> mutableState.update {
                it.copy(draftFilters = it.draftFilters.copy(dateFrom = intent.value))
            }
            is ScheduleIntent.SetDraftDateTo -> mutableState.update {
                it.copy(draftFilters = it.draftFilters.copy(dateTo = intent.value))
            }
            is ScheduleIntent.ApplyQuickDatePreset -> {
                val (from, to) = QuickDateFilter.range(intent.preset, Clock.System.now())
                val newFilters = mutableState.value.filters.copy(dateFrom = from, dateTo = to)
                mutableState.update {
                    it.copy(
                        filters = newFilters,
                        draftFilters = newFilters,
                        filterBadge = SlotFilterPolicy.filterBadgeCount(newFilters),
                        groups = Loadable.Initial,
                    )
                }
                load(refresh = false)
            }
        }
    }

    override suspend fun effects(): Nothing = effects.receive()

    private fun load(refresh: Boolean) {
        val current = mutableState.value.groups
        if (!refresh && current is Loadable.Loading) return
        viewModelScope.launch {
            if (refresh && current is Loadable.Content) {
                mutableState.update { it.copy(groups = current.copy(refreshing = true)) }
            } else {
                mutableState.update { it.copy(groups = Loadable.Loading) }
            }
            val filters = mutableState.value.filters
            slotRepository.listSlots(filters).fold(
                onSuccess = { slots ->
                    val grouped = SlotGroupingPolicy.group(slots, Clock.System.now())
                    mutableState.update {
                        it.copy(
                            groups = if (grouped.isEmpty()) {
                                Loadable.Empty(
                                    if (SlotFilterPolicy.filterBadgeCount(filters) > 0 ||
                                        !SlotFilterPolicy.isDefaultDateRange(filters)
                                    ) EmptyReason.NoSlotsByFilters else EmptyReason.NoSlots,
                                )
                            } else {
                                Loadable.Content(grouped)
                            },
                        )
                    }
                },
                onFailure = { e ->
                    mutableState.update {
                        it.copy(groups = Loadable.Error(e.asAppFailure()))
                    }
                },
            )
        }
    }

    private fun loadInstructors() {
        if (mutableState.value.instructors is Loadable.Content) return
        viewModelScope.launch {
            mutableState.update { it.copy(instructors = Loadable.Loading) }
            instructorRepository.listInstructors().fold(
                onSuccess = { list ->
                    mutableState.update { it.copy(instructors = Loadable.Content(list)) }
                },
                onFailure = { e ->
                    mutableState.update { it.copy(instructors = Loadable.Error(e.asAppFailure())) }
                },
            )
        }
    }
}
