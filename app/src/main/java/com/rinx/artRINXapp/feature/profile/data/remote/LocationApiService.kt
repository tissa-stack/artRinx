package com.rinx.artRINXapp.feature.profile.data.remote

import com.rinx.artRINXapp.feature.home.data.remote.dto.EnvelopeDto
import com.rinx.artRINXapp.feature.profile.data.remote.dto.MasterCityDto
import com.rinx.artRINXapp.feature.profile.data.remote.dto.MasterCountryDto
import com.rinx.artRINXapp.feature.profile.data.remote.dto.MasterListDto
import com.rinx.artRINXapp.feature.profile.data.remote.dto.MasterStateDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** Master location catalog used by the create-profile cascade (country → state → city). */
interface LocationApiService {

    @GET("api/locations/master/countries")
    suspend fun getCountries(): Response<EnvelopeDto<MasterListDto<MasterCountryDto>>>

    @GET("api/locations/master/states")
    suspend fun getStates(
        @Query("country_iso2") countryIso2: String,
    ): Response<EnvelopeDto<MasterListDto<MasterStateDto>>>

    /** Cities scoped to country+state; `q` is an optional case-insensitive prefix filter. */
    @GET("api/locations/master/cities")
    suspend fun getCities(
        @Query("country_iso2") countryIso2: String,
        @Query("state_code") stateCode: String,
        @Query("q") q: String? = null,
        @Query("limit") limit: Int = 200,
    ): Response<EnvelopeDto<MasterListDto<MasterCityDto>>>
}
