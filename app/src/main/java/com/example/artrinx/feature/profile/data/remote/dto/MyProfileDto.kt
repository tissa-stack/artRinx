package com.example.artrinx.feature.profile.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Subset of GET /api/profile needed to attribute an upload to the current user ("myself"). */
data class MyProfileDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("profile_picture_url") val profilePictureUrl: String? = null,
)
