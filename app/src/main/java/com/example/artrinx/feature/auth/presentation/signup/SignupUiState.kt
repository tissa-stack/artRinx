package com.example.artrinx.feature.auth.presentation.signup

import android.util.Patterns
import com.example.artrinx.core.navigation.OtpArgs
import com.example.artrinx.feature.auth.domain.model.ContactType
import com.example.artrinx.feature.auth.presentation.waitlist.CountryCode
import com.example.artrinx.feature.auth.presentation.waitlist.CountryCodes

data class SignupUiState(
    val contactType: ContactType = ContactType.EMAIL,
    val email: String = "",
    val rawPhone: String = "",
    val selectedCountry: CountryCode = CountryCodes.default,
    val acceptedTerms: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val navigateToOtp: OtpArgs? = null,
)

val SignupUiState.isContinueEnabled: Boolean
    get() = !isLoading && acceptedTerms && when (contactType) {
        ContactType.EMAIL -> email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
        ContactType.PHONE -> rawPhone.isNotBlank()
    }
