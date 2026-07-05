package com.vertical.app.presentation.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.vertical.app.domain.policy.PhoneMaskFormatter
import com.vertical.app.domain.policy.PhoneValidator

@Composable
fun PhoneTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Телефон",
    enabled: Boolean = true,
) {
    val normalized = PhoneValidator.normalize(value)
    val isError = value.isNotBlank() && !PhoneValidator.isValid(normalized)

    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValueChange(PhoneMaskFormatter.format(raw)) },
        label = { Text(label) },
        placeholder = { Text("+7 (900) 123-45-67") },
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        supportingText = {
            Text(if (isError) "Введите номер полностью" else "Формат: +7 (900) 123-45-67")
        },
    )
}
