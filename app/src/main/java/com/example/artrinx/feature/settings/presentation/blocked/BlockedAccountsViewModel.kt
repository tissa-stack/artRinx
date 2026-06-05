package com.example.artrinx.feature.settings.presentation.blocked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.profile.domain.repository.ProfileRepository
import com.example.artrinx.feature.settings.domain.model.BlockedAccount
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

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.getBlockedUsers(page = 1, size = 50)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        accounts = result.data.map { u ->
                            BlockedAccount(
                                id = u.userId.toString(),
                                name = u.name,
                                role = u.role,
                                userId = u.userId,
                                avatarUrl = u.avatarUrl,
                            )
                        },
                        isLoading = false,
                    )
                }
                is ApiResult.Error -> _state.update {
                    it.copy(isLoading = false, error = result.toMessage())
                }
            }
        }
    }

    fun onRetry() = load()

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
}

private fun ApiResult.Error.toMessage(): String = when (this) {
    is ApiResult.Error.Network -> "No connection. Please try again."
    is ApiResult.Error.Server -> "Something went wrong. Please try again."
    else -> "Couldn't load blocked accounts. Please try again."
}
