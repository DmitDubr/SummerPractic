package com.vertical.app.presentation.slot

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vertical.app.core.ui.Loadable
import com.vertical.app.domain.policy.SlotCta
import com.vertical.app.domain.model.SlotId
import com.vertical.app.domain.policy.SlotAvailabilityPolicy
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotDetailScreen(
    state: SlotDetailState,
    onIntent: (SlotDetailIntent) -> Unit,
    onBack: () -> Unit,
    onBook: (SlotId) -> Unit,
    onWaitlist: (SlotId) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали слота") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← Назад") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (val slot = state.slot) {
                is Loadable.Loading, is Loadable.Initial -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is Loadable.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(slot.failure.toString())
                    Button(onClick = { onIntent(SlotDetailIntent.Retry) }) { Text("Повторить") }
                }
                is Loadable.Content -> {
                    val detail = slot.value
                    val summary = detail.summary
                    val cta = SlotAvailabilityPolicy.cta(summary)
                    val disabledReason = SlotAvailabilityPolicy.disabledReason(
                        summary,
                        detail.rentalAvailability.isBookable,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(summary.format, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("${summary.zone} · ${detail.gym.name}")
                        Text(
                            SlotAvailabilityPolicy.spotsLabel(summary.freeSpots, summary.capacity),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (summary.freeSpots <= 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        if (summary.freeSpots <= 0) {
                            Text(
                                "Можно встать в лист ожидания — уведомим, когда освободится место",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            "Прокат: скальники ${detail.rentalAvailability.shoesAvailable}, " +
                                "страховки ${detail.rentalAvailability.harnessAvailable}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text("${detail.durationMinutes} мин · ${detail.priceBreakdown.trainingPrice.toInt()} ₽")
                        if (detail.gym.address.isNotBlank()) {
                            Text(detail.gym.address, style = MaterialTheme.typography.bodySmall)
                            TextButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(detail.gym.address))
                                    scope.launch { snackbarHostState.showSnackbar("Адрес скопирован") }
                                },
                            ) { Text("Скопировать адрес") }
                        }
                        disabledReason?.let { reason ->
                            Text(reason, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.height(16.dp))
                        when (cta) {
                            SlotCta.Book -> Button(
                                onClick = { onBook(summary.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Записаться") }
                            SlotCta.Waitlist -> Button(
                                onClick = { onWaitlist(summary.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("В лист ожидания") }
                            SlotCta.Disabled -> Text(
                                disabledReason ?: "Запись недоступна",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}
