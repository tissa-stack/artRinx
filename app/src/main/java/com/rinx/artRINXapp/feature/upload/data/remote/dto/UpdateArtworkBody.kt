package com.rinx.artRINXapp.feature.upload.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Request body for PUT /api/artworks/{id} (application/json) — metadata-only edit (no image). */
data class UpdateArtworkBody(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("tags") val tags: List<String>?,
    @SerializedName("medium_id") val mediumId: Int?,
    @SerializedName("shop_link") val shopLink: String?,
    @SerializedName("price") val price: Double?,
    @SerializedName("privacy") val privacy: Boolean?,
    @SerializedName("artist_id") val artistId: Int?,
    @SerializedName("artist_name") val artistName: String?,
)
