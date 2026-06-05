package com.example.artrinx.feature.profile.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Subscription embedded in the profile response (§9.2). Fields are camelCase on the wire.
 * `plan` is "basic" | "artist_pro"; `status` is active | trialing | cancelled | past_due | expired.
 */
data class SubscriptionDto(
    @SerializedName("plan") val plan: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("provider") val provider: String? = null,
    @SerializedName("expiresAt") val expiresAt: String? = null,
    @SerializedName("renewsAt") val renewsAt: String? = null,
    @SerializedName("trialEndsAt") val trialEndsAt: String? = null,
)
