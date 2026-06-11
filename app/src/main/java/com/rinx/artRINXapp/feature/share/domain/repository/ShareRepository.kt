package com.rinx.artRINXapp.feature.share.domain.repository

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.share.domain.model.ShareKind

interface ShareRepository {
    /** Share the [kind]/[resourceId] entity to the given follower ids (recipients get a notification). */
    suspend fun shareToFollowers(
        kind: ShareKind,
        resourceId: String,
        recipientIds: List<Int>,
    ): ApiResult<Unit>
}
