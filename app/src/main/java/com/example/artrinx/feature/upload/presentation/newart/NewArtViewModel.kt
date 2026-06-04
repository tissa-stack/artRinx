package com.example.artrinx.feature.upload.presentation.newart

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.profile.domain.repository.ProfileRepository
import com.example.artrinx.feature.search.domain.repository.SearchRepository
import com.example.artrinx.feature.search.domain.model.UserSearchItem
import com.example.artrinx.feature.upload.domain.UploadManager
import com.example.artrinx.feature.upload.domain.model.ArtFormState
import com.example.artrinx.feature.upload.domain.model.ArtistResult
import com.example.artrinx.feature.upload.domain.model.MediumOption
import com.example.artrinx.feature.upload.domain.model.PrivacyOption
import com.example.artrinx.feature.upload.domain.model.UploadRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewArtViewModel @Inject constructor(
    private val uploadManager: UploadManager,
    private val profileRepository: ProfileRepository,
    private val searchRepository: SearchRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ArtFormState())
    val state: StateFlow<ArtFormState> = _state.asStateFlow()

    private var artistSearchJob: Job? = null

    init {
        loadMediums()
        loadTrendingTags()
        loadSelfArtist()
    }

    private fun loadSelfArtist() {
        viewModelScope.launch {
            val result = profileRepository.getMyProfile()
            if (result is ApiResult.Success) {
                val me = result.data
                _state.update {
                    it.copy(
                        selfArtist = ArtistResult(
                            handle = me.username,
                            displayName = me.displayName,
                            subtitle = "myself",
                            userId = me.id,
                            avatarUrl = me.avatarUrl,
                        ),
                    )
                }
            }
        }
    }

    private fun loadMediums() {
        viewModelScope.launch {
            val result = profileRepository.getMediums()
            if (result is ApiResult.Success) {
                _state.update { it.copy(mediums = result.data.map { m -> MediumOption(m.id, m.title) }) }
            }
        }
    }

    private fun loadTrendingTags() {
        viewModelScope.launch {
            val result = searchRepository.getTrendingTags()
            if (result is ApiResult.Success) {
                _state.update { it.copy(tagSuggestions = result.data) }
            }
        }
    }

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

    fun onArtistSearchQueryChange(q: String) {
        _state.update { it.copy(artistSearchQuery = q) }
        artistSearchJob?.cancel()
        val query = q.trim()
        if (query.isEmpty()) {
            _state.update { it.copy(artistResults = emptyList()) }
            return
        }
        artistSearchJob = viewModelScope.launch {
            delay(300) // debounce
            val result = searchRepository.searchUsers(query)
            if (result is ApiResult.Success) {
                val selfId = _state.value.selfArtist?.userId
                _state.update {
                    it.copy(artistResults = result.data.map { u -> u.toArtistResult() }.filter { a -> a.userId != selfId })
                }
            }
        }
    }

    fun onArtistSelected(artist: ArtistResult) = _state.update {
        it.copy(selectedArtist = artist, artistSearchQuery = "", artistResults = emptyList())
    }

    /** "Add artist without RINX profile" → upload with no artist attribution. */
    fun onClearArtist() = _state.update {
        it.copy(selectedArtist = null, artistSearchQuery = "", artistResults = emptyList())
    }

    private fun UserSearchItem.toArtistResult(): ArtistResult = ArtistResult(
        handle = username,
        displayName = displayName,
        subtitle = profileTypeName.ifBlank { "@$username" },
        userId = id.toIntOrNull(),
        avatarUrl = profilePictureUrl,
    )

    // ── Medium ────────────────────────────────────────────────────────────────

    fun onShowMediumPicker()   = _state.update { it.copy(showMediumPicker = true) }
    fun onDismissMediumPicker() = _state.update { it.copy(showMediumPicker = false) }
    fun onMediumSelected(m: MediumOption) = _state.update {
        it.copy(selectedMedium = m.title, selectedMediumId = m.id, showMediumPicker = false)
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

    /** Add a suggested (trending) tag from the suggestions row. */
    fun onSuggestedTagTap(tag: String) {
        val normalized = tag.trim().lowercase()
        if (normalized.isNotEmpty() && normalized !in _state.value.tags) {
            _state.update { it.copy(tags = it.tags + normalized) }
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

    /**
     * Hands the upload off to the app-scoped [UploadManager] (so it survives navigation) and
     * returns true if it was enqueued. The artwork is always owned by the authenticated user.
     */
    fun onUpload(): Boolean {
        if (!validate()) return false
        val s = _state.value
        val uri = s.imageUri ?: return false
        val artist = s.selectedArtist
        uploadManager.enqueue(
            UploadRequest(
                imageUri = uri,
                title = s.title,
                description = s.description.ifBlank { null },
                tags = s.tags,
                mediumId = s.selectedMediumId,
                shopLink = s.shopLink.ifBlank { null },
                price = null,
                isPrivate = s.privacy == PrivacyOption.PRIVATE,
                artistId = artist?.userId,
                artistName = artist?.displayName,
            ),
            artistName = artist?.displayName.orEmpty(),
            artistHandle = artist?.handle?.let { "@$it" }.orEmpty(),
        )
        return true
    }
}
