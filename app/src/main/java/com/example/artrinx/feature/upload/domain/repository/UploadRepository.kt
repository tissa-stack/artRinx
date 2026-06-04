package com.example.artrinx.feature.upload.domain.repository

import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.upload.domain.model.CreatedArtwork
import com.example.artrinx.feature.upload.domain.model.PreparedUpload
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
}
