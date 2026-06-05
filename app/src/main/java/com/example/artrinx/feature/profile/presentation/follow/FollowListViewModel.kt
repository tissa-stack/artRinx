package com.example.artrinx.feature.profile.presentation.follow

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.profile.domain.model.FollowUser
import com.example.artrinx.feature.profile.domain.repository.ProfileRepository
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
    val query: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
) {
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
            val followersJob = async { repository.getFollowers(1, 50) }
            val followingJob = async { repository.getFollowing(1, 50) }
            val followersRes = followersJob.await()
            val followingRes = followingJob.await()
            if (followersRes is ApiResult.Error && followingRes is ApiResult.Error) {
                _state.update { it.copy(isLoading = false, error = followersRes.toMessage()) }
            } else {
                _state.update {
                    it.copy(
                        followers = (followersRes as? ApiResult.Success)?.data.orEmpty(),
                        following = (followingRes as? ApiResult.Success)?.data.orEmpty(),
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
}

private fun ApiResult.Error.toMessage(): String = when (this) {
    is ApiResult.Error.Network -> "No connection. Please try again."
    is ApiResult.Error.Server -> "Something went wrong. Please try again."
    else -> "Couldn't load this list. Please try again."
}
