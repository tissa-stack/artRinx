package com.example.artrinx.feature.auth.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OtpVerifyRequest(
    @SerializedName("code") val code: String,
    @SerializedName("mode") val mode: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("invite_code") val inviteCode: String? = null,
    @SerializedName("consents") val consents: ConsentsDto? = null,
)
