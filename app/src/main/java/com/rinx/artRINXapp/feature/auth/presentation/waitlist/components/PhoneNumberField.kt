package com.rinx.artRINXapp.feature.auth.presentation.waitlist.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
) {
    val dimens = LocalDimens.current
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
            Surface(
                modifier = Modifier
                    .heightIn(min = dimens.textFieldHeight)
                    .clickable { expanded = true },
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

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                CountryCodes.all.forEach { country ->
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
}
