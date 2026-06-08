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
    ): ApiResult<List<SearchResultItem>>

    suspend fun searchCurations(
        query: String,
        mediumIds: List<Int>,
        sortBy: SortOption,
    ): ApiResult<List<CurationItem>>

    suspend fun searchUsers(query: String): ApiResult<List<UserSearchItem>>

    suspend fun getTrendingTags(): ApiResult<List<String>>

    suspend fun getRecommended(): ApiResult<List<SearchResultItem>>
}