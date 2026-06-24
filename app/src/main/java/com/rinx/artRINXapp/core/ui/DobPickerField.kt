package com.rinx.artRINXapp.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Minimum age required to use artRinx (must be at least 18). */
private const val MIN_AGE_YEARS = 18

/**
 * A read-only, themed field that opens a Material3 date picker for choosing a date of birth.
 *
 * [value] is the committed ISO `yyyy-MM-dd` string (empty when unset); the field displays it in a
 * friendly form (e.g. `15 Jun 2005`). Picking a date calls [onDobSelected] with the ISO string.
 * Dates that would make the user younger than [MIN_AGE_YEARS] are not selectable, mirroring the
 * server's minimum-age rule. All date math uses a fixed [Locale] + UTC so the calendar day never
 * shifts with the device timezone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DobPickerField(
    label: String,
    value: String,
    onDobSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    val dimens = LocalDimens.current
    var showDialog by remember { mutableStateOf(false) }

    // The field is `enabled = false` so it never grabs focus or shows a keyboard; a transparent
    // overlay captures the tap to open the picker.
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            // Empty value renders the label as a placeholder; non-empty shows the friendly date.
            value = TextFieldValue(isoToDisplay(value)),
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(text = label, style = MaterialTheme.typography.labelMedium) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            isError = isError,
            singleLine = true,
            shape = RoundedCornerShape(dimens.authButtonHeight / 4),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = fieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dimens.textFieldHeight),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true },
        )
    }

    if (showDialog) {
        val maxSelectableMillis = remember { latestAllowedDobUtcMillis() }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = isoToUtcMillis(value) ?: maxSelectableMillis,
            initialDisplayedMonthMillis = isoToUtcMillis(value) ?: maxSelectableMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= maxSelectableMillis
                override fun isSelectableYear(year: Int) = year <= latestAllowedYear()
            },
        )

        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDobSelected(utcMillisToIso(it)) }
                        showDialog = false
                    },
                ) { Text("OK", color = BrandPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ── Date helpers (fixed Locale.US + UTC to keep the calendar day stable) ──────────────────

private fun utcCalendar(): Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US)

private fun isoFormatter() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
    isLenient = false
}

private fun displayFormatter() = SimpleDateFormat("d MMM yyyy", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

private fun isoToUtcMillis(iso: String): Long? {
    if (iso.isBlank()) return null
    return try {
        isoFormatter().parse(iso)?.time
    } catch (_: Exception) {
        null
    }
}

private fun utcMillisToIso(millis: Long): String = isoFormatter().format(millis)

/** ISO `yyyy-MM-dd` → e.g. `15 Jun 2005`; returns "" for blank/unparseable so the label shows. */
private fun isoToDisplay(iso: String): String {
    val millis = isoToUtcMillis(iso) ?: return ""
    return displayFormatter().format(millis)
}

/** Latest DOB (UTC millis) that still satisfies the minimum age: today − [MIN_AGE_YEARS] years. */
private fun latestAllowedDobUtcMillis(): Long = utcCalendar().apply {
    add(Calendar.YEAR, -MIN_AGE_YEARS)
}.timeInMillis

private fun latestAllowedYear(): Int = utcCalendar().get(Calendar.YEAR) - MIN_AGE_YEARS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    // The field is `enabled = false` to make the whole surface tappable; map the disabled colors
    // back to the normal filled-field look so it reads as an active input, not a greyed-out one.
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledTextColor = MaterialTheme.colorScheme.onSurface,
    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledBorderColor = Color.Transparent,
    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
    errorBorderColor = MaterialTheme.colorScheme.error,
    errorTextColor = MaterialTheme.colorScheme.onSurface,
    errorLabelColor = MaterialTheme.colorScheme.error,
)
