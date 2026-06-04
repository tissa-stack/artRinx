package com.example.artrinx.feature.upload.presentation.curation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.upload.domain.CurationManager
import com.example.artrinx.feature.upload.domain.model.ArtTab
import com.example.artrinx.feature.upload.domain.model.CreateCurationRequest
import com.example.artrinx.feature.upload.domain.model.NewCurationState
import com.example.artrinx.feature.upload.domain.model.PrivacyOption
import com.example.artrinx.feature.upload.domain.model.UserArtItem
import com.example.artrinx.feature.upload.domain.repository.CurationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewCurationViewModel @Inject constructor(
    private val curationRepository: CurationRepository,
    private val curationManager: CurationManager,
) : ViewModel() {

    private val _state = MutableStateFlow(NewCurationState())
    val state: StateFlow<NewCurationState> = _state.asStateFlow()

    init {
        loadSelectionSources()
    }

    private fun loadSelectionSources() {
        viewModelScope.launch {
            val uploads = curationRepository.getMyArtworks(PAGE, SIZE)
            if (uploads is ApiResult.Success) {
                _state.update { it.copy(uploadedArts = it.applySelection(uploads.data)) }
            }
        }
        viewModelScope.launch {
            val liked = curationRepository.getLikedArtworks(PAGE, SIZE)
            if (liked is ApiResult.Success) {
                _state.update { it.copy(likedArts = it.applySelection(liked.data)) }
            }
        }
    }

    /** Preserve current selection flags when a freshly-loaded list arrives. */
    private fun NewCurationState.applySelection(items: List<UserArtItem>): List<UserArtItem> =
        items.map { item -> item.copy(isSelected = selectedArts.any { it.id == item.id }) }

    fun onTitleChange(t: String) = _state.update { it.copy(title = t.take(40)) }
    fun onDescriptionChange(d: String) = _state.update { it.copy(description = d.take(255)) }

    // ── Art selection ─────────────────────────────────────────────────────────

    fun onTabSelected(tab: ArtTab) = _state.update { it.copy(activeArtTab = tab) }

    fun onToggleArtSelection(item: UserArtItem) {
        _state.update { state ->
            val alreadySelected = state.selectedArts.any { it.id == item.id }
            val updated = if (alreadySelected) {
                state.selectedArts.filter { it.id != item.id }
            } else {
                state.selectedArts + item
            }
            // Update the displayed lists' isSelected flags
            val updatedUploads = state.uploadedArts.map { a ->
                a.copy(isSelected = updated.any { it.id == a.id })
            }
            val updatedLiked = state.likedArts.map { a ->
                a.copy(isSelected = updated.any { it.id == a.id })
            }
            state.copy(
                selectedArts  = updated,
                uploadedArts  = updatedUploads,
                likedArts     = updatedLiked,
                previewIndex  = 0,
            )
        }
    }

    fun onPreviewPrev() = _state.update {
        val newIdx = (it.previewIndex - 1).coerceAtLeast(0)
        it.copy(previewIndex = newIdx)
    }

    fun onPreviewNext() = _state.update {
        val newIdx = (it.previewIndex + 1).coerceAtMost(it.selectedArts.size - 1)
        it.copy(previewIndex = newIdx)
    }

    // ── Privacy ───────────────────────────────────────────────────────────────

    fun onShowPrivacyPicker()    = _state.update { it.copy(showPrivacyPicker = true) }
    fun onDismissPrivacyPicker() = _state.update { it.copy(showPrivacyPicker = false) }
    fun onPrivacySelected(p: PrivacyOption) = _state.update {
        it.copy(privacy = p, showPrivacyPicker = false)
    }

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Hands the create off to the app-scoped [CurationManager] (survives navigation) and returns
     * true if it was enqueued.
     */
    fun onCreate(): Boolean {
        val s = _state.value
        if (!s.isValid) return false
        val artworkIds = s.selectedArts.mapNotNull { it.artworkId }
        if (artworkIds.isEmpty()) return false
        curationManager.enqueue(
            CreateCurationRequest(
                title = s.title,
                description = s.description.ifBlank { null },
                isPrivate = s.privacy == PrivacyOption.PRIVATE,
                artworkIds = artworkIds,
                artworkUrls = s.selectedArts.mapNotNull { it.imageUrl },
            ),
        )
        return true
    }

    private companion object {
        const val PAGE = 1
        const val SIZE = 50
    }
}
