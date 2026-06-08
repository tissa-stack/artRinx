package com.example.artrinx.feature.home.presentation.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.core.util.ProfileRefreshBus
import com.example.artrinx.feature.home.domain.model.ArtworkItem
import com.example.artrinx.feature.home.domain.model.ShoppablePost
import com.example.artrinx.feature.home.domain.repository.HomeRepository
import com.example.artrinx.feature.notifications.domain.repository.MessagesRepository
import com.example.artrinx.feature.profile.domain.repository.ProfileRepository
import com.example.artrinx.feature.upload.domain.EditTargetStore
import com.example.artrinx.feature.upload.domain.repository.UploadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@Immutable
data class ArtDetailUiState(
    val post: ShoppablePost? = null,
    val moreLikeThis: List<ArtworkItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: Boolean = false,
    /** True when the current user owns this artwork → show Edit/Delete instead of Report. */
    val isOwn: Boolean = false,
    val isDeleting: Boolean = false,
    // ── Report / block (moderation) ──
    val isReporting: Boolean = false,
    val reportSent: Boolean = false,
    val isBlocking: Boolean = false,
    val actionError: String? = null,
    // ── Send-message invitation sheet ──
    val isSendingInvite: Boolean = false,
    val inviteSent: Boolean = false,
    val invitationsLeft: Int? = null,
)

@HiltViewModel
class ArtDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HomeRepository,
    private val profileRepository: ProfileRepository,
    private val messagesRepository: MessagesRepository,
    private val uploadRepository: UploadRepository,
    private val editTargetStore: EditTargetStore,
    private val profileRefreshBus: ProfileRefreshBus,
) : ViewModel() {

    private val artworkId: Int? = savedStateHandle.get<String>("postId")?.toIntOrNull()
    private val source: String? = savedStateHandle.get<String>("source")
    private val isFromProfile: Boolean = source == "profile"

    private var currentUserId: Int = 0

    private val _uiState = MutableStateFlow(ArtDetailUiState())
    val uiState: StateFlow<ArtDetailUiState> = _uiState.asStateFlow()

    /** One-shot: emitted after a successful delete so the screen can pop back. */
    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted = _deleted.receiveAsFlow()

    /** One-shot: emitted after a successful block so the screen can close the sheet and pop back. */
    private val _blocked = Channel<Unit>(Channel.BUFFERED)
    val blocked = _blocked.receiveAsFlow()

    /** Reasons chosen on the report step, reused as the message when blocking the art. */
    private var lastReportMessage: String = ""

    init {
        load()
    }

    private fun load() {
        val id = artworkId
        if (id == null) {
            _uiState.update { it.copy(isLoading = false, error = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = false) }
            val detailJob = async { repository.getArtworkDetail(id) }
            // Opened from Profile → no "More like this" (it should read like a clean preview).
            val similarJob = if (isFromProfile) null else async { repository.getSimilarArtworks(id) }
            val meJob = async { profileRepository.getMyProfile() }
            val detailRes = detailJob.await()
            val similarRes = similarJob?.await()
            val meId = (meJob.await() as? ApiResult.Success)?.data?.id
            currentUserId = meId ?: 0

            if (detailRes is ApiResult.Success) {
                val post = detailRes.data
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = false,
                        post = post,
                        moreLikeThis = (similarRes as? ApiResult.Success)?.data.orEmpty(),
                        isOwn = meId != null && post.ownerId == meId,
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = true) }
            }
        }
    }

    /** Stage this artwork for the edit flow before navigating to the New Art screen. */
    fun prepareEdit() {
        artworkId?.let { editTargetStore.setArtwork(it) }
    }

    fun deleteArtwork() {
        val id = artworkId ?: return
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            when (uploadRepository.deleteArtwork(id)) {
                is ApiResult.Success -> _deleted.send(Unit)
                is ApiResult.Error -> _uiState.update { it.copy(isDeleting = false) }
            }
        }
    }

    fun onLikeToggled() {
        val id = artworkId ?: return
        val post = _uiState.value.post ?: return
        val nowLiked = !post.isLiked
        setLiked(nowLiked)
        viewModelScope.launch {
            val result = if (nowLiked) repository.likeArtwork(id) else repository.unlikeArtwork(id)
            if (result is ApiResult.Error) {
                setLiked(!nowLiked)   // revert on failure
            } else {
                // Keep the Profile "Liked" tab in sync — an unliked art drops out on return.
                profileRefreshBus.signal()
            }
        }
    }

    private fun setLiked(liked: Boolean) {
        _uiState.update { state ->
            val p = state.post ?: return@update state
            state.copy(
                post = p.copy(
                    isLiked = liked,
                    likeCount = (p.likeCount + if (liked) 1 else -1).coerceAtLeast(0),
                ),
            )
        }
    }

    // ── Report / block ────────────────────────────────────────────────────────

    fun submitReport(message: String) {
        val id = artworkId ?: return
        if (_uiState.value.isReporting) return
        _uiState.update { it.copy(isReporting = true, actionError = null) }
        viewModelScope.launch {
            when (profileRepository.reportArtwork(id, message)) {
                is ApiResult.Success -> {
                    lastReportMessage = message
                    _uiState.update { it.copy(isReporting = false, reportSent = true) }
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isReporting = false, actionError = "Couldn't send the report. Please try again.")
                }
            }
        }
    }

    fun blockArt() {
        val id = artworkId ?: return
        if (_uiState.value.isBlocking) return
        _uiState.update { it.copy(isBlocking = true, actionError = null) }
        viewModelScope.launch {
            when (profileRepository.blockArtwork(id, lastReportMessage.ifBlank { "Reported from app" })) {
                is ApiResult.Success -> {
                    profileRefreshBus.signal()
                    _blocked.send(Unit)
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isBlocking = false, actionError = "Couldn't block this art. Please try again.")
                }
            }
        }
    }

    fun blockUser() {
        val ownerId = _uiState.value.post?.ownerId ?: return
        if (_uiState.value.isBlocking) return
        _uiState.update { it.copy(isBlocking = true, actionError = null) }
        viewModelScope.launch {
            when (profileRepository.blockUser(ownerId)) {
                is ApiResult.Success -> {
                    profileRefreshBus.signal()
                    _blocked.send(Unit)
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isBlocking = false, actionError = "Couldn't block this user. Please try again.")
                }
            }
        }
    }

    // ── Send-message invitation ────────────────────────────────────────────────

    /** Fetch remaining invites for the sheet footer when it opens. */
    fun onInviteSheetOpened() {
        val ownerId = _uiState.value.post?.ownerId ?: return
        viewModelScope.launch {
            val res = messagesRepository.resolveChatroom(ownerId)
            if (res is ApiResult.Success) {
                _uiState.update { it.copy(invitationsLeft = res.data.remainingInvites) }
            }
        }
    }

    /** Send the invitation message, sharing this artwork (image_id = artworkId). */
    fun onSendInvite(text: String) {
        val ownerId = _uiState.value.post?.ownerId ?: return
        if (text.isBlank() || _uiState.value.isSendingInvite) return
        _uiState.update { it.copy(isSendingInvite = true, actionError = null) }
        viewModelScope.launch {
            val cid = UUID.randomUUID().toString()
            when (messagesRepository.sendMessage(ownerId, currentUserId, text, imageId = artworkId, clientMessageId = cid)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isSendingInvite = false, inviteSent = true,
                        invitationsLeft = it.invitationsLeft?.let { n -> (n - 1).coerceAtLeast(0) })
                }
                is ApiResult.Error.Blocked -> _uiState.update {
                    it.copy(isSendingInvite = false,
                        actionError = "You can't message this profile right now.")
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isSendingInvite = false, actionError = "Couldn't send your invitation. Please try again.")
                }
            }
        }
    }

    fun onInviteSheetClosed() = _uiState.update { it.copy(isSendingInvite = false, inviteSent = false) }

    /** Reset moderation flags when the report sheet is dismissed (so reopening starts fresh). */
    fun onReportSheetClosed() = _uiState.update {
        it.copy(isReporting = false, reportSent = false, isBlocking = false, actionError = null)
    }

    fun onActionErrorShown() = _uiState.update { it.copy(actionError = null) }
}
