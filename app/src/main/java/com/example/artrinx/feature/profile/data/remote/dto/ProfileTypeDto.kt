package com.example.artrinx.feature.profile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProfileTypeDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
)

data class ProfileTypesResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<ProfileTypeDto> = emptyList(),
)
