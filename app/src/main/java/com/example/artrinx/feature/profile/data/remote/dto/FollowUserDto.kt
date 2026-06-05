package com.example.artrinx.feature.profile.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Item from GET /api/followers and /api/followed-users (UserFollowedShare). */
data class FollowUserDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("profile_picture_url") val profilePictureUrl: String? = null,
    @SerializedName("is_following") val isFollowing: Boolean? = null,
)
