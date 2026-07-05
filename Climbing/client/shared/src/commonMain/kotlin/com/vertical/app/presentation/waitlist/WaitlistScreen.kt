package com.vertical.app.presentation.waitlist

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vertical.app.core.ui.ActionStatus
import com.vertical.app.core.ui.Loadable
import com.vertical.app.presentation.components.PhoneTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaitlistScreen(
    state: WaitlistState,
    onIntent: (WaitlistIntent) -> Unit,
    onBack: () -> Unit,
    onSuccessNavigate: () -> Unit,
) {
    state.success?.let { entry ->
        AlertDialog(
            onDismissRequest = onSuccessNavigate,
            title = { Text("Вы в очереди") },
            text = { Text("Ваша позиция: ${entry.position}. Мы уведомим, когда освободится место.") },
            confirmButton = {
                TextButton(onClick = onSuccessNavigate) { Text("Мои записи") }
            },
        )
    }

    state.error?.let { err ->
        AlertDialog(
            onDismissRequest = { onIntent(WaitlistIntent.ClearError) },
            title = { Text(err.title) },
            text = { Text(err.message) },
            confirmButton = {
                TextButton(onClick = { onIntent(WaitlistIntent.ClearError) }) { Text("OK") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Лист ожидания") },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } },
            )
        },
    ) { padding ->
        when (val slot = state.slot) {
            is Loadable.Loading, is Loadable.Initial -> Box(Modifier.fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            is Loadable.Error -> Text(slot.failure.toString(), Modifier.padding(padding).padding(16.dp))
            is Loadable.Content -> {
                Column(
                    Modifier.padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(slot.value.summary.format, style = MaterialTheme.typography.titleLarge)
                    Text("Мест нет — встаньте в очередь. При освобождении места мы отправим уведомление.")
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { onIntent(WaitlistIntent.NameChanged(it)) },
                        label = { Text("Имя") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PhoneTextField(
                        value = state.phone,
                        onValueChange = { onIntent(WaitlistIntent.PhoneChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { onIntent(WaitlistIntent.Submit) },
                        enabled = state.canSubmit,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.actionStatus == ActionStatus.Submitting) {
                            CircularProgressIndicator(Modifier.size(20.dp))
                        } else {
                            Text("Встать в очередь")
                        }
                    }
                }
            }
            else -> Unit
        }
    }
}
