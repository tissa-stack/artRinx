package com.rinx.artRINXapp.feature.search.data.remote.dto

import com.rinx.artRINXapp.feature.home.data.remote.dto.ArtworkDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.CurationDto
import com.google.gson.annotations.SerializedName

/**
 * `GET /api/search` → `data`. The backend returns the matched artworks / curations / users in
 * parallel arrays (the `category` query param selects which one is populated). The artwork and
 * curation element shapes are identical to the discover feed, so we reuse those DTOs.
 */
data class SearchDataDto(
    @SerializedName("artworks") val artworks: List<ArtworkDto>? = null,
    @SerializedName("curations") val curations: List<CurationDto>? = null,
    @SerializedName("users") val users: List<SearchUserDto>? = null,
)

/**
 * `GET /api/search/locations/{countries|states|cities}` → `data`. Each endpoint returns places that
 * contain at least one searchable user, ranked by `count` (number of users) or alphabetically.
 */
data class LocationItemsDataDto(
    @SerializedName("items") val items: List<LocationItemDto>? = null,
    @SerializedName("total") val total: Int = 0,
)

data class LocationItemDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("count") val count: Int = 0,
)

/** A user/artist result. Defensive (all-nullable) — the exact shape isn't pinned in the contract. */
data class SearchUserDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("user_id") val userId: Int? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("profile_type_name") val profileTypeName: String? = null,
    @SerializedName("profile_picture_url") val profilePictureUrl: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("follower_count") val followerCount: Int? = null,
    @SerializedName("is_following") val isFollowing: Boolean? = null,
)