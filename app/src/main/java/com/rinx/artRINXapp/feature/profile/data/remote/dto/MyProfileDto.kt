package com.rinx.artRINXapp.feature.profile.data.remote.dto

import com.google.gson.annotations.SerializedName

/** GET /api/profile — the current user's own profile (header data + ownership id). */
data class MyProfileDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("profile_title") val profileTitle: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("dob") val dob: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("profile_link") val profileLink: String? = null,
    @SerializedName("profile_picture_url") val profilePictureUrl: String? = null,
    @SerializedName("full_name_edit_count") val fullNameEditCount: Int? = null,
    @SerializedName(value = "marketing_sms_consent", alternate = ["marketingSmsConsent"]) val marketingSmsConsent: Boolean? = null,
    @SerializedName("invitation_code") val invitationCode: String? = null,
    @SerializedName("remaining_invites") val remainingInvites: Int? = null,
    @SerializedName("total_user_invites") val totalUserInvites: Int? = null,
    @SerializedName("subscription") val subscription: SubscriptionDto? = null,
    // Role name + active-artwork cap (handout Upload gate). Optional — confirm wire names w/ backend.
    @SerializedName(value = "profile_type_name", alternate = ["profileTypeName"]) val profileTypeName: String? = null,
    @SerializedName(value = "max_uploads", alternate = ["maxUploads"]) val maxUploads: Int? = null,
    // New-chats-this-month counter (2026-06 semantic). Optional.
    @SerializedName(value = "remaining_chat_invites", alternate = ["remainingChatInvites"]) val remainingChatInvites: Int? = null,
    @SerializedName("artwork_count") val artworkCount: Int? = null,
    @SerializedName("curation_count") val curationCount: Int? = null,
    @SerializedName("follower_count") val followerCount: Int? = null,
    @SerializedName("following_count") val followingCount: Int? = null,
    // V1.9 override-aware nested counters (preferred over the legacy flat fields above).
    @SerializedName("peer_invites") val peerInvites: QuotaCounterDto? = null,
    @SerializedName("message_requests") val messageRequests: QuotaCounterDto? = null,
)

/** Override-aware quota counter (handout §16): reflects admin overrides on the plan default. */
data class QuotaCounterDto(
    @SerializedName(value = "monthly_cap", alternate = ["monthlyCap"]) val monthlyCap: Int? = null,
    @SerializedName("remaining") val remaining: Int? = null,
)
