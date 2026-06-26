package com.rinx.artRINXapp.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * An **editable** type-to-search field (companion to the read-only [SearchableDropdownField]). The
 * text field accepts typing — each change calls [onQueryChange] (the caller debounces and fetches
 * matching [options]) — and tapping a suggestion calls [onOptionSelected] with the exact value.
 *
 * Suggestions are rendered **inline** (not in a popup) so they coexist with the soft keyboard inside
 * an IME-padded, scrollable form: a popup window isn't IME-inset-aware and would be clipped behind
 * the keyboard. The inline list is shown only while the field is focused and has options.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchableTextDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onQueryChange: (String) -> Unit,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val dimens = LocalDimens.current
    val focusManager = LocalFocusManager.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var isFocused by remember { mutableStateOf(false) }

    val showSuggestions = isFocused && enabled && options.isNotEmpty()

    // When the suggestions appear, scroll the field + list above the keyboard in the parent scroll.
    LaunchedEffect(showSuggestions) {
        if (showSuggestions) bringIntoViewRequester.bringIntoView()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onQueryChange,
            enabled = enabled,
            label = { Text(text = label, style = MaterialTheme.typography.labelMedium) },
            trailingIcon = {
                // Show a clear (X) once there's text to wipe the search; fall back to the
                // open/closed dropdown chevron when the field is empty.
                if (value.isNotEmpty() && enabled) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { onQueryChange("") },
                    )
                } else {
                    Icon(
                        imageVector = if (showSuggestions) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        // Tapping the up-chevron while the list is open collapses it (clears focus →
                        // suggestions hide). Decorative when closed (tap the field to open).
                        modifier = if (showSuggestions) {
                            Modifier.clickable { focusManager.clearFocus() }
                        } else {
                            Modifier
                        },
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(dimens.authButtonHeight / 4),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = fieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dimens.textFieldHeight)
                .onFocusChanged { isFocused = it.isFocused },
        )

        AnimatedVisibility(visible = showSuggestions) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xs)
                    // Bound the height so a long result set scrolls within the surface instead of
                    // exploding the form (and to satisfy LazyColumn-in-verticalScroll constraints).
                    .heightIn(max = dimens.textFieldHeight * 4)
                    .clip(RoundedCornerShape(Spacing.md))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = Spacing.xs / 4, // hairline ≈ 1dp, derived from a spacing token
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(Spacing.md),
                    )
                    .bringIntoViewRequester(bringIntoViewRequester),
            ) {
                // No value key: option strings (e.g. cities) can legitimately repeat (two distinct
                // cities named "Amaravati"), and keying by the string crashes LazyColumn on the
                // duplicate. Positional keys are correct here — the list is transient and stateless.
                items(items = options) { option ->
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(option)
                                focusManager.clearFocus()
                            }
                            .padding(horizontal = Spacing.md, vertical = Spacing.md),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedBorderColor = BrandPrimary,
    unfocusedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedLabelColor = BrandPrimary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = BrandPrimary,
    focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
)
