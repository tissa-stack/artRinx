package com.rinx.artRINXapp.feature.profile.presentation.follow

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.paging.ListPage
import com.rinx.artRINXapp.core.paging.toLoadMoreMessage
import com.rinx.artRINXapp.feature.profile.domain.model.FollowUser
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FollowTab { FOLLOWERS, FOLLOWING }

data class FollowListUiState(
    val activeTab: FollowTab = FollowTab.FOLLOWERS,
    val followers: List<FollowUser> = emptyList(),
    val following: List<FollowUser> = emptyList(),
    val followersPaging: ListPage = ListPage(),
    val followingPaging: ListPage = ListPage(),
    val query: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    fun pagingFor(tab: FollowTab): ListPage =
        if (tab == FollowTab.FOLLOWERS) followersPaging else followingPaging

    private val active: List<FollowUser> get() = if (activeTab == FollowTab.FOLLOWERS) followers else following

    /** The active tab's list, filtered by the search query (name or @handle, case-insensitive). */
    val visible: List<FollowUser>
        get() = if (query.isBlank()) active else active.filter {
            it.name.contains(query, ignoreCase = true) || it.handle.contains(query, ignoreCase = true)
        }
}

@HiltViewModel
class FollowListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        FollowListUiState(
            activeTab = if (savedStateHandle.get<String>("tab") == "following") {
                FollowTab.FOLLOWING
            } else {
                FollowTab.FOLLOWERS
            },
        ),
    )
    val state: StateFlow<FollowListUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val followersJob = async { repository.getFollowers(1, SIZE) }
            val followingJob = async { repository.getFollowing(1, SIZE) }
            val followersRes = followersJob.await()
            val followingRes = followingJob.await()
            if (followersRes is ApiResult.Error && followingRes is ApiResult.Error) {
                _state.update { it.copy(isLoading = false, error = followersRes.toMessage()) }
            } else {
                val followers = (followersRes as? ApiResult.Success)?.data.orEmpty()
                val following = (followingRes as? ApiResult.Success)?.data.orEmpty()
                _state.update {
                    it.copy(
                        followers = followers,
                        following = following,
                        followersPaging = ListPage(page = 1, hasMore = followers.size >= SIZE),
                        followingPaging = ListPage(page = 1, hasMore = following.size >= SIZE),
                        isLoading = false,
                        error = null,
                    )
                }
            }
        }
    }

    fun onRetry() = load()
    fun onTabSelected(tab: FollowTab) = _state.update { it.copy(activeTab = tab) }
    fun onQueryChange(q: String) = _state.update { it.copy(query = q) }
    fun onClearQuery() = _state.update { it.copy(query = "") }

    /** Load the next page for [tab] on scroll-to-bottom (no-op while loading / at end / errored). */
    fun loadMore(tab: FollowTab) {
        val st = _state.value
        if (st.isLoading || st.pagingFor(tab).blocked) return
        setLoadingMore(tab, true)
        viewModelScope.launch {
            val next = st.pagingFor(tab).page + 1
            val res = if (tab == FollowTab.FOLLOWERS) {
                repository.getFollowers(next, SIZE)
            } else {
                repository.getFollowing(next, SIZE)
            }
            _state.update { s ->
                if (res is ApiResult.Success) {
                    val current = if (tab == FollowTab.FOLLOWERS) s.followers else s.following
                    val seen = current.mapTo(HashSet()) { it.userId }
                    val merged = current + res.data.filter { seen.add(it.userId) }
                    val paging = s.pagingFor(tab).copy(
                        page = next, isLoadingMore = false,
                        hasMore = res.data.size >= SIZE, loadMoreError = null,
                    )
                    if (tab == FollowTab.FOLLOWERS) s.copy(followers = merged, followersPaging = paging)
                    else s.copy(following = merged, followingPaging = paging)
                } else {
                    val err = (res as ApiResult.Error).toLoadMoreMessage()
                    val paging = s.pagingFor(tab).copy(isLoadingMore = false, loadMoreError = err)
                    if (tab == FollowTab.FOLLOWERS) s.copy(followersPaging = paging)
                    else s.copy(followingPaging = paging)
                }
            }
        }
    }

    /** Footer "Retry": clear the error for [tab] and try the same next page again. */
    fun retryLoadMore(tab: FollowTab) {
        setLoadMoreError(tab, null)
        loadMore(tab)
    }

    private fun setLoadingMore(tab: FollowTab, loading: Boolean) = _state.update { s ->
        if (tab == FollowTab.FOLLOWERS) s.copy(followersPaging = s.followersPaging.copy(isLoadingMore = loading))
        else s.copy(followingPaging = s.followingPaging.copy(isLoadingMore = loading))
    }

    private fun setLoadMoreError(tab: FollowTab, message: String?) = _state.update { s ->
        if (tab == FollowTab.FOLLOWERS) s.copy(followersPaging = s.followersPaging.copy(loadMoreError = message))
        else s.copy(followingPaging = s.followingPaging.copy(loadMoreError = message))
    }

    private companion object {
        const val SIZE = 20
    }
}

private fun ApiResult.Error.toMessage(): String = when (this) {
    is ApiResult.Error.Network -> "No connection. Please try again."
    is ApiResult.Error.Server -> "Something went wrong. Please try again."
    else -> "Couldn't load this list. Please try again."
}
