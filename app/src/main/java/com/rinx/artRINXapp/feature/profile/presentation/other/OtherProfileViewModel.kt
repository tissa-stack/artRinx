package com.rinx.artRINXapp.feature.profile.presentation.other

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.network.userMessage
import com.rinx.artRINXapp.core.util.BlockedArtworkBus
import com.rinx.artRINXapp.core.util.BlockedUserBus
import com.rinx.artRINXapp.core.util.BlockedUsersStore
import com.rinx.artRINXapp.core.util.ProfileRefreshBus
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileArtItem
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileCurationItem
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileTab
import com.rinx.artRINXapp.feature.profile.domain.model.PublicProfile
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import com.rinx.artRINXapp.feature.notifications.domain.model.ChatroomResolution
import com.rinx.artRINXapp.feature.notifications.domain.repository.MessagesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    /** True when this profile is gone for us — deleted account OR the other user blocked us (404).
     *  Drives a neutral "not available" panel (no Retry, no block reveal); never set on a transient
     *  network/server error (those keep [error] + Retry). */
    val notAvailable: Boolean = false,
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
    /** False only when the user can't start a NEW chat with this profile (monthly new-chat quota
     *  exhausted AND no existing chat). Drives the Message button's disabled state. Defaults true so
     *  messaging is never blocked before/if the chatroom check can't resolve. */
    val messageEnabled: Boolean = true,
)

@HiltViewModel
class OtherProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
    private val profileRefreshBus: ProfileRefreshBus,
    private val blockedArtworkBus: BlockedArtworkBus,
    private val blockedUserBus: BlockedUserBus,
    private val blockedUsersStore: BlockedUsersStore,
    private val messagesRepository: MessagesRepository,
) : ViewModel() {

    private val userId: Int? = savedStateHandle.get<String>("userId")?.toIntOrNull()

    private val _uiState = MutableStateFlow(OtherProfileUiState())
    val uiState: StateFlow<OtherProfileUiState> = _uiState.asStateFlow()

    private var lastReportMessage: String = ""
    private var artPage = 1
    private var curationPage = 1

    init {
        load()
        observeBlocks()
        observeUserBlocks()
    }

    /** Drop a blocked artwork from the grid immediately (no refresh wait). */
    private fun observeBlocks() {
        viewModelScope.launch {
            blockedArtworkBus.events.collect { blockedId ->
                val idStr = blockedId.toString()
                _uiState.update { it.copy(artItems = it.artItems.filterNot { item -> item.id == idStr }) }
            }
        }
        // Unblock → re-fetch so the artwork reappears in this profile's grid.
        viewModelScope.launch {
            blockedArtworkBus.unblocked.collect { refresh() }
        }
    }

    /** If this profile's user gets blocked (e.g. from chat while this screen is in the backstack),
     *  flip it into the blocked panel and drop their art from the grid immediately. */
    private fun observeUserBlocks() {
        viewModelScope.launch {
            blockedUserBus.events.collect { blockedUserId ->
                _uiState.update { st ->
                    val isThisProfile = blockedUserId == userId
                    st.copy(
                        profile = if (isThisProfile) st.profile?.copy(iBlocked = true) else st.profile,
                        artItems = if (isThisProfile) emptyList() else st.artItems.filterNot { item ->
                            item.ownerId == blockedUserId || item.artistId == blockedUserId
                        },
                        curations = if (isThisProfile) emptyList() else st.curations,
                    )
                }
            }
        }
        // Unblocked elsewhere (e.g. from the chat screen) → re-fetch so this profile flips out of the
        // blocked panel and its art/curations reappear.
        viewModelScope.launch {
            blockedUserBus.unblocked.collect { unblockedUserId ->
                if (unblockedUserId == userId) refresh()
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
            val chatJob = async { messagesRepository.resolveChatroom(id) }
            val profileRes = profileJob.await()
            // They blocked us mid-session (or the account is gone): flip to the neutral panel and
            // drop the stale header/content instead of keeping it visible.
            if (profileRes is ApiResult.Error.NotFound) {
                repository.evictPublicProfile(id)
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        notAvailable = true,
                        error = null,
                        profile = null,
                        artItems = emptyList(),
                        curations = emptyList(),
                    )
                }
                return@launch
            }
            val fetched = (profileRes as? ApiResult.Success)?.data
            val locallyBlocked = blockedUsersStore.isBlocked(id)
            val profile = fetched?.let { if (locallyBlocked) it.copy(iBlocked = true) else it }
            val blocked = profile?.iBlocked ?: _uiState.value.profile?.iBlocked ?: false
            // Don't surface content for a blocked profile — the panel replaces both tabs.
            val art = if (blocked) emptyList() else (artJob.await() as? ApiResult.Success)?.data
            val cur = if (blocked) emptyList() else (curationJob.await() as? ApiResult.Success)?.data
            artPage = 1; curationPage = 1
            _uiState.update { st ->
                st.copy(
                    profile = profile ?: st.profile,
                    artItems = if (blocked) emptyList() else art ?: st.artItems,
                    curations = if (blocked) emptyList() else cur ?: st.curations,
                    artHasMore = !blocked && (art?.size ?: st.artItems.size) >= PAGE_SIZE,
                    curationHasMore = !blocked && (cur?.size ?: st.curations.size) >= PAGE_SIZE,
                    isRefreshing = false,
                    messageEnabled = canMessageNew(profile ?: st.profile, chatJob.await()),
                )
            }
        }
    }

    /**
     * Can the user open/start a chat with this profile? The monthly new-chat quota only limits
     * *starting* new conversations, so an existing chat (or a still-positive quota, or having blocked
     * them — which opens the chat to unblock) keeps Message enabled. A failed/absent resolve → true,
     * so a flaky check never strands the user.
     */
    private fun canMessageNew(profile: PublicProfile?, chatRes: ApiResult<ChatroomResolution>): Boolean {
        val res = (chatRes as? ApiResult.Success)?.data ?: return true
        val hasChat = res.exists || res.chatroomId != null
        val canStartNew = (res.remainingInvites ?: Int.MAX_VALUE) > 0
        return (profile?.iBlocked == true) || hasChat || canStartNew
    }

    private fun load() {
        val id = userId
        if (id == null) {
            _uiState.update { it.copy(isLoading = false, error = "Profile unavailable.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null, notAvailable = false) }
        viewModelScope.launch {
            val profileJob = async { repository.getPublicProfile(id) }
            val artJob = async { repository.getPublicArtworks(id, 1, 30) }
            val curationJob = async { repository.getPublicCurations(id, 1, 30) }
            // Resolve the chat state up-front so the Message button can reflect the new-chat limit.
            val chatJob = async { messagesRepository.resolveChatroom(id) }
            when (val profileRes = profileJob.await()) {
                is ApiResult.Success -> {
                    // Trust the local store too: if I blocked this user elsewhere (e.g. from an art
                    // detail screen), force the blocked state even if the server field lags behind.
                    val locallyBlocked = blockedUsersStore.isBlocked(id)
                    val profile = if (locallyBlocked) profileRes.data.copy(iBlocked = true) else profileRes.data
                    // A blocked profile shows the "Profile Blocked" panel, never their content.
                    val art = if (profile.iBlocked) emptyList() else (artJob.await() as? ApiResult.Success)?.data.orEmpty()
                    val cur = if (profile.iBlocked) emptyList() else (curationJob.await() as? ApiResult.Success)?.data.orEmpty()
                    artPage = 1; curationPage = 1
                    _uiState.update {
                        it.copy(
                            profile = profile,
                            artItems = art,
                            curations = cur,
                            artHasMore = art.size >= PAGE_SIZE,
                            curationHasMore = cur.size >= PAGE_SIZE,
                            isLoading = false,
                            error = null,
                            messageEnabled = canMessageNew(profile, chatJob.await()),
                        )
                    }
                }
                // 404 → gone for us (deleted, or they blocked us): neutral "not available" panel,
                // no Retry, no sign-out. Drop the cached header so a re-open can't flash it back.
                is ApiResult.Error.NotFound -> {
                    repository.evictPublicProfile(id)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            notAvailable = true,
                            error = null,
                            profile = null,
                            artItems = emptyList(),
                            curations = emptyList(),
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
            if (r is ApiResult.Error.NotFound) {
                // They blocked us mid-session → re-fetch so the screen settles into the neutral
                // "not available" panel (no stuck "Following" button), instead of reverting.
                _uiState.update { it.copy(isFollowPending = false) }
                repository.evictPublicProfile(id)
                load()
            } else if (r is ApiResult.Error) {
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
                // They blocked us mid-session → re-fetch into the neutral "not available" panel.
                is ApiResult.Error.NotFound -> {
                    _uiState.update { it.copy(isActioning = false) }
                    repository.evictPublicProfile(id)
                    load()
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
                // Stay on the profile and flip it into the "Profile Blocked" panel (iOS parity).
                // The repository already signals the blocked-user bus + store, so feeds/search drop
                // their content; we only clear this screen's content + toast a confirmation here.
                is ApiResult.Success -> {
                    profileRefreshBus.signal()
                    _uiState.update { s ->
                        val name = s.profile?.displayName?.takeIf { it.isNotBlank() }
                        s.copy(
                            isActioning = false,
                            profile = s.profile?.copy(iBlocked = true),
                            artItems = emptyList(),
                            curations = emptyList(),
                            actionMessage = if (name != null) "Blocked $name" else "Blocked",
                        )
                    }
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
                is ApiResult.Success -> {
                    _uiState.update { s ->
                        s.copy(isActioning = false, profile = s.profile?.copy(iBlocked = false), unblockedSuccess = true)
                    }
                    // Repopulate the grids now that the profile is visible again (silent reload).
                    refresh()
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
