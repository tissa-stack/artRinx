package com.rinx.artRINXapp.feature.profile.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * An item from GET /api/blocked/artworks — an artwork the current user has blocked (§3.8).
 * This endpoint returns snake_case (unlike the camelCase Artwork shape in api-skill §4.1),
 * so the keys are pinned explicitly. Only the fields the Blocked Artworks row needs are mapped.
 */
data class BlockedArtworkDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("webp_url") val webpUrl: String? = null,
)
