package com.rinx.artRINXapp.feature.auth.presentation.signup

import android.util.Patterns
import com.rinx.artRINXapp.core.navigation.OtpArgs
import com.rinx.artRINXapp.core.phone.PhoneValidation
import com.rinx.artRINXapp.feature.auth.domain.model.ContactType
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.CountryCode
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.CountryCodes

data class SignupUiState(
    val contactType: ContactType = ContactType.EMAIL,
    val email: String = "",
    val rawPhone: String = "",
    val selectedCountry: CountryCode = CountryCodes.default,
    /** Dial-code options; bundled fallback list, replaced by the master catalog once it loads. */
    val availableCountries: List<CountryCode> = CountryCodes.all,
    /** libphonenumber length/validity of [rawPhone] for [selectedCountry]. */
    val phoneValidation: PhoneValidation = PhoneValidation.EMPTY,
    /** Max digits typeable for [selectedCountry] (caps the phone input). */
    val phoneMaxDigits: Int = 15,
    val acceptedTerms: Boolean = false,
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val errorMessage: String? = null,
    val navigateToOtp: OtpArgs? = null,
    val navigateToHome: Boolean = false,
    val navigateToProfileCompletion: Boolean = false,
)

val SignupUiState.isContinueEnabled: Boolean
    get() = !isLoading && !isGoogleLoading && acceptedTerms && when (contactType) {
        ContactType.EMAIL -> email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
        ContactType.PHONE -> phoneValidation == PhoneValidation.OK
    }
