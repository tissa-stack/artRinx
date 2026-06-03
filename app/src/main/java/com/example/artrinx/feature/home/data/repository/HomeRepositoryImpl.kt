package com.example.artrinx.feature.home.data.repository

import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.home.data.remote.HomeApiService
import com.example.artrinx.feature.home.data.remote.dto.ArtworkDto
import com.example.artrinx.feature.home.data.remote.dto.BannerDto
import com.example.artrinx.feature.home.data.remote.dto.CurationDto
import com.example.artrinx.feature.home.data.remote.dto.LikeArtworkRequest
import com.example.artrinx.feature.home.domain.model.ArtworkItem
import com.example.artrinx.feature.home.domain.model.BannerItem
import com.example.artrinx.feature.home.domain.model.CurationItem
import com.example.artrinx.feature.home.domain.model.FeedPost
import com.example.artrinx.feature.home.domain.model.HomeFeed
import com.example.artrinx.feature.home.domain.model.ShoppablePost
import com.example.artrinx.feature.home.domain.repository.HomeRepository
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val apiService: HomeApiService,
) : HomeRepository {

    override suspend fun getDiscoverFeed(): ApiResult<HomeFeed> = safeCall {
        val response = apiService.getDiscoverFeed()
        if (response.isSuccessful) {
            val data = response.body()?.data
            val newArt = data?.newArt.orEmpty()
            ApiResult.Success(
                HomeFeed(
                    banners = data?.sponsored.orEmpty().map { it.toBannerItem() },
                    newArt = newArt.map { it.toArtworkItem() },
                    curations = data?.popularCurations.orEmpty().map { it.toCurationItem() },
                    posts = newArt.map { it.toFeedPost() },
                ),
            )
        } else {
            errorFor(response)
        }
    }

    override suspend fun getShopArtworks(page: Int, size: Int): ApiResult<List<ShoppablePost>> = safeCall {
        val response = apiService.getShopArtworks(page, size)
        if (response.isSuccessful) {
            ApiResult.Success(response.body()?.data?.items.orEmpty().map { it.toShoppablePost() })
        } else {
            errorFor(response)
        }
    }

    override suspend fun likeArtwork(artworkId: Int): ApiResult<Unit> = safeCall {
        val response = apiService.likeArtwork(LikeArtworkRequest(artworkId))
        if (response.isSuccessful) ApiResult.Success(Unit) else errorFor(response)
    }

    override suspend fun unlikeArtwork(artworkId: Int): ApiResult<Unit> = safeCall {
        val response = apiService.unlikeArtwork(artworkId)
        if (response.isSuccessful) ApiResult.Success(Unit) else errorFor(response)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    // ── DTO → domain mappers ────────────────────────────────────────────────────

    private fun BannerDto.toBannerItem() = BannerItem(
        id = id?.toString() ?: "",
        imageUrl = imageUrl.orEmpty(),
        title = title.orEmpty(),
        artistName = artist?.let { "by $it" } ?: subtitle.orEmpty(),
        isSponsored = true,
    )

    private fun ArtworkDto.bestImage(): String = imageUrl ?: webpUrl ?: thumbnailUrl ?: ""
    private fun ArtworkDto.artistDisplay(): String = displayName ?: artist?.artistName ?: ""

    private fun ArtworkDto.toArtworkItem() = ArtworkItem(
        id = id?.toString() ?: "",
        imageUrl = bestImage(),
        title = title.orEmpty(),
        artistName = artistDisplay(),
        artistAvatarUrl = profilePictureUrl,
    )

    private fun ArtworkDto.toFeedPost() = FeedPost(
        id = id?.toString() ?: "",
        artistName = artistDisplay(),
        artistHandle = artist?.artistName?.let { "@$it" } ?: "",
        artistRole = profileTypeName ?: "Artist",
        artistAvatarUrl = profilePictureUrl,
        imageUrl = bestImage(),
        title = title.orEmpty(),
        likeCount = likesCount ?: 0,
        isLiked = isLiked ?: false,
    )

    private fun ArtworkDto.toShoppablePost() = ShoppablePost(
        id = id?.toString() ?: "",
        artistName = artistDisplay(),
        artistHandle = artist?.artistName?.let { "@$it" } ?: "",
        artistRole = profileTypeName ?: "Artist",
        artistAvatarUrl = profilePictureUrl,
        imageUrl = bestImage(),
        title = title.orEmpty(),
        medium = medium?.title.orEmpty(),
        description = description.orEmpty(),
        likeCount = likesCount ?: 0,
        isLiked = isLiked ?: false,
        shopUrl = shopLink.orEmpty(),
    )

    private fun CurationDto.toCurationItem() = CurationItem(
        id = id?.toString() ?: "",
        title = title.orEmpty(),
        curatorHandle = author?.username?.let { "@$it" } ?: "",
        curatorName = author?.displayName ?: author?.username ?: "Curator",
        curatorAvatarUrl = author?.profilePicture,
        artworkUrls = artworks?.mapNotNull { it.imageUrl ?: it.thumbnailUrl }?.take(3) ?: emptyList(),
        description = description ?: "A carefully curated collection of remarkable artworks.",
    )
}