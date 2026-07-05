package com.vertical.app.presentation.bookings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vertical.app.core.error.userMessage
import com.vertical.app.core.ui.EmptyReason
import com.vertical.app.core.ui.Loadable
import com.vertical.app.domain.model.BookingId
import com.vertical.app.domain.model.BookingStatus
import com.vertical.app.domain.policy.BookingStatusLabels
import com.vertical.app.domain.policy.BookingsSegment
import com.vertical.app.domain.policy.CancellationPolicy
import com.vertical.app.domain.policy.EquipmentLabels
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingListScreen(
    state: BookingListState,
    onIntent: (BookingListIntent) -> Unit,
    onBookingClick: (BookingId) -> Unit,
    onGoSchedule: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Мои записи", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        if (state.hasSession) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                BookingsSegment.entries.forEachIndexed { index, segment ->
                    SegmentedButton(
                        selected = state.segment == segment,
                        onClick = { onIntent(BookingListIntent.SelectSegment(segment)) },
                        shape = SegmentedButtonDefaults.itemShape(index, BookingsSegment.entries.size),
                    ) {
                        Text(
                            when (segment) {
                                BookingsSegment.Upcoming -> "Предстоящие"
                                BookingsSegment.Past -> "Прошедшие"
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        when (val items = state.items) {
            is Loadable.Loading, is Loadable.Initial -> CircularProgressIndicator()
            is Loadable.Empty -> Column {
                Text(
                    when (items.reason) {
                        EmptyReason.NoSession ->
                            "Запишитесь на тренировку — профиль сохранится автоматически"
                        EmptyReason.NoPastBookings -> "Нет прошедших записей"
                        else -> "У вас пока нет записей"
                    },
                )
                if (!state.hasSession) {
                    Button(onClick = onGoSchedule) { Text("К расписанию") }
                }
            }
            is Loadable.Error -> {
                Text(items.failure.userMessage())
                Button(onClick = { onIntent(BookingListIntent.Refresh) }) { Text("Повторить") }
            }
            is Loadable.Content -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items.value, key = { it.id.value }) { booking ->
                    Card(Modifier.fillMaxWidth().clickable { onBookingClick(booking.id) }) {
                        Column(Modifier.padding(12.dp)) {
                            val dt = booking.slotStartAt.toLocalDateTime(TimeZone.currentSystemDefault())
                            Text("${dt.dayOfMonth}.${dt.monthNumber} ${dt.hour}:${dt.minute.toString().padStart(2, '0')}")
                            Text("${booking.formatName} · ${booking.instructorName}")
                            val statusLabel = when {
                                booking.status == BookingStatus.Waitlist && booking.waitlistPosition != null ->
                                    "В очереди · место ${booking.waitlistPosition}"
                                else -> BookingStatusLabels.label(booking.status)
                            }
                            Text("${booking.totalPrice.toInt()} ₽ · $statusLabel")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    state: BookingDetailState,
    onIntent: (BookingDetailIntent) -> Unit,
    onBack: () -> Unit,
    onGoSchedule: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (state.showCancelConfirm) {
        ModalBottomSheet(
            onDismissRequest = { onIntent(BookingDetailIntent.DismissCancel) },
            sheetState = sheetState,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Отменить запись?", style = MaterialTheme.typography.titleLarge)
                Text(
                    state.cancelWarning ?: "Место будет освобождено для других участников.",
                )
                Button(
                    onClick = { onIntent(BookingDetailIntent.ConfirmCancel) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.cancelling,
                ) {
                    Text(if (state.cancelling) "Отмена…" else "Отменить запись")
                }
                OutlinedButton(
                    onClick = { onIntent(BookingDetailIntent.DismissCancel) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Оставить запись") }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали записи") },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } },
            )
        },
        snackbarHost = {
            state.cancelError?.let { message ->
                Snackbar(
                    action = {
                        TextButton(onClick = { onIntent(BookingDetailIntent.DismissCancelError) }) {
                            Text("OK")
                        }
                    },
                ) { Text(message) }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).padding(16.dp)) {
            when (val booking = state.booking) {
                is Loadable.Loading -> CircularProgressIndicator()
                is Loadable.Error -> Text(booking.failure.userMessage())
                is Loadable.Content -> {
                    val b = booking.value
                    val now = Clock.System.now()
                    val canCancel = CancellationPolicy.canCancel(b.status, b.slotStartsAt, now)
                    val canLeave = CancellationPolicy.canLeaveWaitlist(b.status)
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(b.formatName.orEmpty(), style = MaterialTheme.typography.titleLarge)
                        Text(b.instructorName.orEmpty())
                        b.zoneName?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        b.slotStartsAt?.let { starts ->
                            Text(formatTimeRange(starts, b.slotEndsAt))
                        }
                        Text("Статус: ${BookingStatusLabels.label(b.status)}")
                        if (b.status == BookingStatus.Waitlist && b.waitlistPosition != null) {
                            Text("Место в очереди: ${b.waitlistPosition}")
                        }
                        Text("Снаряжение: ${EquipmentLabels.label(b.equipment)}")
                        b.gym?.takeIf { it.address.isNotBlank() }?.let { gym ->
                            Text("Адрес: ${gym.address}")
                        }
                        Text("Сумма: ${b.totalPrice.toInt()} ₽")
                        if (b.status == BookingStatus.CancelledByGym && !b.cancellationReason.isNullOrBlank()) {
                            Text(
                                "Причина отмены: ${b.cancellationReason}",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        if (canLeave) {
                            OutlinedButton(
                                onClick = { onIntent(BookingDetailIntent.ConfirmLeaveWaitlist) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.leavingWaitlist,
                            ) {
                                Text(if (state.leavingWaitlist) "Выход…" else "Покинуть очередь")
                            }
                        }
                        if (canCancel) {
                            OutlinedButton(
                                onClick = {
                                    val warning = b.slotStartsAt?.let { starts ->
                                        if (CancellationPolicy.isLateCancel(starts, now)) {
                                            "До начала меньше часа. Место может не успеть освободиться."
                                        } else null
                                    }
                                    onIntent(BookingDetailIntent.ShowCancel(warning))
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Отменить запись") }
                        }
                        if (b.status == BookingStatus.CancelledByGym || b.status == BookingStatus.CancelledByClient) {
                            Button(
                                onClick = onGoSchedule,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Выбрать другую тренировку") }
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}

private fun formatTimeRange(start: kotlinx.datetime.Instant, end: kotlinx.datetime.Instant?): String {
    val tz = TimeZone.currentSystemDefault()
    val s = start.toLocalDateTime(tz)
    val startStr = "${s.hour}:${s.minute.toString().padStart(2, '0')}"
    if (end == null) return startStr
    val e = end.toLocalDateTime(tz)
    return "$startStr – ${e.hour}:${e.minute.toString().padStart(2, '0')}"
}
