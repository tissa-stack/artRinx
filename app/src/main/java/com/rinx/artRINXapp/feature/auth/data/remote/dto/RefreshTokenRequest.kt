package com.rinx.artRINXapp.feature.auth.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RefreshTokenRequest(
    @SerializedName("refresh_token") val refreshToken: String,
)
