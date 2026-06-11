package com.rinx.artRINXapp.feature.search.domain.repository

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.home.domain.model.CurationItem
import com.rinx.artRINXapp.feature.search.domain.model.SearchResultItem
import com.rinx.artRINXapp.feature.search.domain.model.SortOption
import com.rinx.artRINXapp.feature.search.domain.model.UserSearchItem

interface SearchRepository {
    suspend fun searchArtworks(
        query: String,
        mediumIds: List<Int>,
        shopArtOnly: Boolean,
        sortBy: SortOption,
        country: String? = null,
        state: String? = null,
        city: String? = null,
    ): ApiResult<List<SearchResultItem>>

    suspend fun searchCurations(
        query: String,
        mediumIds: List<Int>,
        sortBy: SortOption,
        country: String? = null,
        state: String? = null,
        city: String? = null,
    ): ApiResult<List<CurationItem>>

    suspend fun searchUsers(
        query: String,
        country: String? = null,
        state: String? = null,
        city: String? = null,
    ): ApiResult<List<UserSearchItem>>

    suspend fun getTrendingTags(): ApiResult<List<String>>

    suspend fun getRecommended(): ApiResult<List<SearchResultItem>>

    // ── In-memory SWR cache for the idle screen (cleared on logout/delete) ───
    fun cachedTrendingTags(): List<String>?
    fun cachedRecommended(): List<SearchResultItem>?
    fun clearCache()
}