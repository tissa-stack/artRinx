package com.rinx.artRINXapp.feature.auth.presentation.waitlist

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.profile.domain.repository.CountryOption
import com.rinx.artRINXapp.feature.profile.domain.repository.MasterLocationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of dial-code options for every phone-code picker in the app (waitlist, login,
 * signup, change-phone). Loads the full master country catalog
 * (`GET /api/locations/master/countries`) and maps each country to a [CountryCode] — flag emoji,
 * ISO2, "+"-prefixed dial code, and name. Countries without a phone code are dropped.
 *
 * Falls back to the bundled [CountryCodes.all] when the catalog is unavailable (offline / error /
 * empty) so a picker is always usable. A successful mapped list is memoised for the app session;
 * the fallback is never cached, so a later screen retries until the catalog loads.
 */
@Singleton
class CountryCodeProvider @Inject constructor(
    private val masterLocationRepository: MasterLocationRepository,
) {
    @Volatile private var cache: List<CountryCode>? = null

    suspend fun load(): List<CountryCode> {
        cache?.let { return it }
        val result = masterLocationRepository.getCountries()
        val mapped = (result as? ApiResult.Success)?.data?.mapNotNull { it.toCountryCode() }.orEmpty()
        if (mapped.isNotEmpty()) {
            cache = mapped
            return mapped
        }
        return CountryCodes.all
    }

    private fun CountryOption.toCountryCode(): CountryCode? {
        val dial = phoneCode?.takeIf { it.isNotBlank() } ?: return null
        return CountryCode(flag = emoji.orEmpty(), code = iso2, dialCode = dial, name = name)
    }
}
