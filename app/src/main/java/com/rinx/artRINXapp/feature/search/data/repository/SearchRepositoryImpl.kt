package com.rinx.artRINXapp.feature.search.data.repository

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.home.data.remote.dto.ArtworkDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.CurationDto
import com.rinx.artRINXapp.feature.home.domain.model.CurationItem
import com.rinx.artRINXapp.feature.search.data.remote.SearchApiService
import com.rinx.artRINXapp.feature.search.data.remote.dto.SearchUserDto
import com.rinx.artRINXapp.feature.search.domain.model.CardHeight
import com.rinx.artRINXapp.feature.search.domain.model.SearchResultItem
import com.rinx.artRINXapp.feature.search.domain.model.SortOption
import com.rinx.artRINXapp.feature.search.domain.model.UserSearchItem
import com.rinx.artRINXapp.feature.search.domain.model.apiValue
import com.rinx.artRINXapp.feature.search.domain.repository.SearchRepository
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class SearchRepositoryImpl @Inject constructor(
    private val apiService: SearchApiService,
) : SearchRepository {

    // SWR cache for the idle screen — survives navigation (@Singleton); cleared on logout/delete.
    @Volatile private var trendingCache: List<String>? = null
    @Volatile private var recommendedCache: List<SearchResultItem>? = null

    override fun cachedTrendingTags(): List<String>? = trendingCache
    override fun cachedRecommended(): List<SearchResultItem>? = recommendedCache
    override fun clearCache() {
        trendingCache = null
        recommendedCache = null
    }

    override suspend fun searchArtworks(
        query: String,
        mediumIds: List<Int>,
        shopArtOnly: Boolean,
        sortBy: SortOption,
    ): ApiResult<List<SearchResultItem>> = safeCall {
        val response = apiService.search(
            query = query,
            category = "artwork",
            mediumIds = mediumIds.ifEmpty { null },
            hasShopLink = if (shopArtOnly) true else null,
            sortBy = sortBy.apiValue,
        )
        if (response.isSuccessful) {
            ApiResult.Success(response.body()?.data?.artworks.orEmpty().map { it.toResultItem() })
        } else {
            errorFor(response)
        }
    }

    override suspend fun searchCurations(
        query: String,
        mediumIds: List<Int>,
        sortBy: SortOption,
    ): ApiResult<List<CurationItem>> = safeCall {
        val response = apiService.search(
            query = query,
            category = "curation",
            mediumIds = mediumIds.ifEmpty { null },
            hasShopLink = null,
            sortBy = sortBy.apiValue,
        )
        if (response.isSuccessful) {
            ApiResult.Success(response.body()?.data?.curations.orEmpty().map { it.toCurationItem() })
        } else {
            errorFor(response)
        }
    }

    override suspend fun searchUsers(query: String): ApiResult<List<UserSearchItem>> = safeCall {
        val response = apiService.search(
            query = query,
            category = "user",
            mediumIds = null,
            hasShopLink = null,
            sortBy = SortOption.NEWEST.apiValue,
        )
        if (response.isSuccessful) {
            ApiResult.Success(response.body()?.data?.users.orEmpty().map { it.toUserItem() })
        } else {
            errorFor(response)
        }
    }

    override suspend fun getTrendingTags(): ApiResult<List<String>> = safeCall {
        val response = apiService.getTrendingTags()
        if (response.isSuccessful) {
            val tags = response.body()?.data.orEmpty()
            trendingCache = tags
            ApiResult.Success(tags)
        } else {
            errorFor(response)
        }
    }

    override suspend fun getRecommended(): ApiResult<List<SearchResultItem>> = safeCall {
        val response = apiService.getRecommended(page = PAGE, size = SIZE)
        if (response.isSuccessful) {
            val items = response.body()?.data?.items.orEmpty().map { it.toResultItem() }
            recommendedCache = items
            ApiResult.Success(items)
        } else {
            errorFor(response)
        }
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private fun ArtworkDto.toResultItem(): SearchResultItem = SearchResultItem(
        id = id?.toString().orEmpty(),
        imageUrl = imageUrl ?: thumbnailUrl ?: webpUrl.orEmpty(),
        title = title ?: "Untitled",
        artistName = artist?.artistName ?: displayName.orEmpty(),
        cardHeight = aspectRatio.toCardHeight(),
        artId = id?.toString().orEmpty(),
    )

    private fun CurationDto.toCurationItem(): CurationItem {
        val ordered = artworks.orEmpty()
        return CurationItem(
            id = id?.toString().orEmpty(),
            title = title ?: "Untitled",
            curatorHandle = author?.username?.let { "@$it" } ?: "",
            curatorName = author?.displayName ?: author?.username ?: "Curator",
            curatorAvatarUrl = author?.profilePicture,
            artworkUrls = ordered.mapNotNull { it.imageUrl ?: it.thumbnailUrl },
            styles = ordered.mapNotNull { it.medium?.title }.distinct().joinToString(", ").ifBlank { "Mixed" },
            likeCount = likesCount ?: 0,
            isLiked = isLiked ?: false,
        )
    }

    private fun SearchUserDto.toUserItem(): UserSearchItem = UserSearchItem(
        id = (id ?: userId)?.toString().orEmpty(),
        username = username.orEmpty(),
        displayName = displayName ?: username.orEmpty(),
        profileTypeName = profileTypeName.orEmpty(),
        profilePictureUrl = profilePictureUrl,
    )

    /** aspect_ratio is width/height: <1 = portrait (tall), >1 = landscape (short). */
    private fun Double?.toCardHeight(): CardHeight = when {
        this == null -> CardHeight.MEDIUM
        this <= 0.85 -> CardHeight.TALL
        this >= 1.3 -> CardHeight.SHORT
        else -> CardHeight.MEDIUM
    }

    // ── Error handling (mirrors HomeRepositoryImpl) ───────────────────────────

    private inline fun <T> safeCall(block: () -> ApiResult<T>): ApiResult<T> = try {
        block()
    } catch (e: IOException) {
        ApiResult.Error.Network(e)
    } catch (e: Exception) {
        ApiResult.Error.Unknown(e)
    }

    private fun errorFor(response: Response<*>): ApiResult.Error = when (response.code()) {
        in 400..499 -> ApiResult.Error.Validation("Request failed (${response.code()})")
        in 500..599 -> ApiResult.Error.Server(response.code())
        else -> ApiResult.Error.Unknown(RuntimeException("HTTP ${response.code()}"))
    }

    private companion object {
        const val PAGE = 1
        const val SIZE = 20
    }
}