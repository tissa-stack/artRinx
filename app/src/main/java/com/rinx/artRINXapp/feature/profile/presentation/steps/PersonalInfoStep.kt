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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.profile.presentation.components.ProfileTextField

private val AGE_RANGES = listOf("Under 18", "18-25", "26-35", "36-45", "46-55", "56-65", "65+")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInfoStep(
    age: String,
    country: String,
    state: String,
    city: String,
    ageError: Boolean,
    countryError: Boolean,
    stateError: Boolean,
    cityError: Boolean,
    showTooltip: Boolean,
    onAgeChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onStateChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onTooltipToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current
    var ageExpanded by remember { mutableStateOf(false) }

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

        // ── Age Dropdown ──────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {
            ExposedDropdownMenuBox(
                expanded = ageExpanded,
                onExpandedChange = { ageExpanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Uses placeholder style (no floating label / no notch)
                OutlinedTextField(
                    value = age,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = {
                        Text(
                            text = "Age",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = if (ageExpanded) Icons.Filled.ArrowDropUp
                            else Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = if (ageError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    isError = ageError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = dimens.textFieldHeight)
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(dimens.authButtonHeight / 4),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        errorContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = Color.Transparent,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        errorTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                ExposedDropdownMenu(
                    expanded = ageExpanded,
                    onDismissRequest = { ageExpanded = false },
                    modifier = Modifier.exposedDropdownSize(),
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    AGE_RANGES.forEach { range ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = range,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            onClick = {
                                onAgeChange(range)
                                ageExpanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }
            }
            if (ageError) {
                Text(
                    text = "This field should not be empty",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = Spacing.md, top = Spacing.xs),
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))

        FieldWithError(value = country, onValueChange = onCountryChange, label = "Country", hasError = countryError)
        Spacer(Modifier.height(Spacing.md))
        FieldWithError(value = state, onValueChange = onStateChange, label = "State", hasError = stateError)
        Spacer(Modifier.height(Spacing.md))
        FieldWithError(value = city, onValueChange = onCityChange, label = "City", hasError = cityError)

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
private fun FieldWithError(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hasError: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ProfileTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = label,
            modifier = Modifier.fillMaxWidth(),
            hasError = hasError,
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
