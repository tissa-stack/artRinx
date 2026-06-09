package com.rinx.artRINXapp.feature.profile.presentation.other

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.network.userMessage
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
    // pagination
    val isLoadingMore: Boolean = false,
    val artHasMore: Boolean = false,
    val curationHasMore: Boolean = false,
    // actions
    val isFollowPending: Boolean = false,
    val isActioning: Boolean = false,
    val actionError: String? = null,
    // report sheet
    val isReporting: Boolean = false,
    val reportSent: Boolean = false,
    // one-shot: set after a successful unblock so the screen can toast a success message
    val unblockedSuccess: Boolean = false,
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
    private var artPage = 1
    private var curationPage = 1

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
                is ApiResult.Success -> {
                    val art = (artJob.await() as? ApiResult.Success)?.data.orEmpty()
                    val cur = (curationJob.await() as? ApiResult.Success)?.data.orEmpty()
                    artPage = 1; curationPage = 1
                    _uiState.update {
                        it.copy(
                            profile = profileRes.data,
                            artItems = art,
                            curations = cur,
                            artHasMore = art.size >= PAGE_SIZE,
                            curationHasMore = cur.size >= PAGE_SIZE,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = profileRes.toMessage())
                }
            }
        }
    }

    /** Infinite scroll: page the active tab (art / curations), append + dedup, stop when short. */
    fun loadMore() {
        val id = userId ?: return
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore) return
        when (state.activeTab) {
            ProfileTab.ART -> {
                if (!state.artHasMore) return
                _uiState.update { it.copy(isLoadingMore = true) }
                viewModelScope.launch {
                    val next = (repository.getPublicArtworks(id, artPage + 1, PAGE_SIZE) as? ApiResult.Success)?.data.orEmpty()
                    artPage += 1
                    _uiState.update { st ->
                        val existing = st.artItems.associateBy { it.id }
                        st.copy(
                            artItems = st.artItems + next.filter { it.id !in existing },
                            artHasMore = next.size >= PAGE_SIZE,
                            isLoadingMore = false,
                        )
                    }
                }
            }
            ProfileTab.CURATIONS -> {
                if (!state.curationHasMore) return
                _uiState.update { it.copy(isLoadingMore = true) }
                viewModelScope.launch {
                    val next = (repository.getPublicCurations(id, curationPage + 1, PAGE_SIZE) as? ApiResult.Success)?.data.orEmpty()
                    curationPage += 1
                    _uiState.update { st ->
                        val existing = st.curations.associateBy { it.id }
                        st.copy(
                            curations = st.curations + next.filter { it.id !in existing },
                            curationHasMore = next.size >= PAGE_SIZE,
                            isLoadingMore = false,
                        )
                    }
                }
            }
            ProfileTab.LIKED -> Unit // public profiles have no Liked tab
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
            when (val r = repository.blockUser(id)) {
                is ApiResult.Success -> {
                    profileRefreshBus.signal()
                    _closed.send(Unit)
                }
                is ApiResult.Error -> _uiState.update { it.copy(isActioning = false, actionError = r.userMessage("Couldn't block. Please try again.")) }
            }
        }
    }

    fun unblock() {
        val id = userId ?: return
        if (_uiState.value.isActioning) return
        _uiState.update { it.copy(isActioning = true, actionError = null) }
        viewModelScope.launch {
            when (val r = repository.unblockUser(id)) {
                is ApiResult.Success -> _uiState.update { s ->
                    s.copy(isActioning = false, profile = s.profile?.copy(iBlocked = false), unblockedSuccess = true)
                }
                is ApiResult.Error -> _uiState.update { it.copy(isActioning = false, actionError = r.userMessage("Couldn't unblock. Please try again.")) }
            }
        }
    }

    fun submitReport(message: String) {
        val id = userId ?: return
        if (_uiState.value.isReporting) return
        _uiState.update { it.copy(isReporting = true, actionError = null) }
        viewModelScope.launch {
            when (val r = repository.reportUser(id, message)) {
                is ApiResult.Success -> {
                    lastReportMessage = message
                    _uiState.update { it.copy(isReporting = false, reportSent = true) }
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isReporting = false, actionError = r.userMessage("Couldn't send the report. Please try again."))
                }
            }
        }
    }

    fun onReportClosed() = _uiState.update { it.copy(isReporting = false, reportSent = false) }
    fun onActionErrorShown() = _uiState.update { it.copy(actionError = null) }
    fun onUnblockedShown() = _uiState.update { it.copy(unblockedSuccess = false) }

    private companion object {
        const val PAGE_SIZE = 30
    }
}

private fun ApiResult.Error.toMessage(): String = when (this) {
    is ApiResult.Error.Network -> "No connection. Please try again."
    is ApiResult.Error.Server -> "Something went wrong. Please try again."
    else -> "Couldn't load this profile. Please try again."
}
