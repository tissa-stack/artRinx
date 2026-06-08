package com.rinx.artRINXapp.feature.upload.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Request body for POST /api/curations/ (application/json). */
data class CreateCurationBody(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("privacy") val privacy: Boolean,
    @SerializedName("artwork_ids") val artworkIds: List<Int>,
)

/** Request body for PUT /api/curations/{id}. Send all fields so the PUT (full update) doesn't blank them. */
data class UpdateCurationBody(
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("privacy") val privacy: Boolean? = null,
    @SerializedName("artwork_ids") val artworkIds: List<Int>,
)
