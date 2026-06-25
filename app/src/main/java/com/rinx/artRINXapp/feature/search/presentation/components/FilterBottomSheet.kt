package com.rinx.artRINXapp.feature.search.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.core.ui.SearchableDropdownField
import com.rinx.artRINXapp.core.ui.SearchableTextDropdownField
import com.rinx.artRINXapp.feature.profile.domain.model.Medium
import com.rinx.artRINXapp.feature.search.domain.model.SearchFilter

@Composable
fun FilterBottomSheet(
    filter: SearchFilter,
    mediums: List<Medium>,
    isStyleExpanded: Boolean,
    onToggleStyle: () -> Unit,
    onToggleShopArt: () -> Unit,
    onToggleMedium: (Int) -> Unit,
    onReset: () -> Unit,
    onViewResults: () -> Unit,
    onDismiss: () -> Unit,
    countryOptions: List<String> = emptyList(),
    stateOptions: List<String> = emptyList(),
    cityOptions: List<String> = emptyList(),
    onCountrySelected: (String?) -> Unit = {},
    onStateSelected: (String?) -> Unit = {},
    onCityChanged: (String) -> Unit = {},
    onCitySelected: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val divider = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            // Lift content above the keyboard, and dismiss the keyboard on a tap outside the fields.
            .imePadding()
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
    ) {
        // ── Scrollable body ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            HorizontalDivider(color = divider)
            Spacer(Modifier.height(Spacing.xl))

            // ── Shop Art ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Shop Art Only",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = filter.shopArtOnly,
                    onCheckedChange = { onToggleShopArt() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BrandPrimary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
            }

            Spacer(Modifier.height(Spacing.xl))
            HorizontalDivider(color = divider)
            Spacer(Modifier.height(Spacing.xl))

            // ── Medium (accordion, from /api/mediums/) ─────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleStyle() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Medium",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (isStyleExpanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }

            AnimatedVisibility(
                visible = isStyleExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    Spacer(Modifier.height(Spacing.xs))
                    if (mediums.isEmpty()) {
                        Text(
                            text = "No mediums available.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = Spacing.sm),
                        )
                    } else {
                        mediums.forEach { medium ->
                            MediumOptionRow(
                                label = medium.title,
                                checked = medium.id in filter.mediumIds,
                                onClick = { onToggleMedium(medium.id) },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xl))
            HorizontalDivider(color = divider)
            Spacer(Modifier.height(Spacing.xl))

            // ── Location ─────────────────────────────────────────────────
            Text(
                text = "Location",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.md))

            SearchableDropdownField(
                label = "Country",
                value = filter.country.orEmpty(),
                options = countryOptions,
                onValueChange = { onCountrySelected(it) },
            )

            // State is only offered when the chosen country has listed states; city only after a state
            // (the cities endpoint requires both country and state).
            if (!filter.country.isNullOrBlank() && stateOptions.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.md))
                SearchableDropdownField(
                    label = "State",
                    value = filter.state.orEmpty(),
                    options = stateOptions,
                    onValueChange = { onStateSelected(it) },
                )

                if (!filter.state.isNullOrBlank()) {
                    Spacer(Modifier.height(Spacing.md))
                    SearchableTextDropdownField(
                        label = "City",
                        value = filter.city.orEmpty(),
                        options = cityOptions,
                        onQueryChange = { onCityChanged(it) },
                        onOptionSelected = { onCitySelected(it) },
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))
        }

        // ── Pinned buttons ────────────────────────────────────────────────
        HorizontalDivider(color = divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onReset() }
                    .padding(vertical = Spacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(BrandPrimary)
                    .clickable { onViewResults() }
                    .padding(vertical = Spacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "View results",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ── Medium option row ─────────────────────────────────────────────────────────

@Composable
private fun MediumOptionRow(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        FilterCheckbox(checked = checked)
    }
}

// ── Custom checkbox — visible in both light and dark themes ────────────────────

@Composable
private fun FilterCheckbox(checked: Boolean) {
    val checkboxColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (checked) BrandPrimary else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (checked) Color.Transparent else checkboxColor,
                shape = RoundedCornerShape(3.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}