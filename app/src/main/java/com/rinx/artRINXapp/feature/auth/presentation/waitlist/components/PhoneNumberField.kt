package com.rinx.artRINXapp.feature.auth.presentation.waitlist.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.CountryCode
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.CountryCodes

@Composable
fun PhoneNumberField(
    rawPhone: String,
    onPhoneChange: (String) -> Unit,
    selectedCountry: CountryCode,
    onCountryChange: (CountryCode) -> Unit,
    modifier: Modifier = Modifier,
    /** Dial-code options. Defaults to the bundled short list; the waitlist passes the master catalog. */
    countries: List<CountryCode> = CountryCodes.all,
    /** When true, the compact trigger opens a searchable bottom sheet instead of a plain dropdown —
     *  needed for the full ~250-country catalog. */
    searchable: Boolean = false,
) {
    val dimens = LocalDimens.current
    var expanded by remember { mutableStateOf(false) }
    var sheetOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
            Surface(
                modifier = Modifier
                    .heightIn(min = dimens.textFieldHeight)
                    .clickable { if (searchable) sheetOpen = true else expanded = true },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(dimens.authButtonHeight / 4),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = Spacing.md),
                ) {
                    Text(
                        text = "${selectedCountry.flag} ${selectedCountry.dialCode}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Compact dropdown — fine for the short bundled list.
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                countries.forEach { country ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${country.flag} ${country.code} ${country.dialCode}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = {
                            onCountryChange(country)
                            expanded = false
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        WaitlistTextField(
            value = rawPhone,
            onValueChange = { onPhoneChange(it.filter { c -> c.isDigit() }.take(15)) },
            label = "Mobile Number",
            modifier = Modifier.weight(1f),
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next,
        )
    }

    if (searchable && sheetOpen) {
        CountryCodePickerSheet(
            countries = countries,
            onSelect = {
                onCountryChange(it)
                sheetOpen = false
            },
            onDismiss = { sheetOpen = false },
        )
    }
}

/** Bottom sheet with a search box + scrollable list, for choosing a dial code from the full catalog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryCodePickerSheet(
    countries: List<CountryCode>,
    onSelect: (CountryCode) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

    val filtered = remember(query, countries) {
        val q = query.trim()
        if (q.isBlank()) {
            countries
        } else {
            countries.filter {
                it.name.contains(q, ignoreCase = true) ||
                    it.code.contains(q, ignoreCase = true) ||
                    it.dialCode.contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        WaitlistTextField(
            value = query,
            onValueChange = { query = it },
            label = "Search country",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search,
        )
        Spacer(Modifier.width(Spacing.sm))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .padding(top = Spacing.sm),
        ) {
            items(filtered, key = { it.code }) { country ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(country) }
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${country.flag} ${country.name.ifBlank { country.code }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = country.dialCode,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
