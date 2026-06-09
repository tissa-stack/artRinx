package com.rinx.artRINXapp.core.push.dto

import com.google.gson.annotations.SerializedName

/** `POST /api/me/fcm-token` body (API §11). `platform`/`device_id` per handout's push spec. */
data class FcmTokenRequest(
    @SerializedName("fcm_token") val token: String,
    @SerializedName("platform") val platform: String = "android",
    @SerializedName("device_id") val deviceId: String? = null,
)
