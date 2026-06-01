package com.example.artrinx.feature.profile.domain.repository

import android.net.Uri
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.profile.domain.model.Medium
import com.example.artrinx.feature.profile.domain.model.ProfileDraft
import com.example.artrinx.feature.profile.domain.model.ProfileType

interface ProfileRepository {
    suspend fun getProfileTypes(): ApiResult<List<ProfileType>>
    suspend fun checkUsername(username: String): ApiResult<Boolean>
    suspend fun getMediums(): ApiResult<List<Medium>>
    suspend fun createProfile(draft: ProfileDraft, pictureUri: Uri?): ApiResult<Unit>
}
