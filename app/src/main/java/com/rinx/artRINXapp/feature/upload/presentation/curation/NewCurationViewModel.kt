package com.rinx.artRINXapp.feature.upload.presentation.curation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.upload.domain.CurationManager
import com.rinx.artRINXapp.feature.upload.domain.CurationSeedStore
import com.rinx.artRINXapp.feature.upload.domain.EditTargetStore
import com.rinx.artRINXapp.feature.upload.domain.model.ArtTab
import com.rinx.artRINXapp.feature.upload.domain.model.CreateCurationRequest
import com.rinx.artRINXapp.feature.upload.domain.model.CreationStatus
import com.rinx.artRINXapp.feature.upload.domain.model.CurationProgress
import com.rinx.artRINXapp.feature.upload.domain.model.NewCurationState
import com.rinx.artRINXapp.feature.upload.domain.model.PrivacyOption
import com.rinx.artRINXapp.feature.upload.domain.model.UserArtItem
import com.rinx.artRINXapp.feature.upload.domain.repository.CurationRepository
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
    seedStore: CurationSeedStore,
    editTargetStore: EditTargetStore,
) : ViewModel() {

    private val _state = MutableStateFlow(NewCurationState())
    val state: StateFlow<NewCurationState> = _state.asStateFlow()

    private var awaitingPrivate = false

    init {
        // Preselect any artworks staged by the "Add to curation → Create Curation" flow.
        val seed = seedStore.consume()
        if (seed.isNotEmpty()) {
            _state.update { it.copy(selectedArts = seed) }
        }
        loadSelectionSources()
        observePrivateCreate()
        editTargetStore.consumeCuration()?.let { loadForEdit(it) }
    }

    /** Prefill the form from an existing curation when entering edit mode. */
    private fun loadForEdit(id: Int) {
        // Synchronous so the loader shows immediately (before the fetch coroutine runs).
        _state.update { it.copy(isLoadingEdit = true) }
        viewModelScope.launch {
            val result = curationRepository.getCurationForEdit(id)
            if (result is ApiResult.Success) {
                val c = result.data
                val selected = c.arts.map { it.copy(isSelected = true) }
                _state.update { state ->
                    state.copy(
                        editCurationId = id,
                        title = c.title,
                        description = c.description.orEmpty(),
                        privacy = if (c.isPrivate) PrivacyOption.PRIVATE else PrivacyOption.PUBLIC,
                        selectedArts = selected,
                        uploadedArts = state.applySelectionTo(state.uploadedArts, selected),
                        likedArts = state.applySelectionTo(state.likedArts, selected),
                        isLoadingEdit = false,
                    )
                }
            } else {
                // Prefill failed (e.g. the curation was deleted server-side) → flag so the screen
                // toasts + pops instead of showing a blank, un-saveable edit form.
                _state.update { it.copy(isLoadingEdit = false, editLoadFailed = true) }
            }
        }
    }

    private fun NewCurationState.applySelectionTo(
        items: List<UserArtItem>,
        selected: List<UserArtItem>,
    ): List<UserArtItem> = items.map { item -> item.copy(isSelected = selected.any { it.id == item.id }) }

    /** While a PRIVATE curation is being created, mirror the manager's progress into overlay state. */
    private fun observePrivateCreate() {
        viewModelScope.launch {
            curationManager.progress.collect { p ->
                if (!awaitingPrivate) return@collect
                when (p) {
                    is CurationProgress.Failed ->
                        _state.update { it.copy(creationStatus = CreationStatus.FAILED, creationError = p.message) }
                    is CurationProgress.Success ->
                        _state.update { it.copy(creationStatus = CreationStatus.CREATED, creationError = null) }
                    null -> {}
                    else ->
                        _state.update { it.copy(creationStatus = CreationStatus.LOADING) }
                }
            }
        }
    }

    fun onCreationDone() {
        awaitingPrivate = false
        // Clear the manager's consumed terminal so the next create starts from a clean flow.
        curationManager.dismiss()
        _state.update { it.copy(creationStatus = null, creationError = null) }
    }

    fun onRetryCreation() = curationManager.retry()

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

    /** Remove the selected art at [index] in the preview deck (edit mode). Persisted on Save. */
    fun onRemoveArtAt(index: Int) {
        _state.update { state ->
            val target = state.selectedArts.getOrNull(index) ?: return@update state
            val updated = state.selectedArts.filterNot { it.id == target.id }
            state.copy(
                selectedArts = updated,
                uploadedArts = state.uploadedArts.map { a -> a.copy(isSelected = updated.any { it.id == a.id }) },
                likedArts    = state.likedArts.map { a -> a.copy(isSelected = updated.any { it.id == a.id }) },
                previewIndex = 0,
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
        // A curation may be created with no artworks (added later) — empty list is allowed.
        val artworkIds = s.selectedArts.mapNotNull { it.artworkId }
        val isPrivate = s.privacy == PrivacyOption.PRIVATE
        // Private creations stay on this screen and show the overlay instead of navigating.
        if (isPrivate) {
            awaitingPrivate = true
            _state.update { it.copy(creationStatus = CreationStatus.LOADING) }
        }
        val started = curationManager.enqueue(
            CreateCurationRequest(
                title = s.title,
                description = s.description.ifBlank { null },
                isPrivate = isPrivate,
                artworkIds = artworkIds,
                artworkUrls = s.selectedArts.mapNotNull { it.imageUrl },
            ),
        )
        // Already creating → nothing enqueued; undo the optimistic LOADING so it doesn't spin forever.
        if (!started && isPrivate) {
            awaitingPrivate = false
            _state.update { it.copy(creationStatus = null) }
        }
        return started
    }

    /**
     * Edit mode: PUT the curation changes for [NewCurationState.editCurationId] and drive the
     * overlay (LOADING → CREATED / FAILED).
     */
    fun onSaveEdit() {
        val s = _state.value
        if (!s.isValid) return
        val id = s.editCurationId ?: return
        // Empty list is allowed — the user may remove every artwork from a collection.
        val artworkIds = s.selectedArts.mapNotNull { it.artworkId }
        _state.update { it.copy(creationStatus = CreationStatus.LOADING, creationError = null) }
        viewModelScope.launch {
            val result = curationRepository.updateCuration(
                id = id,
                title = s.title,
                description = s.description.ifBlank { null },
                isPrivate = s.privacy == PrivacyOption.PRIVATE,
                artworkIds = artworkIds,
            )
            when (result) {
                is ApiResult.Success ->
                    _state.update { it.copy(creationStatus = CreationStatus.CREATED) }
                is ApiResult.Error ->
                    _state.update { it.copy(creationStatus = CreationStatus.FAILED, creationError = "Couldn't save changes — please try again.") }
            }
        }
    }

    fun onRetryEdit() = onSaveEdit()

    private companion object {
        const val PAGE = 1
        const val SIZE = 50
    }
}
