package com.example.artrinx.feature.profile.data.remote.dto

import com.google.gson.annotations.SerializedName

/** GET /api/profile — the current user's own profile (header data + ownership id). */
data class MyProfileDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("profile_title") val profileTitle: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("age") val age: Int? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("profile_link") val profileLink: String? = null,
    @SerializedName("profile_picture_url") val profilePictureUrl: String? = null,
    @SerializedName("full_name_edit_count") val fullNameEditCount: Int? = null,
    @SerializedName("subscription") val subscription: SubscriptionDto? = null,
    @SerializedName("artwork_count") val artworkCount: Int? = null,
    @SerializedName("curation_count") val curationCount: Int? = null,
    @SerializedName("follower_count") val followerCount: Int? = null,
    @SerializedName("following_count") val followingCount: Int? = null,
)
