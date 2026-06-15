package com.rinx.artRINXapp.feature.search.data.remote

import com.rinx.artRINXapp.feature.home.data.remote.dto.ArtworkDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.EnvelopeDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.PageDto
import com.rinx.artRINXapp.feature.search.data.remote.dto.LocationItemsDataDto
import com.rinx.artRINXapp.feature.search.data.remote.dto.SearchDataDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchApiService {

    /**
     * Global search. `category` is required and selects which result list is populated
     * ("artwork" | "curation" | "user"). `medium_ids` and `has_shop_link` apply to artwork
     * results; `sort_by` is newest | oldest | most_popular.
     */
    @GET("api/search")
    suspend fun search(
        @Query("search") query: String,
        @Query("category") category: String,
        @Query("medium_ids") mediumIds: List<Int>?,
        @Query("has_shop_link") hasShopLink: Boolean?,
        @Query("sort_by") sortBy: String,
        @Query("country") country: String? = null,
        @Query("state") state: String? = null,
        @Query("city") city: String? = null,
    ): Response<EnvelopeDto<SearchDataDto>>

    @GET("api/search/trending-tags")
    suspend fun getTrendingTags(): Response<EnvelopeDto<List<String>>>

    // ── Location filters: only places that contain ≥1 searchable user; ranked by `sort` ──────────

    @GET("api/search/locations/countries")
    suspend fun getCountries(
        @Query("sort") sort: String = "count",
    ): Response<EnvelopeDto<LocationItemsDataDto>>

    @GET("api/search/locations/states")
    suspend fun getStates(
        @Query("country") country: String,
        @Query("sort") sort: String = "count",
    ): Response<EnvelopeDto<LocationItemsDataDto>>

    /** Cities scoped to country+state; `q` is an optional case-insensitive prefix filter. */
    @GET("api/search/locations/cities")
    suspend fun getCities(
        @Query("country") country: String,
        @Query("state") state: String,
        @Query("q") q: String? = null,
        @Query("limit") limit: Int = 200,
        @Query("sort") sort: String = "count",
    ): Response<EnvelopeDto<LocationItemsDataDto>>

    /** Personalized recommendations shown on the idle/empty search screen. */
    @GET("api/artworks/recommended")
    suspend fun getRecommended(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Response<EnvelopeDto<PageDto<ArtworkDto>>>
}