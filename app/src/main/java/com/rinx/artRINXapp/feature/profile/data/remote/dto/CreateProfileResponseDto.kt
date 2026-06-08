package com.rinx.artRINXapp.feature.profile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateProfileResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("code") val code: Int = 0,
    @SerializedName("message") val message: String = "",
    @SerializedName("data") val data: ProfileDataDto? = null,
)

data class ProfileDataDto(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("display_name") val displayName: String,
)
