package com.rinx.artRINXapp.feature.home.presentation.components

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
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
)

@HiltViewModel
class AddToCurationViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val curationRepository: CurationRepository,
    private val seedStore: CurationSeedStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddToCurationUiState())
    val uiState: StateFlow<AddToCurationUiState> = _uiState.asStateFlow()

    // One-shot "close the sheet" signal — a Channel (not state) so it never replays when the
    // shared ViewModel's sheet is reopened for the same artwork.
    private val _closeSheet = Channel<Unit>(Channel.BUFFERED)
    val closeSheet = _closeSheet.receiveAsFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = profileRepository.getMyCurations(PAGE, SIZE)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    curations = (result as? ApiResult.Success)?.data ?: it.curations,
                )
            }
        }
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
                    _uiState.update { it.copy(isAdding = false, message = "Added to ${target.title}") }
                    _closeSheet.send(Unit)
                }
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
        const val SIZE = 50
    }
}
