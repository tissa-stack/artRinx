package com.example.artrinx.feature.upload.domain.repository

import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.upload.domain.model.CreateCurationRequest
import com.example.artrinx.feature.upload.domain.model.CreatedCuration
import com.example.artrinx.feature.upload.domain.model.UserArtItem

interface CurationRepository {

    /** Current user's own artworks (the "Uploads" selection tab). */
    suspend fun getMyArtworks(page: Int, size: Int): ApiResult<List<UserArtItem>>

    /** Artworks the current user has liked (the "Liked" selection tab). */
    suspend fun getLikedArtworks(page: Int, size: Int): ApiResult<List<UserArtItem>>

    /** Create a curation from selected artwork ids. */
    suspend fun createCuration(request: CreateCurationRequest): ApiResult<CreatedCuration>
}
