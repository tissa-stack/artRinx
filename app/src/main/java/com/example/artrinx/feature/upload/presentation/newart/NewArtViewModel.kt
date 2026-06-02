package com.example.artrinx.feature.upload.presentation.newart

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.feature.upload.domain.model.ArtFormState
import com.example.artrinx.feature.upload.domain.model.ArtistResult
import com.example.artrinx.feature.upload.domain.model.MockUploadData
import com.example.artrinx.feature.upload.domain.model.PrivacyOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewArtViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(ArtFormState())
    val state: StateFlow<ArtFormState> = _state.asStateFlow()

    // ── Form fields ───────────────────────────────────────────────────────────

    fun onImageSet(uri: Uri) = _state.update { it.copy(imageUri = uri) }

    fun onTitleChange(t: String) = _state.update {
        it.copy(title = t.take(40), isTitleError = false)
    }

    fun onDescriptionChange(d: String) = _state.update {
        it.copy(description = d.take(255), isDescriptionError = false)
    }

    fun onShopLinkChange(url: String) = _state.update { it.copy(shopLink = url) }

    // ── Artist ────────────────────────────────────────────────────────────────

    fun onArtistSearchQueryChange(q: String) = _state.update { it.copy(artistSearchQuery = q) }

    fun filteredArtists(): List<ArtistResult> {
        val q = _state.value.artistSearchQuery.trim().lowercase()
        return if (q.isEmpty()) emptyList()
        else MockUploadData.artists.filter {
            it.handle.lowercase().contains(q) || it.displayName.lowercase().contains(q)
        }
    }

    fun onArtistSelected(artist: ArtistResult) = _state.update {
        it.copy(selectedArtist = artist, artistSearchQuery = "")
    }

    // ── Medium ────────────────────────────────────────────────────────────────

    fun onShowMediumPicker()   = _state.update { it.copy(showMediumPicker = true) }
    fun onDismissMediumPicker() = _state.update { it.copy(showMediumPicker = false) }
    fun onMediumSelected(m: String) = _state.update {
        it.copy(selectedMedium = m, showMediumPicker = false)
    }

    // ── Tags ──────────────────────────────────────────────────────────────────

    fun onTagInputChange(t: String) = _state.update { it.copy(currentTagInput = t) }

    fun onAddTag() {
        val tag = _state.value.currentTagInput.trim().lowercase()
        if (tag.isNotEmpty() && tag !in _state.value.tags) {
            _state.update { it.copy(tags = it.tags + tag, currentTagInput = "") }
        } else {
            _state.update { it.copy(currentTagInput = "") }
        }
    }

    fun onRemoveTag(tag: String) = _state.update { it.copy(tags = it.tags - tag) }

    // ── Privacy ───────────────────────────────────────────────────────────────

    fun onShowPrivacyPicker()    = _state.update { it.copy(showPrivacyPicker = true) }
    fun onDismissPrivacyPicker() = _state.update { it.copy(showPrivacyPicker = false) }
    fun onPrivacySelected(p: PrivacyOption) = _state.update {
        it.copy(privacy = p, showPrivacyPicker = false)
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    fun validate(): Boolean {
        val titleEmpty = _state.value.title.isEmpty()
        _state.update { it.copy(isTitleError = titleEmpty) }
        return !titleEmpty
    }

    fun onUpload() {
        if (!validate()) return
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true) }
            delay(1500)
            _state.update { it.copy(isUploading = false) }
        }
    }
}
