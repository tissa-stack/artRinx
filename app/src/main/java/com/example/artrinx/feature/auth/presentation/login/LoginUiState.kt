package com.example.artrinx.feature.auth.presentation.login

import android.util.Patterns
import com.example.artrinx.core.navigation.OtpArgs
import com.example.artrinx.feature.auth.domain.model.ContactType
import com.example.artrinx.feature.auth.presentation.waitlist.CountryCode
import com.example.artrinx.feature.auth.presentation.waitlist.CountryCodes

data class LoginUiState(
    val contactType: ContactType = ContactType.EMAIL,
    val email: String = "",
    val rawPhone: String = "",
    val selectedCountry: CountryCode = CountryCodes.default,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val navigateToOtp: OtpArgs? = null,
)

val LoginUiState.isContinueEnabled: Boolean
    get() = !isLoading && when (contactType) {
        ContactType.EMAIL -> email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
        ContactType.PHONE -> rawPhone.isNotBlank()
    }
