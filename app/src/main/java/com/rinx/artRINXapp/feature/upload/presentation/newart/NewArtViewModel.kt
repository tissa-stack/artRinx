package com.rinx.artRINXapp.feature.upload.presentation.newart

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import com.rinx.artRINXapp.feature.search.domain.repository.SearchRepository
import com.rinx.artRINXapp.feature.search.domain.model.UserSearchItem
import com.rinx.artRINXapp.feature.upload.domain.EditTargetStore
import com.rinx.artRINXapp.feature.upload.domain.UploadManager
import com.rinx.artRINXapp.feature.upload.domain.model.ArtFormState
import com.rinx.artRINXapp.feature.upload.domain.model.MAX_ARTWORK_TAGS
import com.rinx.artRINXapp.feature.upload.domain.model.ArtistResult
import com.rinx.artRINXapp.feature.upload.domain.model.CreationStatus
import com.rinx.artRINXapp.feature.upload.domain.model.MediumOption
import com.rinx.artRINXapp.feature.upload.domain.model.PrivacyOption
import com.rinx.artRINXapp.feature.upload.domain.model.ShopLinkVisibility
import com.rinx.artRINXapp.feature.upload.domain.model.UpdateArtworkRequest
import com.rinx.artRINXapp.feature.upload.domain.model.UploadProgress
import com.rinx.artRINXapp.feature.upload.domain.model.UploadRequest
import com.rinx.artRINXapp.feature.upload.domain.repository.UploadRepository
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
    private val uploadRepository: UploadRepository,
    editTargetStore: EditTargetStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ArtFormState())
    val state: StateFlow<ArtFormState> = _state.asStateFlow()

    private var artistSearchJob: Job? = null
    private var awaitingPrivate = false

    init {
        loadMediums()
        loadTrendingTags()
        loadSelfArtist()
        loadShopLinkVisibility()
        observePrivateUpload()
        editTargetStore.consumeArtwork()?.let { loadForEdit(it) }
    }

    /** Resolve shop-link field gating from the user's role×plan (handout §Field gating). */
    private fun loadShopLinkVisibility() {
        viewModelScope.launch {
            val result = profileRepository.getUploadQuota()
            if (result is ApiResult.Success) {
                val q = result.data
                _state.update {
                    it.copy(shopLinkVisibility = ShopLinkVisibility.resolve(q.role, q.isPaid))
                }
            }
        }
    }

    /** Prefill the form from an existing artwork when entering edit mode. */
    private fun loadForEdit(id: Int) {
        // Synchronous so the loader shows immediately (before the fetch coroutine runs).
        _state.update { it.copy(isLoadingEdit = true) }
        viewModelScope.launch {
            val result = uploadRepository.getArtworkForEdit(id)
            if (result is ApiResult.Success) {
                val a = result.data
                _state.update {
                    it.copy(
                        editArtworkId = id,
                        imageUrl = a.imageUrl,
                        title = a.title,
                        description = a.description.orEmpty(),
                        tags = a.tags,
                        selectedMediumId = a.mediumId,
                        selectedMedium = a.mediumTitle,
                        shopLink = a.shopLink.orEmpty(),
                        price = a.price?.let { p -> if (p % 1.0 == 0.0) p.toLong().toString() else p.toString() }.orEmpty(),
                        sizeHeightCm = a.sizeHeightCm.orEmpty(),
                        sizeWidthCm = a.sizeWidthCm.orEmpty(),
                        privacy = if (a.isPrivate) PrivacyOption.PRIVATE else PrivacyOption.PUBLIC,
                        // Restore the artist when either an id or a (no-profile) name exists.
                        selectedArtist = if (a.artistId != null || !a.artistName.isNullOrBlank()) {
                            ArtistResult(
                                handle = "",
                                displayName = a.artistName.orEmpty(),
                                subtitle = if (a.artistId != null) a.artistName.orEmpty() else "No artRINX profile",
                                userId = a.artistId,
                            )
                        } else {
                            null
                        },
                        isLoadingEdit = false,
                    )
                }
            } else {
                // Prefill failed (e.g. the artwork was deleted server-side) → don't strand the user on
                // a blank, un-saveable form; flag so the screen toasts + pops back.
                _state.update { it.copy(isLoadingEdit = false, editLoadFailed = true) }
            }
        }
    }

    /** While a PRIVATE upload is in flight, mirror the manager's progress into the overlay state. */
    private fun observePrivateUpload() {
        viewModelScope.launch {
            uploadManager.progress.collect { p ->
                if (!awaitingPrivate) return@collect
                when (p) {
                    is UploadProgress.Failed ->
                        _state.update { it.copy(creationStatus = CreationStatus.FAILED, creationError = p.message) }
                    is UploadProgress.Success ->
                        _state.update { it.copy(creationStatus = CreationStatus.CREATED, creationError = null) }
                    null -> {} // keep latched state
                    else ->
                        _state.update { it.copy(creationStatus = CreationStatus.LOADING) }
                }
            }
        }
    }

    fun onCreationDone() {
        awaitingPrivate = false
        // Clear the manager's consumed terminal so the next upload starts from a clean flow
        // (otherwise a lingering Success/Failed can confuse the next private overlay).
        uploadManager.dismiss()
        _state.update { it.copy(creationStatus = null, creationError = null) }
    }

    fun onRetryCreation() = uploadManager.retry()

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

    fun onShopLinkChange(url: String) = _state.update {
        // Clearing the shop link removes the price requirement, so drop any stale price error.
        it.copy(shopLink = url, isPriceError = false)
    }

    /** Price input; digits + a single decimal point only. Sent only when a shop link is present. */
    fun onPriceChange(p: String) = _state.update {
        it.copy(price = p.filter { c -> c.isDigit() || c == '.' }.take(12), isPriceError = false)
    }

    /** Optional height (cm); digits + a single decimal point only. Never required. */
    fun onSizeHeightChange(h: String) = _state.update {
        it.copy(sizeHeightCm = h.filter { c -> c.isDigit() || c == '.' }.take(8))
    }

    /** Optional width; digits + a single decimal point only. Never required. */
    fun onSizeWidthChange(w: String) = _state.update {
        it.copy(sizeWidthCm = w.filter { c -> c.isDigit() || c == '.' }.take(8))
    }

    /** Switch the dimension unit ("cm" or "in"); the entered values are uploaded in this unit. */
    fun onSizeUnitChange(unit: String) = _state.update { it.copy(sizeUnit = unit) }

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
        it.copy(selectedArtist = artist, isArtistError = false, artistSearchQuery = "", artistResults = emptyList())
    }

    /**
     * "Add artist without artRINX profile" → attribute the artwork to the typed [name] only.
     * No artist id is sent (userId = null); the name is still required.
     */
    fun onArtistWithoutProfile(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        _state.update {
            it.copy(
                selectedArtist = ArtistResult(
                    handle = "",
                    displayName = trimmed,
                    subtitle = "No artRINX profile",
                    userId = null,
                ),
                isArtistError = false,
                artistSearchQuery = "",
                artistResults = emptyList(),
            )
        }
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
        it.copy(selectedMedium = m.title, selectedMediumId = m.id, showMediumPicker = false, isMediumError = false)
    }

    // ── Tags ──────────────────────────────────────────────────────────────────

    fun onTagInputChange(t: String) = _state.update { it.copy(currentTagInput = t) }

    fun onAddTag() {
        val tag = _state.value.currentTagInput.trim().lowercase()
        // Add only a new, non-duplicate tag while under the cap; otherwise leave the input as-is so
        // the (disabled) Add button reflects why it can't be added. Clear the input on a real add.
        if (canAddTag(tag, _state.value.tags)) {
            _state.update { it.copy(tags = it.tags + tag, currentTagInput = "", isTagsError = false) }
        }
    }

    /** Add a suggested (trending) tag from the suggestions row (respects the same cap + dedup). */
    fun onSuggestedTagTap(tag: String) {
        val normalized = tag.trim().lowercase()
        if (canAddTag(normalized, _state.value.tags)) {
            _state.update { it.copy(tags = it.tags + normalized, isTagsError = false) }
        }
    }

    private fun canAddTag(tag: String, tags: List<String>): Boolean =
        tag.isNotEmpty() && tag !in tags && tags.size < MAX_ARTWORK_TAGS

    fun onRemoveTag(tag: String) = _state.update { it.copy(tags = it.tags - tag) }

    // ── Privacy ───────────────────────────────────────────────────────────────

    fun onShowPrivacyPicker()    = _state.update { it.copy(showPrivacyPicker = true) }
    fun onDismissPrivacyPicker() = _state.update { it.copy(showPrivacyPicker = false) }
    fun onPrivacySelected(p: PrivacyOption) = _state.update {
        it.copy(privacy = p, showPrivacyPicker = false)
    }

    // ── Upload ────────────────────────────────────────────────────────────────

    fun validate(): Boolean {
        val s = _state.value
        val titleEmpty = s.title.isEmpty()
        val descriptionBlank = s.description.isBlank()
        val artistNameBlank = (s.selectedArtist?.displayName).isNullOrBlank()
        val mediumMissing = s.selectedMediumId == null
        val tagsEmpty = s.tags.isEmpty()
        // Price is required only when a (visible) shop link has been entered.
        val priceInvalid = !s.isPriceValidForShopLink
        _state.update {
            it.copy(
                isTitleError = titleEmpty,
                isDescriptionError = descriptionBlank,
                isArtistError = artistNameBlank,
                isMediumError = mediumMissing,
                isTagsError = tagsEmpty,
                isPriceError = priceInvalid,
            )
        }
        return !titleEmpty && !descriptionBlank && !artistNameBlank && !mediumMissing &&
            !tagsEmpty && !priceInvalid
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
        val isPrivate = s.privacy == PrivacyOption.PRIVATE
        // Private uploads stay on this screen and show the overlay instead of navigating.
        // Set the awaited/LOADING state BEFORE enqueue (no window where a fast terminal is missed).
        if (isPrivate) {
            awaitingPrivate = true
            _state.update { it.copy(creationStatus = CreationStatus.LOADING) }
        }
        val (height, width, unit) = dimensionsFor(s)
        val started = uploadManager.enqueue(
            UploadRequest(
                imageUri = uri,
                title = s.title,
                description = s.description.ifBlank { null },
                tags = s.tags,
                mediumId = s.selectedMediumId,
                shopLink = s.shopLink.ifBlank { null },
                price = priceFor(s),
                isPrivate = isPrivate,
                artistId = artist?.userId,
                artistName = artist?.displayName,
                sizeHeightCm = height,
                sizeWidthCm = width,
                sizeUnit = unit,
            ),
            artistName = artist?.displayName.orEmpty(),
            artistHandle = artist?.handle?.let { "@$it" }.orEmpty(),
        )
        // An upload was already running → nothing was enqueued. Undo the optimistic LOADING so the
        // private overlay doesn't spin forever waiting for a run that never starts.
        if (!started && isPrivate) {
            awaitingPrivate = false
            _state.update { it.copy(creationStatus = null) }
        }
        return started
    }

    /**
     * Edit mode: PUT the metadata changes for [ArtFormState.editArtworkId] and drive the overlay
     * (LOADING → CREATED / FAILED). The image is never changed.
     */
    fun onSaveEdit() {
        if (!validate()) return
        val s = _state.value
        val id = s.editArtworkId ?: return
        val artist = s.selectedArtist
        val (height, width, unit) = dimensionsFor(s)
        _state.update { it.copy(creationStatus = CreationStatus.LOADING, creationError = null) }
        viewModelScope.launch {
            val result = uploadRepository.updateArtwork(
                id = id,
                request = UpdateArtworkRequest(
                    title = s.title,
                    description = s.description.ifBlank { null },
                    tags = s.tags,
                    mediumId = s.selectedMediumId,
                    shopLink = s.shopLink.ifBlank { null },
                    price = priceFor(s),
                    isPrivate = s.privacy == PrivacyOption.PRIVATE,
                    artistId = artist?.userId,
                    artistName = artist?.displayName,
                    sizeHeightCm = height,
                    sizeWidthCm = width,
                    sizeUnit = unit,
                ),
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

    /** Price is only sent when a shop link is present (handout §Field gating). */
    private fun priceFor(s: ArtFormState): Double? =
        if (s.shopLink.isNotBlank()) s.price.toDoubleOrNull() else null

    /** Optional dimensions: (height, width, unit). Unit is the selected unit (cm/in) iff either
     *  dimension is provided, uploaded as-is (so inches go up as "in"). */
    private fun dimensionsFor(s: ArtFormState): Triple<Double?, Double?, String?> {
        val h = s.sizeHeightCm.toDoubleOrNull()
        val w = s.sizeWidthCm.toDoubleOrNull()
        return Triple(h, w, if (h != null || w != null) s.sizeUnit else null)
    }
}
