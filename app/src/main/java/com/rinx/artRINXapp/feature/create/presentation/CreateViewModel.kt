package com.rinx.artRINXapp.feature.create.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.profile.domain.model.UploadQuota
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import com.rinx.artRINXapp.feature.upload.domain.CurationManager
import com.rinx.artRINXapp.feature.upload.domain.UploadManager
import com.rinx.artRINXapp.feature.upload.domain.model.CurationProgress
import com.rinx.artRINXapp.feature.upload.domain.model.UploadProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Success rows linger this long so the user sees "Uploaded"/"Created" before the row disappears. */
private const val SUCCESS_AUTO_DISMISS_MS = 2_000L

/**
 * Backs the Create tab's "Upload Art" gate (handout §Upload tap handler). Loads the upload quota on
 * entry and resolves the role×plan branch when the user taps Upload Art.
 *
 * It is ALSO the owner of the PUBLIC upload/curation progress row: after a public upload/collection is
 * enqueued the user lands back here (not Home), so this VM observes the shared manager flows, surfaces
 * the row, and — as the single dismiss owner for public items — clears the terminal state (see the
 * shared-progress convention: only one owner per emission may dismiss(), or a slower collector gets
 * starved of the terminal state). Home stays passive (feed insert only); private items keep their
 * form overlay + Profile insert and never appear here (we filter to public only).
 */
@HiltViewModel
class CreateViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val uploadManager: UploadManager,
    private val curationManager: CurationManager,
) : ViewModel() {

    /** The handout's four upload outcomes, plus a transient Loading. */
    enum class UploadAction { OPEN_PICKER, LIMIT_REACHED, GALLERY_WEB, UPGRADE_REQUIRED, LOADING }

    data class State(
        val quota: UploadQuota? = null,
        val uploadLimitText: String = "",
        /** In-progress/just-finished PUBLIC upload, surfaced as a row under the upload-limit card. */
        val uploadProgress: UploadProgress? = null,
        /** In-progress/just-finished PUBLIC curation create, surfaced under the upload row. */
        val curationProgress: CurationProgress? = null,
    )

    // Seed synchronously from the cached quota so re-entering Create shows the real count immediately
    // (no flicker/reset); falls back to the Basic-plan default until the very first fetch lands.
    private val _state = MutableStateFlow(seedState())
    val state: StateFlow<State> = _state.asStateFlow()

    // Pending "clear the success row after a beat" jobs. Cancelled on every new emission so a fresh
    // upload (enqueue() nulls the flow first) can never be nuked by a stale scheduled dismiss.
    private var uploadDismissJob: Job? = null
    private var curationDismissJob: Job? = null

    private fun seedState(): State {
        val cached = profileRepository.cachedUploadQuota()
        return State(quota = cached, uploadLimitText = cached?.label ?: "0 / 10 uploads")
    }

    init {
        refresh()
        observeUploads()
        observeCurations()
    }

    fun refresh() {
        viewModelScope.launch {
            val result = profileRepository.getUploadQuota()
            if (result is ApiResult.Success) {
                val q = result.data
                _state.update { it.copy(quota = q, uploadLimitText = q.label) }
            }
        }
    }

    // ── Public upload progress (Create owns the row + dismiss) ──────────────────

    private fun observeUploads() {
        viewModelScope.launch {
            uploadManager.progress.collect { progress ->
                // Private uploads keep their form overlay + Profile insert — never a row here.
                val forCreate = progress?.takeUnless { it.isPrivate }
                // Any new emission supersedes a pending auto-dismiss (e.g. the null a fresh enqueue emits).
                uploadDismissJob?.cancel()
                uploadDismissJob = null

                _state.update { it.copy(uploadProgress = forCreate) }

                if (forCreate is UploadProgress.Success) {
                    // The upload succeeded → the server count changed; refresh so "X / N uploads" is live.
                    refresh()
                    uploadDismissJob = viewModelScope.launch {
                        delay(SUCCESS_AUTO_DISMISS_MS)
                        uploadManager.dismiss()
                    }
                }
            }
        }
    }

    fun onRetryUpload() = uploadManager.retry()
    fun onDismissUpload() = uploadManager.dismiss()

    // ── Public curation progress ────────────────────────────────────────────────

    private fun observeCurations() {
        viewModelScope.launch {
            curationManager.progress.collect { progress ->
                val forCreate = progress?.takeUnless { it.isPrivate }
                curationDismissJob?.cancel()
                curationDismissJob = null

                _state.update { it.copy(curationProgress = forCreate) }

                if (forCreate is CurationProgress.Success) {
                    refresh()
                    curationDismissJob = viewModelScope.launch {
                        delay(SUCCESS_AUTO_DISMISS_MS)
                        curationManager.dismiss()
                    }
                }
            }
        }
    }

    fun onRetryCuration() = curationManager.retry()
    fun onDismissCuration() = curationManager.dismiss()

    private val UploadQuota.label: String get() = "$artworkCount / $maxUploads uploads"

    /** Resolve the Upload-Art tap branch. Degrades to OPEN_PICKER when the quota hasn't loaded
     * (the backend re-checks on POST, so client gating is UX-only — never a hard security gate). */
    fun resolveUploadAction(): UploadAction {
        val q = _state.value.quota ?: return UploadAction.OPEN_PICKER
        return when {
            // Gallery is a web-billed (Stripe) plan: uploads are managed on artrinx.com, never via
            // an in-app purchase (Apple anti-steering). Keep redirecting Gallery-role users to web.
            q.isGallery -> UploadAction.GALLERY_WEB
            // Every logged-in user is at least a Basic user, which the plan grants 10 uploads. Allow
            // uploading up to the plan cap (10 Basic / 99 paid) and only block once it's reached —
            // no upgrade paywall before the limit.
            q.limitReached -> UploadAction.LIMIT_REACHED
            else -> UploadAction.OPEN_PICKER
        }
    }
}
