package com.rinx.artRINXapp.feature.profile.domain.repository

import com.rinx.artRINXapp.core.network.ApiResult

/** A country from the master catalog; [iso2] is needed to fetch its states. */
data class CountryOption(val name: String, val iso2: String)

/** A state/province from the master catalog; [stateCode] is needed to fetch its cities. */
data class StateOption(val name: String, val stateCode: String)

/**
 * Master location catalog (full world list) backing the create-profile cascade. The UI submits
 * names, but each level needs the parent's id for the next call: states need `country_iso2`, cities
 * need `country_iso2` + `state_code`.
 */
interface MasterLocationRepository {
    suspend fun getCountries(): ApiResult<List<CountryOption>>
    suspend fun getStates(countryIso2: String): ApiResult<List<StateOption>>
    suspend fun getCities(countryIso2: String, stateCode: String, query: String?): ApiResult<List<String>>
}
