package com.rinx.artRINXapp.feature.auth.data.remote.dto

import com.google.gson.annotations.SerializedName

/** `POST /api/auth/native/contact/start-change` — sends an OTP to [newValue] (API §1.8). */
data class ContactStartChangeRequest(
    @SerializedName("kind") val kind: String = "email",
    @SerializedName("new_value") val newValue: String,
)

/** `POST /api/auth/native/contact/confirm-change` — verifies the OTP and swaps the contact (§1.8). */
data class ContactConfirmChangeRequest(
    @SerializedName("kind") val kind: String = "email",
    @SerializedName("new_value") val newValue: String,
    @SerializedName("code") val code: String,
)
