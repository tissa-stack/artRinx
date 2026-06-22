package com.rinx.artRINXapp.feature.home.presentation.components

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.paging.ListPage
import com.rinx.artRINXapp.core.paging.toLoadMoreMessage
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileCurationItem
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import com.rinx.artRINXapp.feature.upload.domain.CurationSeedStore
import com.rinx.artRINXapp.feature.upload.domain.model.CurationSource
import com.rinx.artRINXapp.feature.upload.domain.model.UserArtItem
import com.rinx.artRINXapp.feature.upload.domain.repository.CurationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class AddToCurationUiState(
    val curations: List<ProfileCurationItem> = emptyList(),
    val isLoading: Boolean = true,
    val isAdding: Boolean = false,
    /** One-shot user-facing message (added / failed). Cleared via [consumeMessage]. */
    val message: String? = null,
    /** Infinite-scroll state for the curation list. */
    val paging: ListPage = ListPage(),
)

@HiltViewModel
class AddToCurationViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val curationRepository: CurationRepository,
    private val seedStore: CurationSeedStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddToCurationUiState())
    val uiState: StateFlow<AddToCurationUiState> = _uiState.asStateFlow()

    // One-shot "added → close the sheet" signal carrying the success message. A Channel (not state)
    // so it never replays when the shared ViewModel's sheet is reopened for the same artwork.
    // The message rides this event (rather than [AddToCurationUiState.message]) so the toast is shown
    // synchronously, before the sheet is dismissed — otherwise dismissing disposes the composable and
    // cancels the message-toast effect before it runs.
    private val _closeSheet = Channel<String>(Channel.BUFFERED)
    val closeSheet = _closeSheet.receiveAsFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = profileRepository.getMyCurations(PAGE, SIZE)
            _uiState.update {
                if (result is ApiResult.Success) {
                    it.copy(
                        isLoading = false,
                        curations = result.data,
                        paging = ListPage(page = PAGE, hasMore = result.data.size >= SIZE),
                    )
                } else {
                    it.copy(isLoading = false) // keep any prior list; the empty/error state covers it
                }
            }
        }
    }

    /** Load the next page of the user's curations on scroll-to-bottom (append, dedupe by id). */
    fun loadMore() {
        val st = _uiState.value
        if (st.isLoading || st.paging.blocked) return
        _uiState.update { it.copy(paging = it.paging.copy(isLoadingMore = true)) }
        viewModelScope.launch {
            val next = st.paging.page + 1
            when (val res = profileRepository.getMyCurations(next, SIZE)) {
                is ApiResult.Success -> _uiState.update { s ->
                    val seen = s.curations.mapTo(HashSet()) { it.id }
                    val merged = s.curations + res.data.filter { seen.add(it.id) }
                    s.copy(
                        curations = merged,
                        paging = s.paging.copy(
                            page = next, isLoadingMore = false,
                            hasMore = res.data.size >= SIZE, loadMoreError = null,
                        ),
                    )
                }
                is ApiResult.Error -> _uiState.update { s ->
                    s.copy(paging = s.paging.copy(isLoadingMore = false, loadMoreError = res.toLoadMoreMessage()))
                }
            }
        }
    }

    /** Footer "Retry": clear the error and try the same next page again. */
    fun retryLoadMore() {
        _uiState.update { it.copy(paging = it.paging.copy(loadMoreError = null)) }
        loadMore()
    }

    /** Add the [source]'s artwork(s) to the chosen [target] curation. */
    fun addTo(target: ProfileCurationItem, source: CurationSource) {
        val targetId = target.id.toIntOrNull() ?: return
        if (_uiState.value.isAdding) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAdding = true) }
            val ids = resolveSourceIds(source)
            if (ids.isEmpty()) {
                _uiState.update { it.copy(isAdding = false, message = "Nothing to add") }
                return@launch
            }
            when (val result = curationRepository.addArtworksToCuration(targetId, ids)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isAdding = false) }
                    _closeSheet.send("Added to ${target.title}")
                }
                is ApiResult.Error.Validation ->
                    _uiState.update { it.copy(isAdding = false, message = result.message) }
                is ApiResult.Error ->
                    _uiState.update { it.copy(isAdding = false, message = "Couldn't add — try again") }
            }
        }
    }

    /** Stage the source's arts for the New Curation screen, then invoke [onReady] to navigate. */
    fun prepareCreate(source: CurationSource, onReady: () -> Unit) {
        viewModelScope.launch {
            val items: List<UserArtItem> = when (source) {
                is CurationSource.Artwork -> listOf(
                    UserArtItem(
                        id = source.artworkId.toString(),
                        imageUrl = source.imageUrl,
                        artworkId = source.artworkId,
                    ),
                )
                is CurationSource.Curation ->
                    (curationRepository.getCurationArtItems(source.curationId) as? ApiResult.Success)?.data.orEmpty()
            }
            seedStore.set(items)
            onReady()
        }
    }

    private suspend fun resolveSourceIds(source: CurationSource): List<Int> = when (source) {
        is CurationSource.Artwork -> listOf(source.artworkId)
        is CurationSource.Curation ->
            (curationRepository.getCurationArtItems(source.curationId) as? ApiResult.Success)
                ?.data.orEmpty().mapNotNull { it.artworkId }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    private companion object {
        const val PAGE = 1
        const val SIZE = 20
    }
}
