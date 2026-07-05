package com.vertical.app.presentation.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vertical.app.core.ui.ActionStatus
import com.vertical.app.core.ui.Loadable
import com.vertical.app.domain.model.EquipmentMode
import com.vertical.app.presentation.components.PhoneTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFormScreen(
    state: BookingFormState,
    onIntent: (BookingFormIntent) -> Unit,
    onBack: () -> Unit,
    onSuccessNavigate: () -> Unit,
) {
    if (state.success != null) {
        AlertDialog(
            onDismissRequest = { onIntent(BookingFormIntent.DismissSuccess); onSuccessNavigate() },
            title = { Text("Запись оформлена") },
            text = { Text("К оплате на месте: ${state.success.totalPrice.toInt()} ₽") },
            confirmButton = {
                TextButton(onClick = { onIntent(BookingFormIntent.DismissSuccess); onSuccessNavigate() }) {
                    Text("OK")
                }
            },
        )
    }

    state.bookingError?.let { err ->
        AlertDialog(
            onDismissRequest = { onIntent(BookingFormIntent.ClearError) },
            title = { Text(err.title) },
            text = { Text(err.message) },
            confirmButton = {
                TextButton(onClick = { onIntent(BookingFormIntent.ClearError) }) { Text("OK") }
            },
            dismissButton = if (err.showWaitlistCta) {
                {
                    TextButton(onClick = { onIntent(BookingFormIntent.GoWaitlist) }) { Text("В лист ожидания") }
                }
            } else {
                null
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Оформление записи") },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } },
            )
        },
    ) { padding ->
        when (val slot = state.slot) {
            is Loadable.Loading, is Loadable.Initial -> Box(Modifier.fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(androidx.compose.ui.Alignment.Center))
            }
            is Loadable.Error -> Text(slot.failure.toString(), Modifier.padding(padding).padding(16.dp))
            is Loadable.Content -> {
                Column(
                    Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(slot.value.summary.format, style = MaterialTheme.typography.titleLarge)
                    Text("Оплата на месте в скалодроме", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { onIntent(BookingFormIntent.NameChanged(it)) },
                        label = { Text("Имя") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.name.isNotBlank() && state.name.trim().length < 2,
                        supportingText = {
                            if (state.name.isNotBlank() && state.name.trim().length < 2) {
                                Text("Минимум 2 символа")
                            }
                        },
                    )
                    PhoneTextField(
                        value = state.phone,
                        onValueChange = { onIntent(BookingFormIntent.PhoneChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Снаряжение", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.equipmentMode == EquipmentMode.Own,
                            onClick = { onIntent(BookingFormIntent.SetEquipmentMode(EquipmentMode.Own)) },
                            label = { Text("Своё") },
                        )
                        FilterChip(
                            selected = state.equipmentMode == EquipmentMode.Rental,
                            onClick = { onIntent(BookingFormIntent.SetEquipmentMode(EquipmentMode.Rental)) },
                            label = { Text("Прокат") },
                        )
                    }
                    if (state.equipmentMode == EquipmentMode.Rental) {
                        val rental = slot.value.rentalAvailability
                        Text(
                            "Доступно: скальники ${rental.shoesAvailable}, страховки ${rental.harnessAvailable}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.rentalShoes,
                                onClick = { onIntent(BookingFormIntent.ToggleShoes) },
                                enabled = rental.shoesAvailable > 0,
                                label = { Text("Скальники") },
                            )
                            FilterChip(
                                selected = state.rentalHarness,
                                onClick = { onIntent(BookingFormIntent.ToggleHarness) },
                                enabled = rental.harnessAvailable > 0,
                                label = { Text("Страховка") },
                            )
                        }
                    }
                    state.totalPreview?.let { Text("Итого: ${it.toInt()} ₽", style = MaterialTheme.typography.titleMedium) }
                    state.validationReason?.let { reason ->
                        Text(
                            reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Button(
                        onClick = { onIntent(BookingFormIntent.Submit) },
                        enabled = state.canSubmit,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.actionStatus == ActionStatus.Submitting) {
                            CircularProgressIndicator(Modifier.size(20.dp))
                        } else {
                            Text("Записаться · ${state.totalPreview?.toInt() ?: "—"} ₽")
                        }
                    }
                }
            }
            else -> Unit
        }
    }
}
