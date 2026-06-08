package com.rinx.artRINXapp.feature.profile.presentation.other

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.util.ProfileRefreshBus
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileArtItem
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileCurationItem
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileTab
import com.rinx.artRINXapp.feature.profile.domain.model.PublicProfile
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class OtherProfileUiState(
    val profile: PublicProfile? = null,
    val activeTab: ProfileTab = ProfileTab.ART,
    val artItems: List<ProfileArtItem> = emptyList(),
    val curations: List<ProfileCurationItem> = emptyList(),
    val isBioExpanded: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    // actions
    val isFollowPending: Boolean = false,
    val isActioning: Boolean = false,
    val actionError: String? = null,
    // report sheet
    val isReporting: Boolean = false,
    val reportSent: Boolean = false,
)

@HiltViewModel
class OtherProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
    private val profileRefreshBus: ProfileRefreshBus,
) : ViewModel() {

    private val userId: Int? = savedStateHandle.get<String>("userId")?.toIntOrNull()

    private val _uiState = MutableStateFlow(OtherProfileUiState())
    val uiState: StateFlow<OtherProfileUiState> = _uiState.asStateFlow()

    /** One-shot: emitted after a successful block so the screen pops back. */
    private val _closed = Channel<Unit>(Channel.BUFFERED)
    val closed = _closed.receiveAsFlow()

    private var lastReportMessage: String = ""

    init {
        load()
    }

    private fun load() {
        val id = userId
        if (id == null) {
            _uiState.update { it.copy(isLoading = false, error = "Profile unavailable.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val profileJob = async { repository.getPublicProfile(id) }
            val artJob = async { repository.getPublicArtworks(id, 1, 30) }
            val curationJob = async { repository.getPublicCurations(id, 1, 30) }
            when (val profileRes = profileJob.await()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        profile = profileRes.data,
                        artItems = (artJob.await() as? ApiResult.Success)?.data.orEmpty(),
                        curations = (curationJob.await() as? ApiResult.Success)?.data.orEmpty(),
                        isLoading = false,
                        error = null,
                    )
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = profileRes.toMessage())
                }
            }
        }
    }

    fun onRetry() = load()
    fun onTabSelected(tab: ProfileTab) = _uiState.update { it.copy(activeTab = tab) }
    fun onBioExpandToggle() = _uiState.update { it.copy(isBioExpanded = !it.isBioExpanded) }

    fun follow() {
        val id = userId ?: return
        val p = _uiState.value.profile ?: return
        if (p.isFollowing || _uiState.value.isFollowPending) return
        _uiState.update { it.copy(isFollowPending = true, profile = p.copy(isFollowing = true, followerCount = p.followerCount + 1)) }
        viewModelScope.launch {
            if (repository.followUser(id) is ApiResult.Error) {
                // revert
                _uiState.update { s ->
                    val cur = s.profile ?: return@update s.copy(isFollowPending = false)
                    s.copy(isFollowPending = false, profile = cur.copy(isFollowing = false, followerCount = (cur.followerCount - 1).coerceAtLeast(0)), actionError = "Couldn't follow. Please try again.")
                }
            } else {
                _uiState.update { it.copy(isFollowPending = false) }
            }
        }
    }

    fun unfollow() {
        val id = userId ?: return
        if (_uiState.value.isActioning) return
        _uiState.update { it.copy(isActioning = true, actionError = null) }
        viewModelScope.launch {
            when (repository.unfollowUser(id)) {
                is ApiResult.Success -> _uiState.update { s ->
                    val cur = s.profile
                    s.copy(
                        isActioning = false,
                        profile = cur?.copy(isFollowing = false, followerCount = (cur.followerCount - 1).coerceAtLeast(0)),
                    )
                }
                is ApiResult.Error -> _uiState.update { it.copy(isActioning = false, actionError = "Couldn't unfollow. Please try again.") }
            }
        }
    }

    fun block() {
        val id = userId ?: return
        if (_uiState.value.isActioning) return
        _uiState.update { it.copy(isActioning = true, actionError = null) }
        viewModelScope.launch {
            when (repository.blockUser(id)) {
                is ApiResult.Success -> {
                    profileRefreshBus.signal()
                    _closed.send(Unit)
                }
                is ApiResult.Error -> _uiState.update { it.copy(isActioning = false, actionError = "Couldn't block. Please try again.") }
            }
        }
    }

    fun unblock() {
        val id = userId ?: return
        if (_uiState.value.isActioning) return
        _uiState.update { it.copy(isActioning = true, actionError = null) }
        viewModelScope.launch {
            when (repository.unblockUser(id)) {
                is ApiResult.Success -> _uiState.update { s ->
                    s.copy(isActioning = false, profile = s.profile?.copy(iBlocked = false))
                }
                is ApiResult.Error -> _uiState.update { it.copy(isActioning = false, actionError = "Couldn't unblock. Please try again.") }
            }
        }
    }

    fun submitReport(message: String) {
        val id = userId ?: return
        if (_uiState.value.isReporting) return
        _uiState.update { it.copy(isReporting = true, actionError = null) }
        viewModelScope.launch {
            when (repository.reportUser(id, message)) {
                is ApiResult.Success -> {
                    lastReportMessage = message
                    _uiState.update { it.copy(isReporting = false, reportSent = true) }
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isReporting = false, actionError = "Couldn't send the report. Please try again.")
                }
            }
        }
    }

    fun onReportClosed() = _uiState.update { it.copy(isReporting = false, reportSent = false) }
    fun onActionErrorShown() = _uiState.update { it.copy(actionError = null) }
}

private fun ApiResult.Error.toMessage(): String = when (this) {
    is ApiResult.Error.Network -> "No connection. Please try again."
    is ApiResult.Error.Server -> "Something went wrong. Please try again."
    else -> "Couldn't load this profile. Please try again."
}
