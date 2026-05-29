package com.example.artrinx.feature.auth.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("id") val id: Int,
    @SerializedName("auth_user_id") val authUserId: String,
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("email_verified") val emailVerified: Boolean,
    @SerializedName("phone_verified") val phoneVerified: Boolean,
    @SerializedName("is_admin") val isAdmin: Boolean,
    @SerializedName("role") val role: String,
    @SerializedName("profile_exists") val profileExists: Boolean,
    @SerializedName("profile_completed") val profileCompleted: Boolean,
    @SerializedName("consents") val consents: ConsentsDto?,
)
