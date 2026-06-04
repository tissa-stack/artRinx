package com.example.artrinx.feature.home.data.remote.dto

import com.google.gson.annotations.SerializedName

// ── Standard envelope + pagination wrapper ────────────────────────────────────

data class EnvelopeDto<T>(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("code") val code: Int = 0,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null,
)

data class PageDto<T>(
    @SerializedName("items") val items: List<T>? = null,
    @SerializedName("total") val total: Int = 0,
    @SerializedName("page") val page: Int = 1,
    @SerializedName("size") val size: Int = 0,
)

// ── Artwork (snake_case on the wire — verified from live /api/feed/discover) ──

data class ArtworkDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerializedName("webp_url") val webpUrl: String? = null,
    @SerializedName("shop_link") val shopLink: String? = null,
    @SerializedName("price") val price: Double? = null,
    @SerializedName("privacy") val privacy: Boolean? = null,
    @SerializedName("likes_count") val likesCount: Int? = null,
    @SerializedName("is_liked") val isLiked: Boolean? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("profile_type_name") val profileTypeName: String? = null,
    @SerializedName("profile_picture_url") val profilePictureUrl: String? = null,
    @SerializedName("aspect_ratio") val aspectRatio: Double? = null,
    @SerializedName("artist") val artist: ArtistDto? = null,
    @SerializedName("medium") val medium: ArtworkMediumDto? = null,
)

data class ArtistDto(
    @SerializedName("artist_id") val artistId: Int? = null,
    @SerializedName("artist_name") val artistName: String? = null,
)

data class ArtworkMediumDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("picture") val picture: String? = null,
)

// ── Curation (snake_case) ─────────────────────────────────────────────────────

data class CurationDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("privacy") val privacy: Boolean? = null,
    @SerializedName("artworks") val artworks: List<ArtworkDto>? = null,
    @SerializedName("author") val author: AuthorDto? = null,
    @SerializedName("likes_count") val likesCount: Int? = null,
    @SerializedName("is_liked") val isLiked: Boolean? = null,
)

data class AuthorDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("profile_picture") val profilePicture: String? = null,
)

// ── Banner / sponsored item (snake_case) ──────────────────────────────────────

data class BannerDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("subtitle") val subtitle: String? = null,
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("link") val link: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("is_active") val isActive: Boolean? = null,
)

// ── Discover feed: ONE call returns sponsored + new_art + popular_curations ───

data class DiscoverFeedDto(
    @SerializedName("sponsored") val sponsored: List<BannerDto>? = null,
    @SerializedName("new_art") val newArt: List<ArtworkDto>? = null,
    @SerializedName("popular_curations") val popularCurations: List<CurationDto>? = null,
    @SerializedName("recently_viewed") val recentlyViewed: List<ArtworkDto>? = null,
)

// ── Request bodies ────────────────────────────────────────────────────────────

data class LikeArtworkRequest(
    @SerializedName("artwork_id") val artworkId: Int,
)

data class LikeCurationRequest(
    @SerializedName("curation_id") val curationId: Int,
)