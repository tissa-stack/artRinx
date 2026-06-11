package com.rinx.artRINXapp.feature.search.data.remote

import com.rinx.artRINXapp.feature.home.data.remote.dto.ArtworkDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.EnvelopeDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.PageDto
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

    /** Personalized recommendations shown on the idle/empty search screen. */
    @GET("api/artworks/recommended")
    suspend fun getRecommended(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Response<EnvelopeDto<PageDto<ArtworkDto>>>
}