package com.rinx.artRINXapp.feature.profile.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * `GET /api/locations/master/{countries|states|cities}` → `data`. The master catalog is the full
 * world list (countries), states/provinces per country, and cities per state (with prefix search).
 * Only the fields the app consumes are modeled; the rest (iso3, continent, currency_code, lat/long,
 * …) are ignored. `phone_code` + `emoji` are kept so the phone-number picker can use the catalog.
 */
data class MasterListDto<T>(
    @SerializedName("items") val items: List<T>? = null,
    @SerializedName("total") val total: Int = 0,
)

data class MasterCountryDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("iso2") val iso2: String? = null,
    @SerializedName("phone_code") val phoneCode: String? = null,
    @SerializedName("emoji") val emoji: String? = null,
)

data class MasterStateDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("state_code") val stateCode: String? = null,
)

data class MasterCityDto(
    @SerializedName("name") val name: String? = null,
)
