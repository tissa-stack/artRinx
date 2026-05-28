package com.example.artrinx.feature.auth.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WaitlistResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: Any?,
)
