package com.rinx.artRINXapp.feature.share.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Body for `POST /api/share` when sharing an entity to selected followers.
 *
 * Shape confirmed against the live backend (Swagger / 422 on the old guess): recipients go in
 * [toUserIds], and exactly one of [artworkId] / [curationId] / [profileId] identifies what's being
 * shared. Gson omits null fields by default, so only the relevant id is sent on the wire.
 */
data class ShareRequest(
    @SerializedName("to_user_ids") val toUserIds: List<Int>,
    @SerializedName("artwork_id") val artworkId: Int? = null,
    @SerializedName("curation_id") val curationId: Int? = null,
    @SerializedName("profile_id") val profileId: Int? = null,
    @SerializedName("note") val note: String? = null,
)
