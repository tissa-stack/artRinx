package com.example.artrinx.feature.upload.domain

import com.example.artrinx.core.di.ApplicationScope
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.core.util.ImageCompressor
import com.example.artrinx.feature.upload.domain.model.UploadProgress
import com.example.artrinx.feature.upload.domain.model.UploadRequest
import com.example.artrinx.feature.upload.domain.repository.UploadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped orchestrator for the 3-step artwork upload (compress → prepare → PUT → finalize).
 * Runs on the process-lifetime [ApplicationScope] so the upload survives the user navigating away
 * from the upload screen. Exposes a single [progress] flow that Home (public) and Profile (private)
 * observe to render the "Uploading" row and optimistically insert the finished artwork.
 */
@Singleton
class UploadManager @Inject constructor(
    private val repository: UploadRepository,
    private val compressor: ImageCompressor,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val _progress = MutableStateFlow<UploadProgress?>(null)
    val progress: StateFlow<UploadProgress?> = _progress.asStateFlow()

    private var job: Job? = null
    private var lastRequest: UploadRequest? = null
    private var lastArtistName: String = ""
    private var lastArtistHandle: String = ""

    /** Fire-and-forget. Ignores the call if an upload is already running. */
    fun enqueue(request: UploadRequest, artistName: String = "", artistHandle: String = "") {
        if (job?.isActive == true) return
        lastRequest = request
        lastArtistName = artistName
        lastArtistHandle = artistHandle
        job = scope.launch { run(request, artistName, artistHandle) }
    }

    /** Re-run the last upload from scratch (file_path from a stale prepare may have expired). */
    fun retry() {
        val req = lastRequest ?: return
        enqueue(req, lastArtistName, lastArtistHandle)
    }

    /** Clear the current Success/Failed state so the row disappears. */
    fun dismiss() {
        _progress.value = null
    }

    private suspend fun run(request: UploadRequest, artistName: String, artistHandle: String) {
        val thumb = request.imageUri
        val priv = request.isPrivate

        // 1. Compress
        _progress.value = UploadProgress.Compressing(thumb, priv)
        val encoded = try {
            compressor.compress(request.imageUri)
        } catch (e: Exception) {
            _progress.value = UploadProgress.Failed(thumb, priv, retryable = false, message = "Couldn't read this image")
            return
        }

        // 2. Prepare upload (signed URL)
        val prepared = when (val r = repository.prepareUpload()) {
            is ApiResult.Success -> r.data
            is ApiResult.Error -> { _progress.value = r.toFailed(thumb, priv); return }
        }

        // 3. PUT bytes
        _progress.value = UploadProgress.Uploading(thumb, priv, percent = 0)
        val putResult = repository.uploadBytes(prepared.uploadUrl, encoded.jpeg) { sent, total ->
            val pct = if (total > 0) ((sent * 100) / total).toInt().coerceIn(0, 100) else 0
            _progress.value = UploadProgress.Uploading(thumb, priv, percent = pct)
        }
        if (putResult is ApiResult.Error) { _progress.value = putResult.toFailed(thumb, priv); return }

        // 4. Finalize
        _progress.value = UploadProgress.Finalizing(thumb, priv)
        when (val r = repository.createArtwork(prepared.filePath, request, encoded.aspectRatio, prepared.rekognitionTags)) {
            is ApiResult.Success -> _progress.value = UploadProgress.Success(
                localThumb = thumb,
                isPrivate = priv,
                artwork = r.data,
                title = request.title,
                artistName = artistName,
                artistHandle = artistHandle,
            )
            is ApiResult.Error -> _progress.value = r.toFailed(thumb, priv)
        }
    }

    private fun ApiResult.Error.toFailed(thumb: android.net.Uri, priv: Boolean): UploadProgress.Failed = when (this) {
        is ApiResult.Error.Network -> UploadProgress.Failed(thumb, priv, retryable = true, message = "No internet connection")
        is ApiResult.Error.Server -> UploadProgress.Failed(thumb, priv, retryable = true, message = "Server error — try again")
        is ApiResult.Error.RateLimited -> UploadProgress.Failed(thumb, priv, retryable = true, message = message)
        is ApiResult.Error.Validation -> UploadProgress.Failed(thumb, priv, retryable = false, message = message)
        is ApiResult.Error.Blocked -> UploadProgress.Failed(thumb, priv, retryable = false, message = message)
        is ApiResult.Error.NotFound -> UploadProgress.Failed(thumb, priv, retryable = false, message = message)
        is ApiResult.Error.Conflict -> UploadProgress.Failed(thumb, priv, retryable = false, message = message)
        is ApiResult.Error.Unknown -> UploadProgress.Failed(thumb, priv, retryable = true, message = "Something went wrong")
    }
}
