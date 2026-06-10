package com.rinx.artRINXapp.feature.upload.domain

import com.rinx.artRINXapp.core.di.ApplicationScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.upload.domain.model.CreateCurationRequest
import com.rinx.artRINXapp.feature.upload.domain.model.CurationProgress
import com.rinx.artRINXapp.feature.upload.domain.repository.CurationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped orchestrator for curation creation (a single JSON POST referencing existing
 * artworks). Runs on the process-lifetime [ApplicationScope] so it survives navigating away from
 * the create screen. Home (public) and Profile (private) observe [progress] to render the
 * "Creating…" row and optimistically insert the result. Mirrors [UploadManager].
 */
@Singleton
class CurationManager @Inject constructor(
    private val repository: CurationRepository,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val _progress = MutableStateFlow<CurationProgress?>(null)
    val progress: StateFlow<CurationProgress?> = _progress.asStateFlow()

    private var job: Job? = null
    private var lastRequest: CreateCurationRequest? = null

    /**
     * Fire-and-forget. Returns `false` (and does nothing) if a create is already running. On accept,
     * clears any stale terminal so this run emits a fresh `null → Creating → Success` sequence
     * (mirrors [UploadManager]).
     */
    fun enqueue(request: CreateCurationRequest): Boolean {
        if (job?.isActive == true) return false
        lastRequest = request
        _progress.value = null
        job = scope.launch { run(request) }
        return true
    }

    fun retry() {
        val req = lastRequest ?: return
        enqueue(req)
    }

    fun dismiss() {
        _progress.value = null
    }

    private suspend fun run(request: CreateCurationRequest) {
        val thumb = request.artworkUrls.firstOrNull()
        val priv = request.isPrivate

        _progress.value = CurationProgress.Creating(thumb, priv)
        when (val r = repository.createCuration(request)) {
            is ApiResult.Success -> _progress.value = CurationProgress.Success(
                thumbnail = thumb,
                isPrivate = priv,
                curation = r.data,
                title = request.title,
                artworkUrls = request.artworkUrls,
            )
            is ApiResult.Error -> _progress.value = r.toFailed(thumb, priv)
        }
    }

    private fun ApiResult.Error.toFailed(thumb: String?, priv: Boolean): CurationProgress.Failed = when (this) {
        is ApiResult.Error.Network -> CurationProgress.Failed(thumb, priv, retryable = true, message = "No internet connection")
        is ApiResult.Error.Server -> CurationProgress.Failed(thumb, priv, retryable = true, message = "Server error — try again")
        is ApiResult.Error.RateLimited -> CurationProgress.Failed(thumb, priv, retryable = true, message = message)
        is ApiResult.Error.Validation -> CurationProgress.Failed(thumb, priv, retryable = false, message = message)
        is ApiResult.Error.Blocked -> CurationProgress.Failed(thumb, priv, retryable = false, message = message)
        is ApiResult.Error.NotFound -> CurationProgress.Failed(thumb, priv, retryable = false, message = message)
        is ApiResult.Error.Conflict -> CurationProgress.Failed(thumb, priv, retryable = false, message = message)
        is ApiResult.Error.Unknown -> CurationProgress.Failed(thumb, priv, retryable = true, message = "Something went wrong")
    }
}
