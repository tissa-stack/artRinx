package com.rinx.artRINXapp.feature.auth.presentation.waitlist

import android.util.Patterns
import com.rinx.artRINXapp.core.phone.PhoneValidation
import com.rinx.artRINXapp.feature.auth.domain.model.ProfileType

data class WaitlistUiState(
    val email: String = "",
    val rawPhone: String = "",
    val selectedCountry: CountryCode = CountryCodes.default,
    /** Dial-code options for the picker, supplied offline by libphonenumber. */
    val availableCountries: List<CountryCode> = CountryCodes.all,
    /** libphonenumber length/validity of [rawPhone] for [selectedCountry]. */
    val phoneValidation: PhoneValidation = PhoneValidation.EMPTY,
    /** Max digits typeable for [selectedCountry] (caps the phone input). */
    val phoneMaxDigits: Int = 15,
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
        && phoneValidation == PhoneValidation.OK
        && firstName.isNotBlank()
        && profileType != null
        && acceptedTerms
        && !isLoading
