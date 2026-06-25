package com.rinx.artRINXapp.feature.profile.data.remote.dto

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

/** Body for PUT /api/profile/mediums — replaces the user's selected mediums. */
data class UpdateMediumsRequestDto(
    @SerializedName("medium_ids") val mediumIds: List<Int>,
)
