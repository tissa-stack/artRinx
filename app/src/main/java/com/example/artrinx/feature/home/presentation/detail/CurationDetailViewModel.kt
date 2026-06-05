package com.example.artrinx.feature.home.presentation.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.home.data.local.CurationPreviewStore
import com.example.artrinx.feature.home.domain.model.CurationItem
import com.example.artrinx.feature.home.domain.repository.HomeRepository
import com.example.artrinx.feature.profile.domain.repository.ProfileRepository
import com.example.artrinx.feature.upload.domain.EditTargetStore
import com.example.artrinx.feature.upload.domain.repository.CurationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class CurationDetailUiState(
    val curation: CurationItem? = null,
    val moreLikeThis: List<CurationItem> = emptyList(),
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val isLoading: Boolean = true,
    val error: Boolean = false,
    /** True when the current user owns this curation → show Edit/Delete instead of Report. */
    val isOwn: Boolean = false,
    val isDeleting: Boolean = false,
    // ── Report / block (moderation) ──
    val isReporting: Boolean = false,
    val reportSent: Boolean = false,
    val isBlocking: Boolean = false,
    val actionError: String? = null,
)

@HiltViewModel
class CurationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HomeRepository,
    private val curationPreviewStore: CurationPreviewStore,
    private val profileRepository: ProfileRepository,
    private val curationRepository: CurationRepository,
    private val editTargetStore: EditTargetStore,
) : ViewModel() {

    private val curationId: Int? = savedStateHandle.get<String>("curationId")?.toIntOrNull()
    private val source: String? = savedStateHandle.get<String>("source")
    private val isFromProfile: Boolean = source == "profile"

    private val _uiState = MutableStateFlow(CurationDetailUiState())
    val uiState: StateFlow<CurationDetailUiState> = _uiState.asStateFlow()

    /** One-shot: emitted after a successful delete so the screen can pop back. */
    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted = _deleted.receiveAsFlow()

    /** One-shot: emitted after a successful block so the screen can close the sheet and pop back. */
    private val _blocked = Channel<Unit>(Channel.BUFFERED)
    val blocked = _blocked.receiveAsFlow()

    init {
        load()
    }

    private fun load() {
        val id = curationId
        if (id == null) {
            _uiState.update { it.copy(isLoading = false, error = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = false) }
            val detailJob = async { repository.getCurationDetail(id) }
            // Opened from Profile → no "More like this" (clean preview).
            val moreJob = if (isFromProfile) null else async { repository.getMoreCurations() }
            val meJob = async { profileRepository.getMyProfile() }
            val detailRes = detailJob.await()
            val moreRes = moreJob?.await()
            val currentUserId = (meJob.await() as? ApiResult.Success)?.data?.id

            if (detailRes is ApiResult.Success) {
                val fetched = detailRes.data
                // Open with the SAME first images as the home preview deck (matched by URL),
                // then the curation's remaining artworks in their own order.
                val curation = fetched.copy(
                    artworkUrls = reorderByPreview(
                        detailUrls = fetched.artworkUrls,
                        previewUrls = curationPreviewStore.orderFor(fetched.id),
                    ),
                )
                val more = (moreRes as? ApiResult.Success)?.data.orEmpty()
                    .filter { it.id != curation.id }
                // Remember the previews of the "More like this" curations too, so tapping one
                // opens it with the same first images.
                more.forEach { curationPreviewStore.put(it.id, it.artworkUrls) }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = false,
                        curation = curation,
                        moreLikeThis = more,
                        likeCount = curation.likeCount,
                        isLiked = curation.isLiked,
                        isOwn = currentUserId != null && curation.authorId == currentUserId,
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = true) }
            }
        }
    }

    /** Stage this curation for the edit flow before navigating to the New Curation screen. */
    fun prepareEdit() {
        curationId?.let { editTargetStore.setCuration(it) }
    }

    fun deleteCuration() {
        val id = curationId ?: return
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            when (curationRepository.deleteCuration(id)) {
                is ApiResult.Success -> _deleted.send(Unit)
                is ApiResult.Error -> _uiState.update { it.copy(isDeleting = false) }
            }
        }
    }

    /**
     * Puts the [previewUrls] that exist in [detailUrls] first (in preview order), then the rest of
     * [detailUrls] in their own order. Falls back to the detail's natural order if no preview.
     */
    private fun reorderByPreview(detailUrls: List<String>, previewUrls: List<String>): List<String> {
        if (previewUrls.isEmpty()) return detailUrls
        val detailSet = detailUrls.toHashSet()
        val front = previewUrls.filter { it in detailSet }
        if (front.isEmpty()) return detailUrls
        val frontSet = front.toHashSet()
        return front + detailUrls.filterNot { it in frontSet }
    }

    fun onLikeToggled() {
        val id = curationId ?: return
        val nowLiked = !_uiState.value.isLiked
        setLiked(nowLiked)
        viewModelScope.launch {
            val result = if (nowLiked) repository.likeCuration(id) else repository.unlikeCuration(id)
            if (result is ApiResult.Error) setLiked(!nowLiked)   // revert on failure
        }
    }

    private fun setLiked(liked: Boolean) {
        _uiState.update { state ->
            state.copy(
                isLiked = liked,
                likeCount = (state.likeCount + if (liked) 1 else -1).coerceAtLeast(0),
            )
        }
    }

    // ── Report / block ────────────────────────────────────────────────────────

    fun submitReport(message: String) {
        val id = curationId ?: return
        if (_uiState.value.isReporting) return
        _uiState.update { it.copy(isReporting = true, actionError = null) }
        viewModelScope.launch {
            when (profileRepository.reportCuration(id, message)) {
                is ApiResult.Success -> _uiState.update { it.copy(isReporting = false, reportSent = true) }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isReporting = false, actionError = "Couldn't send the report. Please try again.")
                }
            }
        }
    }

    fun blockUser() {
        val ownerId = _uiState.value.curation?.authorId ?: return
        if (_uiState.value.isBlocking) return
        _uiState.update { it.copy(isBlocking = true, actionError = null) }
        viewModelScope.launch {
            when (profileRepository.blockUser(ownerId)) {
                is ApiResult.Success -> _blocked.send(Unit)
                is ApiResult.Error -> _uiState.update {
                    it.copy(isBlocking = false, actionError = "Couldn't block this user. Please try again.")
                }
            }
        }
    }

    fun onReportSheetClosed() = _uiState.update {
        it.copy(isReporting = false, reportSent = false, isBlocking = false, actionError = null)
    }

    fun onActionErrorShown() = _uiState.update { it.copy(actionError = null) }
}
