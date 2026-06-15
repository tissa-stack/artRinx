package com.rinx.artRINXapp.feature.notifications.presentation.messages.components

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.network.userMessage
import com.rinx.artRINXapp.feature.notifications.domain.repository.MessagesRepository
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMenuUiState(
    val name: String = "User",
    val role: String = "Artist",
    val handle: String = "user", // without leading "@"
    val iBlocked: Boolean = false,
    val isActioning: Boolean = false,
    val actionError: String? = null,
    /** One-shot: set true after a successful block so the screen can exit to a safe screen. */
    val blockedSuccess: Boolean = false,
    /** One-shot: set true after a successful unblock so the screen can close the confirm dialog. */
    val unblockedSuccess: Boolean = false,
    /** One-shot: set true after a successful unfollow so the screen can toast a confirmation. */
    val unfollowedSuccess: Boolean = false,
)

@HiltViewModel
class ChatMenuViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val messagesRepository: MessagesRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val userId: Int = savedStateHandle.get<String>("userId")?.toIntOrNull() ?: 0

    private val _state = MutableStateFlow(ChatMenuUiState())
    val state: StateFlow<ChatMenuUiState> = _state.asStateFlow()

    init {
        if (userId != 0) viewModelScope.launch {
            val res = profileRepository.getPublicProfile(userId)
            if (res is ApiResult.Success) {
                val p = res.data
                _state.update {
                    it.copy(
                        name = p.displayName.ifBlank { "User" },
                        role = p.role.ifBlank { "Artist" },
                        handle = p.handle.removePrefix("@").ifBlank { "user" },
                        iBlocked = p.iBlocked,
                    )
                }
            }
        }
    }

    fun blockUser() {
        if (userId == 0 || _state.value.isActioning) return
        _state.update { it.copy(isActioning = true, actionError = null) }
        viewModelScope.launch {
            when (val r = profileRepository.blockUser(userId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(isActioning = false, iBlocked = true, blockedSuccess = true)
                }
                is ApiResult.Error -> _state.update {
                    it.copy(isActioning = false, actionError = r.userMessage("Couldn't block. Please try again."))
                }
            }
        }
    }

    fun unblockUser() {
        if (userId == 0 || _state.value.isActioning) return
        _state.update { it.copy(isActioning = true, actionError = null) }
        viewModelScope.launch {
            when (val r = profileRepository.unblockUser(userId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(isActioning = false, iBlocked = false, unblockedSuccess = true)
                }
                is ApiResult.Error -> _state.update {
                    it.copy(isActioning = false, actionError = r.userMessage("Couldn't unblock. Please try again."))
                }
            }
        }
    }

    fun reportUser() {
        if (userId == 0) return
        viewModelScope.launch {
            val r = profileRepository.reportUser(userId, "Reported from chat")
            if (r is ApiResult.Error) {
                _state.update { it.copy(actionError = r.userMessage("Couldn't send the report. Please try again.")) }
            }
        }
    }

    /** Caller confirms the unfollow first (see the chat screen's confirm dialog). */
    fun unfollowUser() {
        if (userId == 0 || _state.value.isActioning) return
        _state.update { it.copy(isActioning = true, actionError = null) }
        viewModelScope.launch {
            when (val r = profileRepository.unfollowUser(userId)) {
                is ApiResult.Success -> _state.update { it.copy(isActioning = false, unfollowedSuccess = true) }
                is ApiResult.Error -> _state.update {
                    it.copy(isActioning = false, actionError = r.userMessage("Couldn't unfollow. Please try again."))
                }
            }
        }
    }

    fun onActionErrorShown() = _state.update { it.copy(actionError = null) }
    fun onBlockedHandled() = _state.update { it.copy(blockedSuccess = false) }
    fun onUnblockedHandled() = _state.update { it.copy(unblockedSuccess = false) }
    fun onUnfollowedHandled() = _state.update { it.copy(unfollowedSuccess = false) }

    /** Delete all messages with this user (§7.10). */
    fun deleteChat() {
        if (userId != 0) viewModelScope.launch { messagesRepository.deleteChat(userId) }
    }
}
