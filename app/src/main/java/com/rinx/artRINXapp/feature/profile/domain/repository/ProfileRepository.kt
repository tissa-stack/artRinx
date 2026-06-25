package com.rinx.artRINXapp.feature.profile.domain.repository

import android.net.Uri
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.profile.domain.model.BlockedArtwork
import com.rinx.artRINXapp.feature.profile.domain.model.BlockedUser
import com.rinx.artRINXapp.feature.profile.domain.model.CurrentUser
import com.rinx.artRINXapp.feature.profile.domain.model.EditableProfile
import com.rinx.artRINXapp.feature.profile.domain.model.FollowUser
import com.rinx.artRINXapp.feature.profile.domain.model.InviteInfo
import com.rinx.artRINXapp.feature.profile.domain.model.InvitedUser
import com.rinx.artRINXapp.feature.profile.domain.model.Medium
import com.rinx.artRINXapp.feature.profile.domain.model.PublicProfile
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileArtItem
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileCurationItem
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileDraft
import com.rinx.artRINXapp.feature.profile.domain.model.ProfilePlanSummary
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileType
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileUpdate
import com.rinx.artRINXapp.feature.profile.domain.model.UploadQuota
import com.rinx.artRINXapp.feature.profile.domain.model.UserProfileData

interface ProfileRepository {
    suspend fun getProfileTypes(): ApiResult<List<ProfileType>>
    suspend fun getMyProfile(): ApiResult<CurrentUser>
    /** Role + plan + artwork-count snapshot driving the Upload-tap gate. */
    suspend fun getUploadQuota(): ApiResult<UploadQuota>
    /** Full profile (header + counts) for the Profile screen. */
    suspend fun getProfileData(): ApiResult<UserProfileData>
    /** The current user's profile in editable form, for the Edit Profile screen. */
    suspend fun getEditableProfile(): ApiResult<EditableProfile>
    /** The current user's profile title + subscription plan, for the Profile-title-and-plan screen. */
    suspend fun getProfilePlanSummary(): ApiResult<ProfilePlanSummary>
    /** The current user's invite code + remaining monthly invites. */
    suspend fun getInviteInfo(): ApiResult<InviteInfo>
    /** Users who joined via [code] — the "people I invited" list. */
    suspend fun getInvitedUsers(code: String, page: Int, size: Int): ApiResult<List<InvitedUser>>
    /** The current user's blocked users. */
    suspend fun getBlockedUsers(page: Int, size: Int): ApiResult<List<BlockedUser>>
    /** Unblock the user with [userId]. */
    suspend fun unblockUser(userId: Int): ApiResult<Unit>
    /** The current user's blocked artworks. */
    suspend fun getBlockedArtworks(page: Int, size: Int): ApiResult<List<BlockedArtwork>>
    /** Unblock the artwork with [artworkId]. */
    suspend fun unblockArtwork(artworkId: Int): ApiResult<Unit>
    /** Report an artwork with a free-text [message]. */
    suspend fun reportArtwork(artworkId: Int, message: String): ApiResult<Unit>
    /** Report a curation with a free-text [message]. */
    suspend fun reportCuration(curationId: Int, message: String): ApiResult<Unit>
    /** Report a user/profile with a free-text [message]. */
    suspend fun reportUser(userId: Int, message: String): ApiResult<Unit>
    /** Submit free-text app feedback (POST /api/feedbacks/). */
    suspend fun submitFeedback(review: String): ApiResult<Unit>

    // ── Other user's public profile ─────────────────────────────────────────
    suspend fun getPublicProfile(userId: Int): ApiResult<PublicProfile>
    suspend fun getPublicArtworks(userId: Int, page: Int, size: Int): ApiResult<List<ProfileArtItem>>
    /** Artworks credited to an artist display name (for the "Art by <name>" screen). */
    suspend fun getArtworksByName(name: String, page: Int, size: Int): ApiResult<List<ProfileArtItem>>
    suspend fun getPublicCurations(userId: Int, page: Int, size: Int): ApiResult<List<ProfileCurationItem>>
    suspend fun followUser(userId: Int): ApiResult<Unit>
    suspend fun unfollowUser(userId: Int): ApiResult<Unit>
    /** The current user's followers. */
    suspend fun getFollowers(page: Int, size: Int): ApiResult<List<FollowUser>>
    /** The users the current user follows. */
    suspend fun getFollowing(page: Int, size: Int): ApiResult<List<FollowUser>>
    /** Block (and report) an artwork. */
    suspend fun blockArtwork(artworkId: Int, message: String): ApiResult<Unit>
    /** Block a user. */
    suspend fun blockUser(userId: Int): ApiResult<Unit>
    /** Persist changed profile fields (and optionally a new picture). */
    suspend fun updateProfile(changes: ProfileUpdate, newPictureUri: Uri?): ApiResult<Unit>
    suspend fun checkUsername(username: String): ApiResult<Boolean>
    /** The full catalog of selectable mediums (GET /api/mediums/). */
    suspend fun getMediums(): ApiResult<List<Medium>>
    /** The current user's currently-selected mediums (GET /api/profile/mediums). */
    suspend fun getUserMediums(): ApiResult<List<Medium>>
    /** Replace the user's selected mediums (PUT /api/profile/mediums). */
    suspend fun updateUserMediums(mediumIds: List<Int>): ApiResult<Unit>
    suspend fun getMyArtworks(page: Int, size: Int): ApiResult<List<ProfileArtItem>>
    suspend fun getMyCurations(page: Int, size: Int): ApiResult<List<ProfileCurationItem>>
    suspend fun getLikedArtworks(page: Int, size: Int): ApiResult<List<ProfileArtItem>>
    suspend fun createProfile(draft: ProfileDraft, pictureUri: Uri?): ApiResult<Unit>

    // ── In-memory SWR cache for the Profile tab (first page only; cleared on logout/delete) ───
    /** The logged-in user's numeric id, cached from the last successful [getMyProfile]; null until then. */
    fun cachedCurrentUserId(): Int?
    fun cachedProfileData(): UserProfileData?
    fun cachedMyArtworks(): List<ProfileArtItem>?
    fun cachedMyCurations(): List<ProfileCurationItem>?
    fun cachedLikedArtworks(): List<ProfileArtItem>?
    /** Last-known upload quota, cached from [getUploadQuota]; seeds the Create screen without flicker. */
    fun cachedUploadQuota(): UploadQuota?
    fun clearCache()
}
