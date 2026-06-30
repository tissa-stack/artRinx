package com.rinx.artRINXapp.feature.share.presentation

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.profile.domain.model.FollowUser
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import com.rinx.artRINXapp.feature.share.domain.model.ShareKind
import com.rinx.artRINXapp.feature.share.domain.repository.ShareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ShareUiState(
    /** The people the current user follows — the share-to candidates. */
    val following: List<FollowUser> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    /** Inline error shown if loading the following list fails. */
    val loadFailed: Boolean = false,
)

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val shareRepository: ShareRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    // One-shot toast message (success / failure).
    private val _message = Channel<String>(Channel.BUFFERED)
    val message = _message.receiveAsFlow()

    // One-shot "dismiss the sheet" signal after a successful send.
    private val _closeSheet = Channel<Unit>(Channel.BUFFERED)
    val closeSheet = _closeSheet.receiveAsFlow()

    /** Reload the share-to candidates. Driven by the sheet on every open (the VM is screen-scoped and
     *  reused, so a one-shot init load would leave a failed/empty first open stuck on reopen). */
    fun reload() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            when (val result = profileRepository.getFollowing(PAGE, SIZE)) {
                is ApiResult.Success ->
                    _uiState.update { it.copy(isLoading = false, following = result.data) }
                is ApiResult.Error ->
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    fun retry() = load()

    /** Share [kind]/[resourceId] to [recipientIds]; on success toast + close the sheet. */
    fun send(kind: ShareKind, resourceId: String, recipientIds: List<Int>) {
        if (recipientIds.isEmpty() || _uiState.value.isSending) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            when (shareRepository.shareToFollowers(kind, resourceId, recipientIds)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSending = false) }
                    _message.send(
                        if (recipientIds.size == 1) "Shared with 1 person"
                        else "Shared with ${recipientIds.size} people",
                    )
                    _closeSheet.send(Unit)
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isSending = false) }
                    _message.send("Couldn't share — try again")
                }
            }
        }
    }

    private companion object {
        const val PAGE = 1
        const val SIZE = 100
    }
}
