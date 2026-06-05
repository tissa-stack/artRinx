package com.example.artrinx.feature.profile.domain.repository

import android.net.Uri
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.profile.domain.model.BlockedUser
import com.example.artrinx.feature.profile.domain.model.CurrentUser
import com.example.artrinx.feature.profile.domain.model.EditableProfile
import com.example.artrinx.feature.profile.domain.model.InviteInfo
import com.example.artrinx.feature.profile.domain.model.InvitedUser
import com.example.artrinx.feature.profile.domain.model.Medium
import com.example.artrinx.feature.profile.domain.model.ProfileArtItem
import com.example.artrinx.feature.profile.domain.model.ProfileCurationItem
import com.example.artrinx.feature.profile.domain.model.ProfileDraft
import com.example.artrinx.feature.profile.domain.model.ProfilePlanSummary
import com.example.artrinx.feature.profile.domain.model.ProfileType
import com.example.artrinx.feature.profile.domain.model.ProfileUpdate
import com.example.artrinx.feature.profile.domain.model.UserProfileData

interface ProfileRepository {
    suspend fun getProfileTypes(): ApiResult<List<ProfileType>>
    suspend fun getMyProfile(): ApiResult<CurrentUser>
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
    /** Report an artwork with a free-text [message]. */
    suspend fun reportArtwork(artworkId: Int, message: String): ApiResult<Unit>
    /** Report a curation with a free-text [message]. */
    suspend fun reportCuration(curationId: Int, message: String): ApiResult<Unit>
    /** Block (and report) an artwork. */
    suspend fun blockArtwork(artworkId: Int, message: String): ApiResult<Unit>
    /** Block a user. */
    suspend fun blockUser(userId: Int): ApiResult<Unit>
    /** Persist changed profile fields (and optionally a new picture). */
    suspend fun updateProfile(changes: ProfileUpdate, newPictureUri: Uri?): ApiResult<Unit>
    suspend fun checkUsername(username: String): ApiResult<Boolean>
    suspend fun getMediums(): ApiResult<List<Medium>>
    suspend fun getMyArtworks(page: Int, size: Int): ApiResult<List<ProfileArtItem>>
    suspend fun getMyCurations(page: Int, size: Int): ApiResult<List<ProfileCurationItem>>
    suspend fun getLikedArtworks(page: Int, size: Int): ApiResult<List<ProfileArtItem>>
    suspend fun createProfile(draft: ProfileDraft, pictureUri: Uri?): ApiResult<Unit>
}
