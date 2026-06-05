package com.example.artrinx.feature.profile.data.remote.dto

import com.google.gson.annotations.SerializedName

/** GET /api/profile/{userId}/public/info — another user's public profile. */
data class PublicUserProfileDto(
    @SerializedName("username") val username: String? = null,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("profile_type_name") val profileTypeName: String? = null,
    @SerializedName("profile_picture_url") val profilePictureUrl: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("profile_link") val profileLink: String? = null,
    @SerializedName("artwork_count") val artworkCount: Int? = null,
    @SerializedName("curation_count") val curationCount: Int? = null,
    @SerializedName("follower_count") val followerCount: Int? = null,
    @SerializedName("following_count") val followingCount: Int? = null,
    @SerializedName("is_following") val isFollowing: Boolean? = null,
    @SerializedName("i_blocked") val iBlocked: Boolean? = null,
    @SerializedName("they_blocked") val theyBlocked: Boolean? = null,
    @SerializedName("can_message") val canMessage: Boolean? = null,
    @SerializedName("chatroom_id") val chatroomId: String? = null,
)
