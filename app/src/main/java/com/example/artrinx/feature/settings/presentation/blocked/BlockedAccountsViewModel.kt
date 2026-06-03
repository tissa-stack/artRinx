package com.example.artrinx.feature.settings.presentation.blocked

import androidx.lifecycle.ViewModel
import com.example.artrinx.feature.settings.domain.model.BlockedAccount
import com.example.artrinx.feature.settings.domain.model.MockSettingsData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class BlockedAccountsUiState(
    val accounts: List<BlockedAccount> = emptyList(),
    val pendingUnblock: BlockedAccount? = null,
)

@HiltViewModel
class BlockedAccountsViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(
        BlockedAccountsUiState(accounts = MockSettingsData.blockedAccounts),
    )
    val state: StateFlow<BlockedAccountsUiState> = _state.asStateFlow()

    fun onUnblockRequest(account: BlockedAccount) =
        _state.update { it.copy(pendingUnblock = account) }

    fun onDismissUnblock() = _state.update { it.copy(pendingUnblock = null) }

    fun onConfirmUnblock() = _state.update { s ->
        val target = s.pendingUnblock ?: return@update s.copy(pendingUnblock = null)
        s.copy(
            accounts = s.accounts.filterNot { it.id == target.id },
            pendingUnblock = null,
        )
    }
}