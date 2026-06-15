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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    countrySelected: Boolean,
    stateSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
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
        }

        Spacer(Modifier.height(Spacing.md))

        // Country → State → City type-to-search cascade (master catalog APIs). State appears once a
        // country is picked; city once a state is picked (cities need both ids).
        SearchableFieldWithError(
            value = country,
            options = countryOptions,
            onQueryChange = onCountryQuery,
            onOptionSelected = onCountrySelected,
            label = "Country",
            hasError = countryError,
        )
        if (countrySelected) {
            Spacer(Modifier.height(Spacing.md))
            SearchableFieldWithError(
                value = state,
                options = stateOptions,
                onQueryChange = onStateQuery,
                onOptionSelected = onStateSelected,
                label = "State",
                hasError = stateError,
            )
        }
        if (stateSelected) {
            Spacer(Modifier.height(Spacing.md))
            SearchableFieldWithError(
                value = city,
                options = cityOptions,
                onQueryChange = onCityQuery,
                onOptionSelected = onCitySelected,
                label = "City",
                hasError = cityError,
            )
        }

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
                text = "To provide recommendations based on others your age and connect you with local artists.",
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
                text = "This field should not be empty",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = Spacing.md, top = Spacing.xs),
            )
        }
    }
}
