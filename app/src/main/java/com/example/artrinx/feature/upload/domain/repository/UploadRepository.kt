package com.example.artrinx.feature.upload.domain.repository

import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.upload.domain.model.CreatedArtwork
import com.example.artrinx.feature.upload.domain.model.EditableArtwork
import com.example.artrinx.feature.upload.domain.model.PreparedUpload
import com.example.artrinx.feature.upload.domain.model.UpdateArtworkRequest
import com.example.artrinx.feature.upload.domain.model.UploadRequest

interface UploadRepository {

    /** Step 1: get a signed CDN URL + file path. */
    suspend fun prepareUpload(): ApiResult<PreparedUpload>

    /** Step 2: PUT the JPEG bytes to the signed URL (no auth). [onProgress] reports sent/total bytes. */
    suspend fun uploadBytes(
        uploadUrl: String,
        jpeg: ByteArray,
        onProgress: (sent: Long, total: Long) -> Unit,
    ): ApiResult<Unit>

    /** Step 3: finalize the artwork referencing the uploaded [filePath]. */
    suspend fun createArtwork(
        filePath: String,
        request: UploadRequest,
        aspectRatio: String,
        rekognitionTags: String?,
    ): ApiResult<CreatedArtwork>

    /** Fetch an existing artwork's fields to prefill the edit form. */
    suspend fun getArtworkForEdit(id: Int): ApiResult<EditableArtwork>

    /** Edit an artwork's metadata (PUT, JSON; image unchanged). */
    suspend fun updateArtwork(id: Int, request: UpdateArtworkRequest): ApiResult<Unit>

    /** Delete an artwork. */
    suspend fun deleteArtwork(id: Int): ApiResult<Unit>
}
