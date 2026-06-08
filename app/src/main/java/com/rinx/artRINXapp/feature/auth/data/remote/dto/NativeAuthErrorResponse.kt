package com.rinx.artRINXapp.feature.auth.data.remote.dto

import com.google.gson.annotations.SerializedName

data class NativeAuthErrorResponse(
    @SerializedName("detail") val detail: NativeAuthErrorDetail?,
)

data class NativeAuthErrorDetail(
    @SerializedName("code") val code: String?,
)
