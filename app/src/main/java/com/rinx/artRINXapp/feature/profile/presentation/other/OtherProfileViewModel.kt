package com.rinx.artRINXapp.feature.profile.presentation.other

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.network.userMessage
import com.rinx.artRINXapp.core.util.BlockedArtworkBus
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
    val isRefreshing: Boolean = false,
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
    /** One-shot follow/unfollow success message (toasted then cleared by the screen). */
    val actionMessage: String? = null,
)

@HiltViewModel
class OtherProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
    private val profileRefreshBus: ProfileRefreshBus,
    private val blockedArtworkBus: BlockedArtworkBus,
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
        observeBlocks()
    }

    /** Drop a blocked artwork from the grid immediately (no refresh wait). */
    private fun observeBlocks() {
        viewModelScope.launch {
            blockedArtworkBus.events.collect { blockedId ->
                val idStr = blockedId.toString()
                _uiState.update { it.copy(artItems = it.artItems.filterNot { item -> item.id == idStr }) }
            }
        }
    }

    /** Pull-to-refresh: re-fetch profile + art + curations, keeping content visible (SWR). */
    fun refresh() {
        val id = userId ?: return
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val profileJob = async { repository.getPublicProfile(id) }
            val artJob = async { repository.getPublicArtworks(id, 1, PAGE_SIZE) }
            val curationJob = async { repository.getPublicCurations(id, 1, PAGE_SIZE) }
            val profileRes = profileJob.await()
            val art = (artJob.await() as? ApiResult.Success)?.data
            val cur = (curationJob.await() as? ApiResult.Success)?.data
            artPage = 1; curationPage = 1
            _uiState.update { st ->
                st.copy(
                    profile = (profileRes as? ApiResult.Success)?.data ?: st.profile,
                    artItems = art ?: st.artItems,
                    curations = cur ?: st.curations,
                    artHasMore = (art?.size ?: st.artItems.size) >= PAGE_SIZE,
                    curationHasMore = (cur?.size ?: st.curations.size) >= PAGE_SIZE,
                    isRefreshing = false,
                )
            }
        }
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
            // Advance the page + recompute hasMore ONLY on success; on a transient error keep both and
            // just clear the spinner so the next scroll retries the same page (never skip/permanently stop).
            ProfileTab.ART -> {
                if (!state.artHasMore) return
                _uiState.update { it.copy(isLoadingMore = true) }
                viewModelScope.launch {
                    when (val res = repository.getPublicArtworks(id, artPage + 1, PAGE_SIZE)) {
                        is ApiResult.Success -> {
                            artPage += 1
                            _uiState.update { st ->
                                val existing = st.artItems.associateBy { it.id }
                                st.copy(
                                    artItems = st.artItems + res.data.filter { it.id !in existing },
                                    artHasMore = res.data.size >= PAGE_SIZE,
                                    isLoadingMore = false,
                                )
                            }
                        }
                        is ApiResult.Error -> _uiState.update { it.copy(isLoadingMore = false) }
                    }
                }
            }
            ProfileTab.CURATIONS -> {
                if (!state.curationHasMore) return
                _uiState.update { it.copy(isLoadingMore = true) }
                viewModelScope.launch {
                    when (val res = repository.getPublicCurations(id, curationPage + 1, PAGE_SIZE)) {
                        is ApiResult.Success -> {
                            curationPage += 1
                            _uiState.update { st ->
                                val existing = st.curations.associateBy { it.id }
                                st.copy(
                                    curations = st.curations + res.data.filter { it.id !in existing },
                                    curationHasMore = res.data.size >= PAGE_SIZE,
                                    isLoadingMore = false,
                                )
                            }
                        }
                        is ApiResult.Error -> _uiState.update { it.copy(isLoadingMore = false) }
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
            val r = repository.followUser(id)
            if (r is ApiResult.Error) {
                // revert
                _uiState.update { s ->
                    val cur = s.profile ?: return@update s.copy(isFollowPending = false)
                    s.copy(isFollowPending = false, profile = cur.copy(isFollowing = false, followerCount = (cur.followerCount - 1).coerceAtLeast(0)), actionError = r.userMessage("Couldn't follow. Please try again."))
                }
            } else {
                _uiState.update { s ->
                    val name = s.profile?.displayName?.takeIf { it.isNotBlank() }
                    s.copy(isFollowPending = false, actionMessage = if (name != null) "Following $name" else "Following")
                }
            }
        }
    }

    fun unfollow() {
        val id = userId ?: return
        if (_uiState.value.isActioning) return
        _uiState.update { it.copy(isActioning = true, actionError = null) }
        viewModelScope.launch {
            when (val r = repository.unfollowUser(id)) {
                is ApiResult.Success -> _uiState.update { s ->
                    val cur = s.profile
                    val name = cur?.displayName?.takeIf { it.isNotBlank() }
                    s.copy(
                        isActioning = false,
                        profile = cur?.copy(isFollowing = false, followerCount = (cur.followerCount - 1).coerceAtLeast(0)),
                        actionMessage = if (name != null) "Unfollowed $name" else "Unfollowed",
                    )
                }
                is ApiResult.Error -> _uiState.update { it.copy(isActioning = false, actionError = r.userMessage("Couldn't unfollow. Please try again.")) }
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
    fun onActionMessageShown() = _uiState.update { it.copy(actionMessage = null) }

    private companion object {
        const val PAGE_SIZE = 30
    }
}

private fun ApiResult.Error.toMessage(): String = when (this) {
    is ApiResult.Error.Network -> "No connection. Please try again."
    is ApiResult.Error.Server -> "Something went wrong. Please try again."
    else -> "Couldn't load this profile. Please try again."
}
