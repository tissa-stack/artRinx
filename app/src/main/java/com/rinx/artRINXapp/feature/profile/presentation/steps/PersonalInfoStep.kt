package com.rinx.artRINXapp.feature.profile.presentation.steps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.core.ui.DobPickerField
import com.rinx.artRINXapp.core.ui.SearchableTextDropdownField

@Composable
fun PersonalInfoStep(
    dob: String,
    country: String,
    state: String,
    city: String,
    dobError: Boolean,
    countryError: Boolean,
    stateError: Boolean,
    cityError: Boolean,
    showTooltip: Boolean,
    onDobChange: (String) -> Unit,
    onCountryQuery: (String) -> Unit,
    onCountrySelected: (String) -> Unit,
    onStateQuery: (String) -> Unit,
    onStateSelected: (String) -> Unit,
    onCityQuery: (String) -> Unit,
    onCitySelected: (String) -> Unit,
    onTooltipToggle: () -> Unit,
    countryOptions: List<String>,
    stateOptions: List<String>,
    cityOptions: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        // No internal scroll — the parent ProfileCreationScreen owns one scroll for the whole flow.
        modifier = modifier.fillMaxWidth(),
    ) {
        Spacer(Modifier.height(Spacing.xxl))

        Text(
            text = "Personal information",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "This information will remain private.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Spacing.xxxl))

        // ── Date of birth ─────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {
            DobPickerField(
                label = "Date of birth",
                value = dob,
                onDobSelected = onDobChange,
                isError = dobError,
            )
            if (dobError) {
                Text(
                    text = "This field should not be empty",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = Spacing.md, top = Spacing.xs),
                )
            }
            Text(
                text = "You must be at least 18 to use artRinx.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.md, top = Spacing.xs),
            )
        }

        Spacer(Modifier.height(Spacing.md))

        // Country → State → City type-to-search cascade (master catalog APIs). All three stay
        // visible at all times so any prefilled values are always shown; the State/City option
        // lists populate once a country/state is picked (cities need both ids).
        // Country/State/City are all optional. The pickers are catalog-backed: a blank field is
        // skipped, but a typed value must resolve to a real pick (errorText guides the user).
        SearchableFieldWithError(
            value = country,
            options = countryOptions,
            onQueryChange = onCountryQuery,
            onOptionSelected = onCountrySelected,
            label = "Country (optional)",
            hasError = countryError,
            errorText = "Please select from the list",
        )
        Spacer(Modifier.height(Spacing.md))
        SearchableFieldWithError(
            value = state,
            options = stateOptions,
            onQueryChange = onStateQuery,
            onOptionSelected = onStateSelected,
            label = "State (optional)",
            hasError = stateError,
            errorText = "Please select from the list",
        )
        Spacer(Modifier.height(Spacing.md))
        SearchableFieldWithError(
            value = city,
            options = cityOptions,
            onQueryChange = onCityQuery,
            onOptionSelected = onCitySelected,
            label = "City (optional)",
            hasError = cityError,
        )

        Spacer(Modifier.height(Spacing.xl))

        // ── Info link ─────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = "How do we use this information?",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(
                onClick = onTooltipToggle,
                modifier = Modifier.size(Spacing.xl),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_help),
                    contentDescription = "How we use this",
                    tint = if (showTooltip) BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Spacing.lg),
                )
            }
        }
        AnimatedVisibility(
            visible = showTooltip,
            enter = expandVertically(tween(220)) + fadeIn(tween(220)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(180)),
        ) {
            InfoTooltip(
                text = "To provide recommendations and connect you with local artists.",
                onClose = onTooltipToggle,
            )
        }

        Spacer(Modifier.height(Spacing.xxxl))
    }
}

@Composable
private fun SearchableFieldWithError(
    value: String,
    options: List<String>,
    onQueryChange: (String) -> Unit,
    onOptionSelected: (String) -> Unit,
    label: String,
    hasError: Boolean,
    errorText: String = "This field should not be empty",
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SearchableTextDropdownField(
            label = label,
            value = value,
            options = options,
            onQueryChange = onQueryChange,
            onOptionSelected = onOptionSelected,
            modifier = Modifier.fillMaxWidth(),
        )
        if (hasError) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = Spacing.md, top = Spacing.xs),
            )
        }
    }
}
