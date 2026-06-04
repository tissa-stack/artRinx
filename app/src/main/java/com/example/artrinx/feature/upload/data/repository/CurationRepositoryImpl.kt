package com.example.artrinx.feature.upload.data.repository

import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.home.data.remote.dto.ArtworkDto
import com.example.artrinx.feature.upload.data.remote.CurationApiService
import com.example.artrinx.feature.upload.data.remote.dto.CreateCurationBody
import com.example.artrinx.feature.upload.domain.model.CreateCurationRequest
import com.example.artrinx.feature.upload.domain.model.CreatedCuration
import com.example.artrinx.feature.upload.domain.model.UserArtItem
import com.example.artrinx.feature.upload.domain.repository.CurationRepository
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class CurationRepositoryImpl @Inject constructor(
    private val apiService: CurationApiService,
) : CurationRepository {

    override suspend fun getMyArtworks(page: Int, size: Int): ApiResult<List<UserArtItem>> = safeCall {
        val response = apiService.getMyArtworks(page, size)
        if (response.isSuccessful) {
            ApiResult.Success(response.body()?.data?.items.orEmpty().mapNotNull { it.toUserArtItem() })
        } else {
            errorFor(response)
        }
    }

    override suspend fun getLikedArtworks(page: Int, size: Int): ApiResult<List<UserArtItem>> = safeCall {
        val response = apiService.getLikedArtworks(page, size)
        if (response.isSuccessful) {
            ApiResult.Success(response.body()?.data?.items.orEmpty().mapNotNull { it.toUserArtItem() })
        } else {
            errorFor(response)
        }
    }

    override suspend fun createCuration(request: CreateCurationRequest): ApiResult<CreatedCuration> = safeCall {
        val response = apiService.createCuration(
            CreateCurationBody(
                title = request.title,
                description = request.description,
                privacy = request.isPrivate,
                artworkIds = request.artworkIds,
            ),
        )
        val data = response.body()?.data
        if (response.isSuccessful && data?.id != null) {
            ApiResult.Success(CreatedCuration(id = data.id))
        } else {
            errorFor(response)
        }
    }

    // ── Mappers ────────────────────────────────────────────────────────────────

    /** Drops artworks without an id (can't be referenced in artwork_ids). */
    private fun ArtworkDto.toUserArtItem(): UserArtItem? {
        val artId = id ?: return null
        return UserArtItem(
            id = artId.toString(),
            imageRes = null,
            isSelected = false,
            imageUrl = imageUrl ?: thumbnailUrl ?: webpUrl,
            artworkId = artId,
        )
    }

    // ── Error handling (mirrors HomeRepositoryImpl) ──────────────────────────────

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
}
