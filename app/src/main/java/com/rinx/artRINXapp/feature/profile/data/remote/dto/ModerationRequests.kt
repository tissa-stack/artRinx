package com.rinx.artRINXapp.feature.profile.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * POST /api/block. Send [userId] to block a user, or [artId] + [message] to block + report an
 * artwork. Field is `art_id` on the wire (verified against the OpenAPI spec).
 */
data class BlockRequest(
    @SerializedName("user_id") val userId: Int? = null,
    @SerializedName("art_id") val artId: Int? = null,
    @SerializedName("message") val message: String? = null,
)

/** POST /api/report-artwork. [message] is the free-text reason. */
data class ReportArtworkRequest(
    @SerializedName("artwork_id") val artworkId: Int,
    @SerializedName("message") val message: String,
)

/** POST /api/report-curation. [message] is the free-text reason. */
data class ReportCurationRequest(
    @SerializedName("curation_id") val curationId: Int,
    @SerializedName("message") val message: String,
)

/** POST /api/follow. */
data class FollowRequest(
    @SerializedName("followed_id") val followedId: Int,
)

/** POST /api/report-message — used to report a user/profile. */
data class ReportMessageRequest(
    @SerializedName("reported_user_id") val reportedUserId: Int,
    @SerializedName("message") val message: String,
)

/** POST /api/feedbacks/ — free-text app feedback. */
data class FeedbackRequest(
    @SerializedName("review") val review: String,
)
