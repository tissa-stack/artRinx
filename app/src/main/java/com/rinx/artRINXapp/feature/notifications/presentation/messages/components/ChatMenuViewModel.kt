package com.rinx.artRINXapp.feature.notifications.presentation.messages.components

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
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
        if (userId != 0) viewModelScope.launch {
            profileRepository.blockUser(userId)
            _state.update { it.copy(iBlocked = true) }
        }
    }

    fun unblockUser() {
        if (userId != 0) viewModelScope.launch {
            profileRepository.unblockUser(userId)
            _state.update { it.copy(iBlocked = false) }
        }
    }

    fun reportUser() {
        if (userId != 0) viewModelScope.launch {
            profileRepository.reportUser(userId, "Reported from chat")
        }
    }

    fun unfollowUser() {
        if (userId != 0) viewModelScope.launch { profileRepository.unfollowUser(userId) }
    }

    /** Delete all messages with this user (§7.10). */
    fun deleteChat() {
        if (userId != 0) viewModelScope.launch { messagesRepository.deleteChat(userId) }
    }
}
