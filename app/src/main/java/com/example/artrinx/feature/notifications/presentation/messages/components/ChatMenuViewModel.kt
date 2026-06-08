package com.example.artrinx.feature.notifications.presentation.messages.components

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.notifications.domain.repository.MessagesRepository
import com.example.artrinx.feature.profile.domain.repository.ProfileRepository
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
                    )
                }
            }
        }
    }

    fun blockUser() {
        if (userId != 0) viewModelScope.launch { profileRepository.blockUser(userId) }
    }

    fun reportUser() {
        if (userId != 0) viewModelScope.launch {
            profileRepository.reportUser(userId, "Reported from chat")
        }
    }

    fun unfollowUser() {
        if (userId != 0) viewModelScope.launch { profileRepository.unfollowUser(userId) }
    }

    fun deleteChat() {
        if (userId != 0) viewModelScope.launch { messagesRepository.deleteChat(userId) }
    }
}
