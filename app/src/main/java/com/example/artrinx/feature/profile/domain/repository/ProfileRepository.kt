package com.example.artrinx.feature.profile.domain.repository

import android.net.Uri
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.profile.domain.model.CurrentUser
import com.example.artrinx.feature.profile.domain.model.Medium
import com.example.artrinx.feature.profile.domain.model.ProfileArtItem
import com.example.artrinx.feature.profile.domain.model.ProfileCurationItem
import com.example.artrinx.feature.profile.domain.model.ProfileDraft
import com.example.artrinx.feature.profile.domain.model.ProfileType
import com.example.artrinx.feature.profile.domain.model.UserProfileData

interface ProfileRepository {
    suspend fun getProfileTypes(): ApiResult<List<ProfileType>>
    suspend fun getMyProfile(): ApiResult<CurrentUser>
    /** Full profile (header + counts) for the Profile screen. */
    suspend fun getProfileData(): ApiResult<UserProfileData>
    suspend fun checkUsername(username: String): ApiResult<Boolean>
    suspend fun getMediums(): ApiResult<List<Medium>>
    suspend fun getMyArtworks(page: Int, size: Int): ApiResult<List<ProfileArtItem>>
    suspend fun getMyCurations(page: Int, size: Int): ApiResult<List<ProfileCurationItem>>
    suspend fun getLikedArtworks(page: Int, size: Int): ApiResult<List<ProfileArtItem>>
    suspend fun createProfile(draft: ProfileDraft, pictureUri: Uri?): ApiResult<Unit>
}
