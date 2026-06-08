package com.rinx.artRINXapp.feature.auth.data.remote.dto

import com.google.gson.annotations.SerializedName

/** `POST /api/auth/native/contact/start-add` — attach a NEW contact (no existing one), §1.8. */
data class ContactStartAddRequest(
    @SerializedName("kind") val kind: String = "email",
    @SerializedName("value") val value: String,
)

/** `POST /api/auth/native/contact/confirm-add` — verify the OTP and persist the new contact (§1.8). */
data class ContactConfirmAddRequest(
    @SerializedName("kind") val kind: String = "email",
    @SerializedName("value") val value: String,
    @SerializedName("code") val code: String,
)
