package com.rinx.artRINXapp.feature.upload.data.repository

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.network.serverMessageOrNull
import com.rinx.artRINXapp.core.util.ProfileRefreshBus
import com.rinx.artRINXapp.feature.home.data.remote.dto.ArtworkDto
import com.rinx.artRINXapp.feature.upload.data.remote.CurationApiService
import com.rinx.artRINXapp.feature.upload.data.remote.dto.CreateCurationBody
import com.rinx.artRINXapp.feature.upload.data.remote.dto.UpdateCurationBody
import com.rinx.artRINXapp.feature.upload.domain.model.CreateCurationRequest
import com.rinx.artRINXapp.feature.upload.domain.model.CreatedCuration
import com.rinx.artRINXapp.feature.upload.domain.model.EditableCuration
import com.rinx.artRINXapp.feature.upload.domain.model.UserArtItem
import com.rinx.artRINXapp.feature.upload.domain.repository.CurationRepository
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class CurationRepositoryImpl @Inject constructor(
    private val apiService: CurationApiService,
    private val profileRefreshBus: ProfileRefreshBus,
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
            // A new collection changes every "my curations" list — tell observers (Add-to-Collection
            // sheet, Profile grids) to refresh so it appears next time the sheet is opened. Mirrors
            // the signal in addArtworksToCuration/updateCuration/deleteCuration.
            profileRefreshBus.signal()
            ApiResult.Success(CreatedCuration(id = data.id))
        } else {
            errorFor(response)
        }
    }

    override suspend fun getCurationArtItems(curationId: Int): ApiResult<List<UserArtItem>> = safeCall {
        val response = apiService.getCuration(curationId)
        val data = response.body()?.data
        if (response.isSuccessful && data != null) {
            ApiResult.Success(data.artworks.orEmpty().mapNotNull { it.toUserArtItem() })
        } else {
            errorFor(response)
        }
    }

    override suspend fun addArtworksToCuration(
        targetCurationId: Int,
        artworkIds: List<Int>,
    ): ApiResult<Unit> = safeCall {
        // 1. Read the target curation's current artworks so the PUT (full replace) preserves them.
        val getResp = apiService.getCuration(targetCurationId)
        val curation = getResp.body()?.data
        if (!getResp.isSuccessful || curation == null) return@safeCall errorFor(getResp)
        val existing = curation.artworks.orEmpty().mapNotNull { it.id }
        // Reject re-adding art already in this curation (no silent no-op PUT) so the UI can warn.
        if (artworkIds.isNotEmpty() && artworkIds.all { it in existing }) {
            return@safeCall ApiResult.Error.Validation("Art already exists in the collection")
        }
        val merged = (existing + artworkIds).distinct()
        // 2. PUT the merged membership, preserving the curation's title/description/privacy.
        val putResp = apiService.updateCuration(
            targetCurationId,
            UpdateCurationBody(
                title = curation.title,
                description = curation.description,
                privacy = curation.privacy,
                artworkIds = merged,
            ),
        )
        if (putResp.isSuccessful) {
            // Tell observers (Add-to-Collection sheet, Profile grids) the curation changed so previews
            // refresh — otherwise a just-emptied/just-filled collection keeps its stale thumbnail.
            profileRefreshBus.signal()
            ApiResult.Success(Unit)
        } else {
            errorFor(putResp)
        }
    }

    override suspend fun getCurationForEdit(id: Int): ApiResult<EditableCuration> = safeCall {
        val response = apiService.getCuration(id)
        val data = response.body()?.data
        if (response.isSuccessful && data != null) {
            ApiResult.Success(
                EditableCuration(
                    title = data.title.orEmpty(),
                    description = data.description,
                    isPrivate = data.privacy ?: false,
                    arts = data.artworks.orEmpty().mapNotNull { it.toUserArtItem() },
                ),
            )
        } else {
            errorFor(response)
        }
    }

    override suspend fun updateCuration(
        id: Int,
        title: String,
        description: String?,
        isPrivate: Boolean,
        artworkIds: List<Int>,
    ): ApiResult<Unit> = safeCall {
        val response = apiService.updateCuration(
            id,
            UpdateCurationBody(
                title = title,
                description = description,
                privacy = isPrivate,
                artworkIds = artworkIds,
            ),
        )
        if (response.isSuccessful) {
            profileRefreshBus.signal()
            ApiResult.Success(Unit)
        } else {
            errorFor(response)
        }
    }

    override suspend fun deleteCuration(id: Int): ApiResult<Unit> = safeCall {
        val response = apiService.deleteCuration(id)
        if (response.isSuccessful) {
            profileRefreshBus.signal()
            ApiResult.Success(Unit)
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

    private fun errorFor(response: Response<*>): ApiResult.Error {
        val body = runCatching { response.errorBody()?.string() }.getOrNull()
        return when (response.code()) {
            in 400..499 -> ApiResult.Error.Validation(
                serverMessageOrNull(body) ?: "Request failed (${response.code()})",
            )
            in 500..599 -> ApiResult.Error.Server(response.code())
            else -> ApiResult.Error.Unknown(RuntimeException("HTTP ${response.code()}"))
        }
    }
}
