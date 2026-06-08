package com.rinx.artRINXapp.feature.profile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UsernameCheckResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: Boolean,
)
