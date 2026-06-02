package com.example.artrinx.feature.upload.presentation.curation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.feature.upload.domain.model.ArtTab
import com.example.artrinx.feature.upload.domain.model.MockUploadData
import com.example.artrinx.feature.upload.domain.model.NewCurationState
import com.example.artrinx.feature.upload.domain.model.PrivacyOption
import com.example.artrinx.feature.upload.domain.model.UserArtItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewCurationViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(NewCurationState())
    val state: StateFlow<NewCurationState> = _state.asStateFlow()

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
            // Update the displayed list's isSelected flags
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

    fun onCreate() {
        if (!_state.value.isValid) return
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true) }
            delay(1500)
            _state.update { it.copy(isCreating = false) }
        }
    }
}
