package com.rinx.artRINXapp.feature.auth.presentation.invite.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens

@Composable
fun InviteCodeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hasError: Boolean,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
) {
    val dimens = LocalDimens.current

    OutlinedTextField(
        value = value,
        // Invite codes are case-insensitive but displayed/submitted upper-case — force every
        // keystroke (and pasted text) to upper case here so casing is handled entirely in the field.
        onValueChange = { onValueChange(it.uppercase()) },
        modifier = modifier.heightIn(min = dimens.textFieldHeight),
        label = {
            Text(
                text = "Enter invite code",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        isError = hasError,
        singleLine = true,
        shape = RoundedCornerShape(dimens.authButtonHeight / 4),
        textStyle = MaterialTheme.typography.bodyMedium,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            errorContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            focusedBorderColor = BrandPrimary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
            errorBorderColor = MaterialTheme.colorScheme.error,
            cursorColor = BrandPrimary,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            errorTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = BrandPrimary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            errorLabelColor = MaterialTheme.colorScheme.error,
        ),
    )
}
