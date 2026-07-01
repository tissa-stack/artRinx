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
    // When false, the trailing area always shows the dropdown chevron (no clear-X). Used by the
    // profile location pickers, which are dropdown-selection-only. Defaults true (e.g. search filters).
    showClearIcon: Boolean = true,
    // Dropdown-selection ONLY: typing is purely a search over [options]; the committed value is set
    // solely by tapping a suggestion. Stray typed text is reverted to [value] on blur (no manual
    // entry), and an empty search shows a "No results found" row. Off by default (free-text search).
    dropdownOnly: Boolean = false,
) {
    val dimens = LocalDimens.current
    val focusManager = LocalFocusManager.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var isFocused by remember { mutableStateOf(false) }

    // Dropdown-only owns a transient search query; the field displays it while the committed [value]
    // is set only via selection. Non-dropdown mode is fully [value]-driven (unchanged).
    var query by remember { mutableStateOf(value) }
    // Skips the blur-revert for the one blur that a suggestion tap triggers (selection already updated
    // the committed value; reverting to the stale [value] of this frame would drop it).
    var justSelected by remember { mutableStateOf(false) }
    // Re-sync when the committed value changes externally (selection, cascade reset, draft restore).
    LaunchedEffect(value) { if (query != value) query = value }
    val text = if (dropdownOnly) query else value

    val hasOptions = options.isNotEmpty()
    // Show the dropdown while focused. In dropdown-only mode also show it (as a "No results" row) when
    // the user has typed something that matched nothing, so they get feedback instead of a blank.
    val showDropdown = isFocused && enabled && (hasOptions || (dropdownOnly && text.isNotBlank()))

    // When the suggestions appear, scroll the field + list above the keyboard in the parent scroll.
    LaunchedEffect(showDropdown) {
        if (showDropdown) bringIntoViewRequester.bringIntoView()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                if (dropdownOnly) query = it
                onQueryChange(it)
            },
            enabled = enabled,
            label = { Text(text = label, style = MaterialTheme.typography.labelMedium) },
            trailingIcon = {
                // Show a clear (X) once there's text to wipe the search; fall back to the
                // open/closed dropdown chevron when the field is empty (or when the clear-X is
                // disabled, e.g. dropdown-only location pickers).
                if (showClearIcon && text.isNotEmpty() && enabled) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { onQueryChange("") },
                    )
                } else {
                    Icon(
                        imageVector = if (showDropdown) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        // Tapping the up-chevron while the list is open collapses it (clears focus →
                        // suggestions hide). Decorative when closed (tap the field to open).
                        modifier = if (showDropdown) {
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
                .onFocusChanged { focusState ->
                    val lostFocus = isFocused && !focusState.isFocused
                    isFocused = focusState.isFocused
                    // Dropdown-only: discard a half-typed search on blur so no manual value sticks.
                    if (lostFocus && dropdownOnly) {
                        if (justSelected) justSelected = false else query = value
                    }
                },
        )

        AnimatedVisibility(visible = showDropdown) {
            if (hasOptions) {
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
                                    justSelected = true
                                    if (dropdownOnly) query = option
                                    onOptionSelected(option)
                                    focusManager.clearFocus()
                                }
                                .padding(horizontal = Spacing.md, vertical = Spacing.md),
                        )
                    }
                }
            } else {
                // Dropdown-only search with no matches → explicit "not found" feedback.
                Text(
                    text = "No results found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.xs)
                        .clip(RoundedCornerShape(Spacing.md))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = Spacing.xs / 4,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(Spacing.md),
                        )
                        .padding(horizontal = Spacing.md, vertical = Spacing.md),
                )
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
