package com.rinx.artRINXapp.feature.settings.presentation.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.paging.ListPage
import com.rinx.artRINXapp.core.paging.toLoadMoreMessage
import com.rinx.artRINXapp.feature.profile.domain.model.InvitedUser
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import com.rinx.artRINXapp.feature.settings.domain.model.Invitee
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InviteFriendsUiState(
    val code: String = "",
    val invitees: List<Invitee> = emptyList(),
    val invitesPerMonth: Int? = null,
    /** Monthly peer-invite cap (e.g. 5). Used with [invitesPerMonth] to show "used/cap". */
    val invitesMonthlyCap: Int? = null,
    val isLoading: Boolean = true,
    /** Lightweight pull-to-refresh spinner (separate from the first-load [isLoading] state). */
    val isRefreshing: Boolean = false,
    val error: String? = null,
    /** Infinite-scroll state for the invitee list. */
    val paging: ListPage = ListPage(),
) {
    /** Invites consumed this month (sent/joined) = cap − remaining. Null when unknown. */
    val invitesUsed: Int?
        get() = if (invitesMonthlyCap != null && invitesPerMonth != null)
            (invitesMonthlyCap - invitesPerMonth).coerceAtLeast(0) else null
}

@HiltViewModel
class InviteFriendsViewModel @Inject constructor(
    private val repository: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(InviteFriendsUiState())
    val state: StateFlow<InviteFriendsUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load(isRefresh: Boolean = false) {
        _state.update {
            if (isRefresh) it.copy(isRefreshing = true) else it.copy(isLoading = true, error = null)
        }
        viewModelScope.launch {
            when (val info = repository.getInviteInfo()) {
                is ApiResult.Success -> {
                    val code = info.data.code.orEmpty()
                    _state.update {
                        it.copy(
                            code = code,
                            invitesPerMonth = info.data.remainingInvites,
                            invitesMonthlyCap = info.data.invitesMonthlyCap,
                        )
                    }
                    // Only fetch the invitee list when there's a usable code (null for agent users).
                    val result = if (code.isNotBlank()) {
                        repository.getInvitedUsers(code, page = 1, size = SIZE)
                    } else {
                        ApiResult.Success(emptyList())
                    }
                    val invitees = (result as? ApiResult.Success)?.data?.map { it.toInvitee() }.orEmpty()
                    _state.update {
                        it.copy(
                            invitees = invitees,
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                            paging = ListPage(page = 1, hasMore = invitees.size >= SIZE),
                        )
                    }
                }
                // On pull-to-refresh keep the existing list (just stop the spinner); only a failed
                // first load surfaces the full-screen error.
                is ApiResult.Error -> _state.update {
                    if (isRefresh) it.copy(isRefreshing = false)
                    else it.copy(isLoading = false, error = info.toMessage())
                }
            }
        }
    }

    /** Pull-to-refresh: re-fetch invite info + the first page of invitees in place. */
    fun refresh() = load(isRefresh = true)

    private fun InvitedUser.toInvitee() = Invitee(
        id = id,
        name = name,
        handle = handle,
        date = joinedDate,
        avatarUrl = avatarUrl,
    )

    fun onRetry() = load()

    /** Load the next page of invitees on scroll-to-bottom (no-op while loading / at end / errored). */
    fun loadMore() {
        val st = _state.value
        if (st.isLoading || st.paging.blocked || st.code.isBlank()) return
        _state.update { it.copy(paging = it.paging.copy(isLoadingMore = true)) }
        viewModelScope.launch {
            val next = st.paging.page + 1
            when (val result = repository.getInvitedUsers(st.code, page = next, size = SIZE)) {
                is ApiResult.Success -> _state.update { s ->
                    val seen = s.invitees.mapTo(HashSet()) { it.id }
                    val merged = s.invitees + result.data.map { it.toInvitee() }.filter { seen.add(it.id) }
                    s.copy(
                        invitees = merged,
                        paging = s.paging.copy(
                            page = next, isLoadingMore = false,
                            hasMore = result.data.size >= SIZE, loadMoreError = null,
                        ),
                    )
                }
                is ApiResult.Error -> _state.update { s ->
                    s.copy(paging = s.paging.copy(isLoadingMore = false, loadMoreError = result.toLoadMoreMessage()))
                }
            }
        }
    }

    /** Footer "Retry": clear the error and try the same next page again. */
    fun retryLoadMore() {
        _state.update { it.copy(paging = it.paging.copy(loadMoreError = null)) }
        loadMore()
    }

    private companion object {
        const val SIZE = 20
    }
}

private fun ApiResult.Error.toMessage(): String = when (this) {
    is ApiResult.Error.Network -> "No connection. Please try again."
    is ApiResult.Error.Server -> "Something went wrong. Please try again."
    else -> "Couldn't load your invitations. Please try again."
}
