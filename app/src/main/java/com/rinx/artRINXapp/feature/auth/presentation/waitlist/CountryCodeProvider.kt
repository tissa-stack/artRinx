package com.rinx.artRINXapp.feature.auth.presentation.waitlist

import com.rinx.artRINXapp.core.phone.PhoneNumberValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of dial-code options for every phone-code picker in the app (waitlist, login, signup,
 * add-phone, change-phone). Backed by Google libphonenumber (via [PhoneNumberValidator]): the full set
 * of supported regions, each mapped to a [CountryCode] — flag emoji, ISO2, "+"-prefixed dial code, and
 * localized name. This is fully offline and complete, so there's no network fetch or bundled fallback.
 */
@Singleton
class CountryCodeProvider @Inject constructor(
    private val phoneNumberValidator: PhoneNumberValidator,
) {
    // Build off the main thread — the first libphonenumber call (metadata load + ~250-region map) is
    // CPU-bound; the result is memoised inside PhoneNumberValidator for the session.
    suspend fun load(): List<CountryCode> = withContext(Dispatchers.Default) {
        phoneNumberValidator.countries()
    }
}
