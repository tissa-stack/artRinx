package com.rinx.artRINXapp.feature.settings.presentation.blocked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.paging.ListPage
import com.rinx.artRINXapp.core.paging.toLoadMoreMessage
import com.rinx.artRINXapp.feature.profile.domain.model.BlockedUser
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import com.rinx.artRINXapp.feature.settings.domain.model.BlockedAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockedAccountsUiState(
    val accounts: List<BlockedAccount> = emptyList(),
    val pendingUnblock: BlockedAccount? = null,
    val isLoading: Boolean = true,
    val isUnblocking: Boolean = false,
    val error: String? = null,
    val unblockError: String? = null,
    /** One-shot: name of the just-unblocked account so the screen can toast a success message. */
    val unblockedName: String? = null,
    /** Infinite-scroll state for the list. */
    val paging: ListPage = ListPage(),
)

@HiltViewModel
class BlockedAccountsViewModel @Inject constructor(
    private val repository: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BlockedAccountsUiState())
    val state: StateFlow<BlockedAccountsUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun BlockedUser.toAccount() = BlockedAccount(
        id = userId.toString(),
        name = name,
        role = role,
        userId = userId,
        avatarUrl = avatarUrl,
    )

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.getBlockedUsers(page = 1, size = SIZE)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        accounts = result.data.map { u -> u.toAccount() },
                        isLoading = false,
                        paging = ListPage(page = 1, hasMore = result.data.size >= SIZE),
                    )
                }
                is ApiResult.Error -> _state.update {
                    it.copy(isLoading = false, error = result.toMessage())
                }
            }
        }
    }

    fun onRetry() = load()

    /** Load the next page when the user scrolls to the bottom (no-op while loading / at end / errored). */
    fun loadMore() {
        val st = _state.value
        if (st.isLoading || st.paging.blocked) return
        _state.update { it.copy(paging = it.paging.copy(isLoadingMore = true)) }
        viewModelScope.launch {
            val next = st.paging.page + 1
            when (val result = repository.getBlockedUsers(page = next, size = SIZE)) {
                is ApiResult.Success -> _state.update { s ->
                    val seen = s.accounts.mapTo(HashSet()) { it.id }
                    val merged = s.accounts + result.data.map { it.toAccount() }.filter { seen.add(it.id) }
                    s.copy(
                        accounts = merged,
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

    fun onUnblockRequest(account: BlockedAccount) =
        _state.update { it.copy(pendingUnblock = account) }

    fun onDismissUnblock() {
        if (_state.value.isUnblocking) return
        _state.update { it.copy(pendingUnblock = null) }
    }

    fun onConfirmUnblock() {
        val target = _state.value.pendingUnblock ?: return
        _state.update { it.copy(isUnblocking = true) }
        viewModelScope.launch {
            when (repository.unblockUser(target.userId)) {
                is ApiResult.Success -> _state.update { s ->
                    s.copy(
                        accounts = s.accounts.filterNot { it.id == target.id },
                        pendingUnblock = null,
                        isUnblocking = false,
                        unblockedName = target.name,
                    )
                }
                is ApiResult.Error -> _state.update {
                    it.copy(
                        isUnblocking = false,
                        pendingUnblock = null,
                        unblockError = "Couldn't unblock ${target.name}. Please try again.",
                    )
                }
            }
        }
    }

    fun onUnblockErrorShown() = _state.update { it.copy(unblockError = null) }
    fun onUnblockMessageShown() = _state.update { it.copy(unblockedName = null) }

    private companion object {
        const val SIZE = 20
    }
}

private fun ApiResult.Error.toMessage(): String = when (this) {
    is ApiResult.Error.Network -> "No connection. Please try again."
    is ApiResult.Error.Server -> "Something went wrong. Please try again."
    else -> "Couldn't load blocked accounts. Please try again."
}
