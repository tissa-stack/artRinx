package com.rinx.artRINXapp.feature.auth.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OtpVerifyResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("access_expires_in") val accessExpiresIn: Long,
    @SerializedName("refresh_expires_in") val refreshExpiresIn: Long,
    @SerializedName("user") val user: UserDto,
)
