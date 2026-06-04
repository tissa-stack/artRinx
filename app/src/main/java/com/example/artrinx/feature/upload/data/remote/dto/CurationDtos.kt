package com.example.artrinx.feature.upload.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Request body for POST /api/curations/ (application/json). */
data class CreateCurationBody(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("privacy") val privacy: Boolean,
    @SerializedName("artwork_ids") val artworkIds: List<Int>,
)
