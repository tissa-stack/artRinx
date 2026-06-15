package com.rinx.artRINXapp.feature.settings.presentation.blocked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.profile.domain.model.BlockedArtwork
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockedArtworksUiState(
    val artworks: List<BlockedArtwork> = emptyList(),
    val pendingUnblock: BlockedArtwork? = null,
    val isLoading: Boolean = true,
    val isUnblocking: Boolean = false,
    val error: String? = null,
    val unblockError: String? = null,
    /** One-shot: title of the just-unblocked artwork so the screen can toast a success message. */
    val unblockedTitle: String? = null,
)

@HiltViewModel
class BlockedArtworksViewModel @Inject constructor(
    private val repository: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BlockedArtworksUiState())
    val state: StateFlow<BlockedArtworksUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.getBlockedArtworks(page = 1, size = 50)) {
                is ApiResult.Success -> _state.update {
                    it.copy(artworks = result.data, isLoading = false)
                }
                is ApiResult.Error -> _state.update {
                    it.copy(isLoading = false, error = result.toMessage())
                }
            }
        }
    }

    fun onRetry() = load()

    fun onUnblockRequest(artwork: BlockedArtwork) =
        _state.update { it.copy(pendingUnblock = artwork) }

    fun onDismissUnblock() {
        if (_state.value.isUnblocking) return
        _state.update { it.copy(pendingUnblock = null) }
    }

    fun onConfirmUnblock() {
        val target = _state.value.pendingUnblock ?: return
        _state.update { it.copy(isUnblocking = true) }
        viewModelScope.launch {
            when (repository.unblockArtwork(target.artId)) {
                is ApiResult.Success -> _state.update { s ->
                    s.copy(
                        artworks = s.artworks.filterNot { it.artId == target.artId },
                        pendingUnblock = null,
                        isUnblocking = false,
                        unblockedTitle = target.title,
                    )
                }
                is ApiResult.Error -> _state.update {
                    it.copy(
                        isUnblocking = false,
                        pendingUnblock = null,
                        unblockError = "Couldn't unblock this artwork. Please try again.",
                    )
                }
            }
        }
    }

    fun onUnblockErrorShown() = _state.update { it.copy(unblockError = null) }
    fun onUnblockMessageShown() = _state.update { it.copy(unblockedTitle = null) }
}

private fun ApiResult.Error.toMessage(): String = when (this) {
    is ApiResult.Error.Network -> "No connection. Please try again."
    is ApiResult.Error.Server -> "Something went wrong. Please try again."
    else -> "Couldn't load blocked artworks. Please try again."
}
