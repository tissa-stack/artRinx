package com.example.artrinx.feature.profile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MediumDto(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("picture") val picture: String,
)

data class MediumsResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<MediumDto> = emptyList(),
)
