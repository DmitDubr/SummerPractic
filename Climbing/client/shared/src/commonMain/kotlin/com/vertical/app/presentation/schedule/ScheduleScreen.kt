package com.vertical.app.presentation.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vertical.app.core.ui.Loadable
import com.vertical.app.domain.model.*
import com.vertical.app.domain.policy.QuickDateFilter
import com.vertical.app.domain.policy.QuickDatePreset
import com.vertical.app.domain.policy.SlotAvailabilityPolicy
import com.vertical.app.domain.policy.SlotFilterPolicy
import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    state: ScheduleState,
    onIntent: (ScheduleIntent) -> Unit,
    onSlotClick: (SlotId) -> Unit,
) {
    if (state.filtersVisible) {
        FiltersSheet(state, onIntent)
    }
    if (state.dateFilterVisible) {
        DateFilterSheet(state, onIntent)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Расписание", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        val now = Clock.System.now()
        val activeQuickPreset = QuickDateFilter.activePreset(state.filters, now)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickDatePreset.entries.forEach { preset ->
                FilterChip(
                    selected = activeQuickPreset == preset,
                    onClick = { onIntent(ScheduleIntent.ApplyQuickDatePreset(preset)) },
                    label = { Text(preset.label) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !SlotFilterPolicy.isDefaultDateRange(state.filters),
                onClick = { onIntent(ScheduleIntent.OpenDateFilter) },
                label = { Text(SlotFilterPolicy.dateRangeChipLabel(state.filters)) },
            )
            FilterChip(
                selected = state.filterBadge > 0,
                onClick = { onIntent(ScheduleIntent.OpenFilters) },
                label = { Text(if (state.filterBadge > 0) "Фильтры •${state.filterBadge}" else "Фильтры") },
            )
        }
        Spacer(Modifier.height(12.dp))
        when (val groups = state.groups) {
            is Loadable.Initial, is Loadable.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is Loadable.Error -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(groups.failure.toString())
                Button(onClick = { onIntent(ScheduleIntent.Load) }) { Text("Повторить") }
            }
            is Loadable.Empty -> Text(
                when (groups.reason) {
                    com.vertical.app.core.ui.EmptyReason.NoSlotsByFilters -> "Ничего не найдено"
                    else -> "Пока нет доступных тренировок"
                },
            )
            is Loadable.Content -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (groups.refreshing) {
                    item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                }
                groups.value.forEach { day ->
                    item {
                        Text(day.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    items(day.slots, key = { it.id.value }) { slot ->
                        SlotCard(slot, onClick = { onSlotClick(slot.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotCard(slot: SlotSummary, onClick: () -> Unit) {
    val time = slot.startAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val timeLabel = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
    val noSpots = slot.freeSpots <= 0
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (noSpots) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$timeLabel · ${slot.format}", fontWeight = FontWeight.Medium)
                if (noSpots) {
                    Badge { Text("Мест нет") }
                }
            }
            Text("${slot.instructor.name}${slot.instructor.rating?.let { " ★ $it" } ?: ""}")
            Text(
                SlotAvailabilityPolicy.spotsShortLabel(slot.freeSpots, slot.capacity, slot.price),
                style = MaterialTheme.typography.bodySmall,
                color = if (noSpots) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateFilterSheet(state: ScheduleState, onIntent: (ScheduleIntent) -> Unit) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val canApply = state.draftFilters.dateFrom != null && state.draftFilters.dateTo != null &&
        state.draftFilters.dateFrom!! <= state.draftFilters.dateTo!! &&
        state.draftFilters.dateFrom!! >= today
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = { onIntent(ScheduleIntent.CloseDateFilter) }, sheetState = sheetState) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Период", style = MaterialTheme.typography.titleLarge)
            Text("Быстрый выбор", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.draftFilters.dateFrom == null && state.draftFilters.dateTo == null,
                    onClick = { onIntent(ScheduleIntent.ResetDateFilter) },
                    label = { Text("7 дней") },
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        onIntent(ScheduleIntent.SetDraftDateFrom(today))
                        onIntent(ScheduleIntent.SetDraftDateTo(today.plus(DatePeriod(days = 13))))
                    },
                    label = { Text("14 дней") },
                )
            }
            OutlinedTextField(
                value = state.draftFilters.dateFrom?.toString().orEmpty(),
                onValueChange = { onIntent(ScheduleIntent.SetDraftDateFrom(parseDateOrNull(it))) },
                label = { Text("С (ГГГГ-ММ-ДД)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(today.toString()) },
            )
            OutlinedTextField(
                value = state.draftFilters.dateTo?.toString().orEmpty(),
                onValueChange = { onIntent(ScheduleIntent.SetDraftDateTo(parseDateOrNull(it))) },
                label = { Text("По (ГГГГ-ММ-ДД)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Выберите дату окончания") },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onIntent(ScheduleIntent.ResetDateFilter) }) { Text("Сбросить") }
                Button(
                    onClick = { onIntent(ScheduleIntent.ApplyDateFilter) },
                    enabled = canApply,
                ) { Text("Применить") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun parseDateOrNull(input: String): LocalDate? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    return runCatching { LocalDate.parse(trimmed) }.getOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersSheet(state: ScheduleState, onIntent: (ScheduleIntent) -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = { onIntent(ScheduleIntent.CloseFilters) }, sheetState = sheetState) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Фильтры", style = MaterialTheme.typography.titleLarge)
            Text("Уровень", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrainingLevel.entries.forEach { level ->
                    FilterChip(
                        selected = state.draftFilters.level == level,
                        onClick = {
                            onIntent(
                                ScheduleIntent.SetLevel(
                                    if (state.draftFilters.level == level) null else level,
                                ),
                            )
                        },
                        label = {
                            Text(
                                when (level) {
                                    TrainingLevel.Beginner -> "Начинающий"
                                    TrainingLevel.Intermediate -> "Средний"
                                    TrainingLevel.Advanced -> "Продвинутый"
                                },
                            )
                        },
                    )
                }
            }
            Text("Время суток", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeOfDay.entries.forEach { tod ->
                    FilterChip(
                        selected = state.draftFilters.timeOfDay == tod,
                        onClick = {
                            onIntent(
                                ScheduleIntent.SetTimeOfDay(
                                    if (state.draftFilters.timeOfDay == tod) null else tod,
                                ),
                            )
                        },
                        label = {
                            Text(
                                when (tod) {
                                    TimeOfDay.Morning -> "Утро"
                                    TimeOfDay.Afternoon -> "День"
                                    TimeOfDay.Evening -> "Вечер"
                                },
                            )
                        },
                    )
                }
            }
            when (val instructors = state.instructors) {
                is Loadable.Content -> {
                    Text("Инструктор", style = MaterialTheme.typography.labelLarge)
                    instructors.value.forEach { instructor ->
                        val selected = instructor.id in state.draftFilters.instructorIds
                        FilterChip(
                            selected = selected,
                            onClick = { onIntent(ScheduleIntent.ToggleInstructor(instructor.id)) },
                            label = { Text(instructor.name) },
                        )
                    }
                }
                else -> CircularProgressIndicator()
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onIntent(ScheduleIntent.ResetFilters) }) { Text("Сбросить") }
                Button(onClick = { onIntent(ScheduleIntent.ApplyFilters) }) { Text("Применить") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
