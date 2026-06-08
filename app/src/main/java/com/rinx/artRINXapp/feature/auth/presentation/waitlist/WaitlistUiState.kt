package com.rinx.artRINXapp.feature.auth.presentation.waitlist

import android.util.Patterns
import com.rinx.artRINXapp.feature.auth.domain.model.ProfileType

data class WaitlistUiState(
    val email: String = "",
    val rawPhone: String = "",
    val selectedCountry: CountryCode = CountryCodes.default,
    val firstName: String = "",
    val profileType: ProfileType? = null,
    val instagramHandle: String = "",
    val referralCode: String = "",
    val acceptedTerms: Boolean = false,
    val smsOptIn: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
)

val WaitlistUiState.isJoinEnabled: Boolean
    get() = email.isNotBlank()
        && Patterns.EMAIL_ADDRESS.matcher(email).matches()
        && rawPhone.isNotBlank()
        && firstName.isNotBlank()
        && profileType != null
        && acceptedTerms
        && !isLoading
