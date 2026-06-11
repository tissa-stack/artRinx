package com.rinx.artRINXapp.feature.share.data.repository

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.share.data.remote.ShareApiService
import com.rinx.artRINXapp.feature.share.data.remote.dto.ShareRequest
import com.rinx.artRINXapp.feature.share.domain.model.ShareKind
import com.rinx.artRINXapp.feature.share.domain.repository.ShareRepository
import java.io.IOException
import javax.inject.Inject

class ShareRepositoryImpl @Inject constructor(
    private val api: ShareApiService,
) : ShareRepository {

    override suspend fun shareToFollowers(
        kind: ShareKind,
        resourceId: String,
        recipientIds: List<Int>,
    ): ApiResult<Unit> {
        val id = resourceId.toIntOrNull()
            ?: return ApiResult.Error.Validation("Invalid item id")
        val request = when (kind) {
            ShareKind.ARTWORK -> ShareRequest(toUserIds = recipientIds, artworkId = id)
            ShareKind.CURATION -> ShareRequest(toUserIds = recipientIds, curationId = id)
            ShareKind.PROFILE -> ShareRequest(toUserIds = recipientIds, profileId = id)
        }
        return try {
            val response = api.share(request)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                when (response.code()) {
                    in 500..599 -> ApiResult.Error.Server(response.code())
                    else -> ApiResult.Error.Validation("Couldn't share (${response.code()})")
                }
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }
}
