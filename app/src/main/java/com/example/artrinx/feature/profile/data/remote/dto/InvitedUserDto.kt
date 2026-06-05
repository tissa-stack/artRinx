package com.example.artrinx.feature.profile.data.remote.dto

import com.google.gson.annotations.SerializedName

/** An item from GET /api/profiles/by-invite-code — a user who joined via the invite code (§2.3). */
data class InvitedUserDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("profile_picture_url") val profilePictureUrl: String? = null,
    @SerializedName("joined_at") val joinedAt: String? = null,
)
