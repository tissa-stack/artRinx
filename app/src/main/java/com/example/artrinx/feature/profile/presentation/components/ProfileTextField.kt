package com.example.artrinx.feature.profile.presentation.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.LocalDimens

/**
 * Profile-specific text field.
 *
 * Key differences from WaitlistTextField:
 * - Uses `placeholder` (not `label`) → no floating label, no notch cut into the border.
 * - Unfocused border is fully transparent → only the container surface is visible.
 * - Focused border is BrandPrimary blue → the full outline appears only when active.
 * - Supports an optional `trailingIcon` rendered inside the field.
 *
 * Result: unfocused = clean rounded container + hint text.
 *         focused   = same container + blue outline + cursor.
 */
@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    maxChars: Int = Int.MAX_VALUE,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    hasError: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val dimens = LocalDimens.current

    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= maxChars) onValueChange(it) },
        modifier = modifier.heightIn(min = dimens.textFieldHeight),
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        // NO label → no floating-label animation, no notch in the border stroke
        trailingIcon = trailingIcon,
        isError = hasError,
        singleLine = true,
        shape = RoundedCornerShape(dimens.authButtonHeight / 4),
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() },
        ),
        colors = OutlinedTextFieldDefaults.colors(
            // Container — same surface in both states
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            errorContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            // Border — transparent when idle, brand-blue when focused, red on error
            focusedBorderColor = BrandPrimary,
            unfocusedBorderColor = Color.Transparent,
            errorBorderColor = MaterialTheme.colorScheme.error,
            // Text
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            errorTextColor = MaterialTheme.colorScheme.onSurface,
            // Cursor
            cursorColor = BrandPrimary,
            // Trailing icon tint
            focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            errorTrailingIconColor = MaterialTheme.colorScheme.error,
        ),
    )
}
