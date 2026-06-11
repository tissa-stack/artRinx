package com.rinx.artRINXapp.feature.settings.presentation.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
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
    val error: String? = null,
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

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
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
                    val invitees = if (code.isNotBlank()) loadInvitees(code) else emptyList()
                    _state.update { it.copy(invitees = invitees, isLoading = false) }
                }
                is ApiResult.Error -> _state.update {
                    it.copy(isLoading = false, error = info.toMessage())
                }
            }
        }
    }

    private suspend fun loadInvitees(code: String): List<Invitee> =
        when (val result = repository.getInvitedUsers(code, page = 1, size = 50)) {
            is ApiResult.Success -> result.data.map {
                Invitee(
                    id = it.id,
                    name = it.name,
                    handle = it.handle,
                    date = it.joinedDate,
                    avatarUrl = it.avatarUrl,
                )
            }
            is ApiResult.Error -> emptyList()
        }

    fun onRetry() = load()
}

private fun ApiResult.Error.toMessage(): String = when (this) {
    is ApiResult.Error.Network -> "No connection. Please try again."
    is ApiResult.Error.Server -> "Something went wrong. Please try again."
    else -> "Couldn't load your invitations. Please try again."
}
