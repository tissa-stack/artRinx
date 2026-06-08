package com.example.artrinx.feature.notifications.presentation.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.notifications.domain.model.UserContact
import com.example.artrinx.feature.profile.domain.repository.ProfileRepository
import com.example.artrinx.feature.search.domain.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewMessageUiState(
    val query: String = "",
    val results: List<UserContact> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class NewMessageViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val searchRepository: SearchRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NewMessageUiState())
    val state: StateFlow<NewMessageUiState> = _state.asStateFlow()

    /** The default (empty-query) list = people you follow. */
    private var following: List<UserContact> = emptyList()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            following = when (val res = profileRepository.getFollowing(page = 1, size = 50)) {
                is ApiResult.Success -> res.data.map {
                    UserContact(
                        id = it.userId.toString(),
                        name = it.name,
                        handle = it.handle,
                        avatarUrl = it.avatarUrl,
                    )
                }
                else -> emptyList()
            }
            _state.update { it.copy(results = following, isLoading = false) }
        }
    }

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q) }
        searchJob?.cancel()
        if (q.isBlank()) {
            _state.update { it.copy(results = following, isLoading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // debounce keystrokes
            _state.update { it.copy(isLoading = true) }
            val results = when (val res = searchRepository.searchUsers(q.trim())) {
                is ApiResult.Success -> res.data.map {
                    UserContact(
                        id = it.id,
                        name = it.displayName,
                        handle = if (it.username.isNotBlank()) "@${it.username}" else "",
                        avatarUrl = it.profilePictureUrl,
                    )
                }
                else -> emptyList()
            }
            _state.update { it.copy(results = results, isLoading = false) }
        }
    }
}
