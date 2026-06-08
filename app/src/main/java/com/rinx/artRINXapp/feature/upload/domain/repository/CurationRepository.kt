package com.rinx.artRINXapp.feature.upload.domain.repository

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.upload.domain.model.CreateCurationRequest
import com.rinx.artRINXapp.feature.upload.domain.model.CreatedCuration
import com.rinx.artRINXapp.feature.upload.domain.model.EditableCuration
import com.rinx.artRINXapp.feature.upload.domain.model.UserArtItem

interface CurationRepository {

    /** Current user's own artworks (the "Uploads" selection tab). */
    suspend fun getMyArtworks(page: Int, size: Int): ApiResult<List<UserArtItem>>

    /** Artworks the current user has liked (the "Liked" selection tab). */
    suspend fun getLikedArtworks(page: Int, size: Int): ApiResult<List<UserArtItem>>

    /** Create a curation from selected artwork ids. */
    suspend fun createCuration(request: CreateCurationRequest): ApiResult<CreatedCuration>

    /** A curation's artworks as selectable items (for previewing / preselecting). */
    suspend fun getCurationArtItems(curationId: Int): ApiResult<List<UserArtItem>>

    /** Add [artworkIds] to an existing curation, preserving its current artworks (GET + merge + PUT). */
    suspend fun addArtworksToCuration(targetCurationId: Int, artworkIds: List<Int>): ApiResult<Unit>

    /** Fetch an existing curation's fields (title/description/privacy + arts) to prefill the edit flow. */
    suspend fun getCurationForEdit(id: Int): ApiResult<EditableCuration>

    /** Edit a curation (PUT — full update of title/description/privacy/artwork_ids). */
    suspend fun updateCuration(
        id: Int,
        title: String,
        description: String?,
        isPrivate: Boolean,
        artworkIds: List<Int>,
    ): ApiResult<Unit>

    /** Delete a curation. */
    suspend fun deleteCuration(id: Int): ApiResult<Unit>
}
