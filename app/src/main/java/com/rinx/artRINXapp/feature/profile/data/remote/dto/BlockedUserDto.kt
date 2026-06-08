package com.rinx.artRINXapp.feature.profile.data.remote.dto

import com.google.gson.annotations.SerializedName

/** An item from GET /api/blocked/users — a user the current user has blocked (§3.8). */
data class BlockedUserDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("user_id") val userId: Int? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("profile_type_name") val profileTypeName: String? = null,
    @SerializedName("profile_picture_url") val profilePictureUrl: String? = null,
)
