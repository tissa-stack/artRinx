package com.rinx.artRINXapp.feature.profile.presentation.artistarts

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileArtItem
import com.rinx.artRINXapp.feature.profile.domain.model.PublicProfile
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ArtByArtistUiState(
    val artistName: String = "",
    /** True when the credited artist has a RINX profile (→ avatar + follow + tap-to-profile). */
    val hasProfile: Boolean = false,
    val profile: PublicProfile? = null,
    val arts: List<ProfileArtItem> = emptyList(),
    val isFollowing: Boolean = false,
    val isLoading: Boolean = true,
    val error: Boolean = false,
)

@HiltViewModel
class ArtByArtistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val artistName: String = savedStateHandle.get<String>("artistName").orEmpty()
    private val artistId: Int? = savedStateHandle.get<String>("artistId")?.toIntOrNull()

    private val _state = MutableStateFlow(
        ArtByArtistUiState(artistName = artistName, hasProfile = artistId != null),
    )
    val state: StateFlow<ArtByArtistUiState> = _state.asStateFlow()

    /** The profile id to open when the header is tapped (null when the artist has no RINX profile). */
    val profileId: Int? = artistId

    // One-shot follow/unfollow result toast.
    private val _message = kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val message = _message.receiveAsFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = false) }
            val artsJob = async { profileRepository.getArtworksByName(artistName, PAGE, SIZE) }
            val profJob = artistId?.let { id -> async { profileRepository.getPublicProfile(id) } }
            val artsRes = artsJob.await()
            val prof = (profJob?.await() as? ApiResult.Success)?.data
            val arts = (artsRes as? ApiResult.Success)?.data.orEmpty()
            _state.update {
                it.copy(
                    isLoading = false,
                    arts = arts,
                    profile = prof,
                    isFollowing = prof?.isFollowing ?: false,
                    error = artsRes is ApiResult.Error && arts.isEmpty(),
                )
            }
        }
    }

    fun onRetry() = load()

    fun follow() {
        val id = artistId ?: return
        if (_state.value.isFollowing) return
        _state.update { it.copy(isFollowing = true) }
        viewModelScope.launch {
            if (profileRepository.followUser(id) is ApiResult.Error) {
                _state.update { it.copy(isFollowing = false) }
                _message.send("Couldn't follow — try again")
            } else {
                _message.send("Following ${displayName()}")
            }
        }
    }

    /** Caller confirms the unfollow first (see the screen's confirm dialog). */
    fun unfollow() {
        val id = artistId ?: return
        if (!_state.value.isFollowing) return
        _state.update { it.copy(isFollowing = false) }
        viewModelScope.launch {
            if (profileRepository.unfollowUser(id) is ApiResult.Error) {
                _state.update { it.copy(isFollowing = true) }
                _message.send("Couldn't unfollow — try again")
            } else {
                _message.send("Unfollowed ${displayName()}")
            }
        }
    }

    private fun displayName(): String = _state.value.profile?.displayName ?: _state.value.artistName

    private companion object {
        const val PAGE = 1
        const val SIZE = 30
    }
}
