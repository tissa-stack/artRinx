package com.rinx.artRINXapp.feature.home.presentation.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.network.userMessage
import com.rinx.artRINXapp.feature.home.data.local.CurationPreviewStore
import com.rinx.artRINXapp.feature.home.data.local.DetailCache
import com.rinx.artRINXapp.feature.home.domain.model.CurationItem
import com.rinx.artRINXapp.feature.home.domain.model.SendMode
import com.rinx.artRINXapp.feature.home.domain.repository.HomeRepository
import com.rinx.artRINXapp.feature.notifications.domain.repository.MessagesRepository
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import com.rinx.artRINXapp.feature.upload.domain.EditTargetStore
import com.rinx.artRINXapp.feature.upload.domain.repository.CurationRepository
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
data class CurationDetailUiState(
    val curation: CurationItem? = null,
    val moreLikeThis: List<CurationItem> = emptyList(),
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val isLoading: Boolean = true,
    val error: Boolean = false,
    /** True when the current user owns this curation. */
    val isOwn: Boolean = false,
    /** True only for my own collection opened from my Profile tab → show Edit/Delete.
     *  Opening my own collection from any other route (search, home, notifications…) hides them. */
    val canManage: Boolean = false,
    /** Whether tapping an artwork card opens its detail. Disabled ONLY for my own collection opened
     *  from my Profile tab; navigable everywhere else (other users' collections, home, search). */
    val artworksNavigable: Boolean = true,
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
    val sendMode: SendMode = SendMode.INVITE,
    /** False until the owner conversation state is resolved — sheet shows a loader, not INVITE. */
    val sendModeReady: Boolean = false,
)

@HiltViewModel
class CurationDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HomeRepository,
    private val curationPreviewStore: CurationPreviewStore,
    private val profileRepository: ProfileRepository,
    private val messagesRepository: MessagesRepository,
    private val curationRepository: CurationRepository,
    private val editTargetStore: EditTargetStore,
    private val likeManager: com.rinx.artRINXapp.feature.home.domain.LikeManager,
    private val likeBus: com.rinx.artRINXapp.core.util.LikeBus,
    private val detailCache: DetailCache,
    private val blockedArtworkBus: com.rinx.artRINXapp.core.util.BlockedArtworkBus,
    private val blockedUserBus: com.rinx.artRINXapp.core.util.BlockedUserBus,
) : ViewModel() {

    private val curationId: Int? = savedStateHandle.get<String>("curationId")?.toIntOrNull()
    private val source: String? = savedStateHandle.get<String>("source")
    private val isFromProfile: Boolean = source == "profile"

    private var currentUserId: Int = 0

    /** Non-null while the user has a like toggle outstanding — keeps a stale refresh from clobbering it. */
    private var pendingLike: Boolean? = null

    // Seed synchronously from cache so a re-open renders instantly with no shimmer (SWR).
    private val _uiState = MutableStateFlow(seedFromCache())
    val uiState: StateFlow<CurationDetailUiState> = _uiState.asStateFlow()

    private fun seedFromCache(): CurationDetailUiState {
        val id = curationId ?: return CurationDetailUiState(isLoading = true)
        val cached = detailCache.peekCuration(id) ?: return CurationDetailUiState(isLoading = true)
        // Resolve ownership synchronously (cached entry, or the cached current-user id vs author) so
        // Edit/Delete render immediately on open instead of popping in after the network resolves.
        val meId = profileRepository.cachedCurrentUserId()
        val isOwn = cached.isOwn || (meId != null && meId == cached.curation.authorId)
        return CurationDetailUiState(
            curation = cached.curation,
            moreLikeThis = cached.more,
            likeCount = cached.curation.likeCount,
            isLiked = cached.curation.isLiked,
            isOwn = isOwn,
            canManage = isFromProfile && isOwn,
            artworksNavigable = !(isFromProfile && isOwn),
            isLoading = false,
        )
    }

    /** One-shot: emitted after a successful delete so the screen can pop back. */
    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted = _deleted.receiveAsFlow()

    /** One-shot: emits the success toast text after a block so the screen toasts, closes the sheet & pops. */
    private val _blocked = Channel<String>(Channel.BUFFERED)
    val blocked = _blocked.receiveAsFlow()

    /** One-shot: emitted when the curation is gone server-side (404) so the screen toasts + pops. */
    private val _gone = Channel<Unit>(Channel.BUFFERED)
    val gone = _gone.receiveAsFlow()

    init {
        load()
        observeBlocks()
        observeLikes()
    }

    /** Reflect a like/unlike outcome (optimistic, confirm, or the async revert dispatched by
     *  LikeManager after the network resolves) for THIS curation, even though the toggle ran off-screen. */
    private fun observeLikes() {
        viewModelScope.launch {
            likeBus.curationEvents.collect { u ->
                if (u.id != curationId) return@collect
                _uiState.update { it.copy(isLiked = u.isLiked, likeCount = u.likeCount) }
            }
        }
    }

    /** If an artwork in this open curation gets blocked, drop it from the live deck immediately. */
    private fun observeBlocks() {
        viewModelScope.launch {
            blockedArtworkBus.events.collect { blockedId ->
                val idStr = blockedId.toString()
                _uiState.update { st ->
                    val cur = st.curation ?: return@update st
                    if (idStr !in cur.artworkIds) return@update st
                    val keptUrls = ArrayList<String>(cur.artworkUrls.size)
                    val keptIds = ArrayList<String>(cur.artworkIds.size)
                    cur.artworkUrls.indices.forEach { i ->
                        if (cur.artworkIds.getOrNull(i) != idStr) {
                            keptUrls += cur.artworkUrls[i]
                            keptIds += (cur.artworkIds.getOrNull(i) ?: "")
                        }
                    }
                    st.copy(curation = cur.copy(artworkUrls = keptUrls, artworkIds = keptIds))
                }
            }
        }
        // Blocking an artist drops their curations from the "More like this" rail right away.
        viewModelScope.launch {
            blockedUserBus.events.collect { blockedUserId ->
                _uiState.update { st ->
                    val filtered = st.moreLikeThis.filterNot { it.authorId == blockedUserId }
                    if (filtered.size == st.moreLikeThis.size) st else st.copy(moreLikeThis = filtered)
                }
            }
        }
    }

    private fun load() {
        val id = curationId
        if (id == null) {
            _uiState.update { it.copy(isLoading = false, error = true) }
            return
        }
        val hasCache = _uiState.value.curation != null
        viewModelScope.launch {
            // Cache hit → already rendered; revalidate silently (don't flip isLoading/shimmer).
            if (!hasCache) _uiState.update { it.copy(isLoading = true, error = false) }
            val detailJob = async { repository.getCurationDetail(id) }
            // Opened from Profile → no "More like this" (clean preview).
            val moreJob = if (isFromProfile) null else async { repository.getMoreCurations() }
            val meJob = async { profileRepository.getMyProfile() }
            val detailRes = detailJob.await()
            val moreRes = moreJob?.await()
            val meId = (meJob.await() as? ApiResult.Success)?.data?.id
            currentUserId = meId ?: 0

            if (detailRes is ApiResult.Success) {
                val fetched = detailRes.data
                // Open with the SAME images the user saw on the home card (preview deck) first, then
                // the curation's remaining artworks. Reorder urls + ids together so a card tap opens
                // the right artwork.
                val (orderedUrls, orderedIds) = reorderArtworks(
                    detailUrls = fetched.artworkUrls,
                    detailIds = fetched.artworkIds,
                    previewUrls = curationPreviewStore.orderFor(fetched.id),
                )
                val reordered = fetched.copy(artworkUrls = orderedUrls, artworkIds = orderedIds)
                // Never let a stale server read overwrite the user's just-made like.
                val isLiked = pendingLike ?: reordered.isLiked
                val likeCount = if (pendingLike != null) _uiState.value.likeCount else reordered.likeCount
                val curation = reordered.copy(isLiked = isLiked, likeCount = likeCount)
                val more = ((moreRes as? ApiResult.Success)?.data ?: _uiState.value.moreLikeThis)
                    .filter { it.id != curation.id }
                // Remember the previews of the "More like this" curations too, so tapping one
                // opens it with the same first images.
                more.forEach { curationPreviewStore.put(it.id, it.artworkUrls) }
                val isOwn = meId != null && curation.authorId == meId
                detailCache.putCuration(id, curation, more, isOwn)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = false,
                        curation = curation,
                        moreLikeThis = more,
                        likeCount = likeCount,
                        isLiked = isLiked,
                        isOwn = isOwn,
                        canManage = isFromProfile && isOwn,
                        artworksNavigable = !(isFromProfile && isOwn),
                    )
                }
                // Resolve the send mode now (before the sheet can open) to avoid an invite→message flicker.
                if (!isOwn) resolveSendMode(curation.authorId)
            } else if (detailRes is ApiResult.Error.NotFound) {
                // Definitively gone server-side — drop any stale cache and leave the screen.
                detailCache.evictCuration(id)
                _gone.send(Unit)
            } else if (!hasCache) {
                _uiState.update { it.copy(isLoading = false, error = true) }
            }
            // else: transient refresh failure with a cache showing → keep it silently (no error/spinner).
        }
    }

    /** Resolve [SendMode] + remaining new-chat count from the chatroom-resolution endpoint. */
    private fun resolveSendMode(ownerId: Int?) {
        if (ownerId == null) {
            _uiState.update { it.copy(sendModeReady = true) }
            return
        }
        viewModelScope.launch {
            val r = (messagesRepository.resolveChatroom(ownerId) as? ApiResult.Success)?.data
            _uiState.update {
                if (r == null) it.copy(sendModeReady = true) // fallback: keep default INVITE
                else it.copy(
                    invitationsLeft = r.remainingInvites,
                    sendMode = SendMode.resolve(
                        canMessage = r.canMessage ?: true,
                        chatroomId = r.chatroomId,
                        iBlocked = r.iBlocked,
                        theyBlocked = r.theyBlocked,
                        blockReason = r.blockReason,
                    ),
                    sendModeReady = true,
                )
            }
        }
    }

    /** Stage this curation for the edit flow before navigating to the New Curation screen. */
    fun prepareEdit() {
        curationId?.let { editTargetStore.setCuration(it) }
    }

    fun deleteCuration() {
        val id = curationId ?: return
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            when (curationRepository.deleteCuration(id)) {
                is ApiResult.Success -> {
                    detailCache.evictCuration(id)
                    _deleted.send(Unit)
                }
                is ApiResult.Error -> _uiState.update { it.copy(isDeleting = false) }
            }
        }
    }

    /**
     * Orders the curation's artworks to match the home preview deck: the tapped card's images
     * ([previewUrls]) first (in that order), then the rest of the detail's artworks — so the detail
     * always leads with what the user tapped. Keeps ids aligned with urls (preview-only urls not in
     * the detail set get an empty id → not tap-navigable). Empty preview → detail's natural order.
     */
    private fun reorderArtworks(
        detailUrls: List<String>,
        detailIds: List<String>,
        previewUrls: List<String>,
    ): Pair<List<String>, List<String>> {
        val urlToId = detailUrls.zip(detailIds).toMap()
        val seen = LinkedHashSet<String>()
        val urls = mutableListOf<String>()
        val ids = mutableListOf<String>()
        fun add(url: String) {
            if (url.isBlank() || !seen.add(url)) return
            urls += url
            ids += urlToId[url].orEmpty()
        }
        // Only reorder preview urls that still exist in the (blocked-filtered) detail set, so a
        // blocked artwork's url lingering in the preview store can't re-add it to the deck.
        val detailSet = detailUrls.toHashSet()
        previewUrls.filter { it in detailSet }.forEach(::add)  // tapped-card images first
        detailUrls.forEach(::add)    // then the canonical remainder
        return urls to ids
    }

    fun onLikeToggled() {
        val id = curationId ?: return
        val nowLiked = !_uiState.value.isLiked
        pendingLike = nowLiked // guards the initial load() from clobbering a like tapped mid-load
        setLiked(nowLiked)     // instant local feedback
        // Durable state + network run on LikeManager's app scope so leaving this screen can't cancel
        // them; a hard-failure revert (or "already liked" convergence) comes back via observeLikes.
        likeManager.toggleCuration(id, nowLiked, _uiState.value.likeCount)
    }

    private fun setLiked(liked: Boolean) {
        _uiState.update { state ->
            state.copy(
                isLiked = liked,
                likeCount = (state.likeCount + if (liked) 1 else -1).coerceAtLeast(0),
            )
        }
    }

    // ── Report / block ────────────────────────────────────────────────────────

    fun submitReport(message: String) {
        val id = curationId ?: return
        if (_uiState.value.isReporting) return
        _uiState.update { it.copy(isReporting = true, actionError = null) }
        viewModelScope.launch {
            when (val r = profileRepository.reportCuration(id, message)) {
                is ApiResult.Success -> _uiState.update { it.copy(isReporting = false, reportSent = true) }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isReporting = false, actionError = r.userMessage("Couldn't send the report. Please try again."))
                }
            }
        }
    }

    fun blockUser() {
        val ownerId = _uiState.value.curation?.authorId ?: return
        if (_uiState.value.isBlocking) return
        _uiState.update { it.copy(isBlocking = true, actionError = null) }
        viewModelScope.launch {
            when (val r = profileRepository.blockUser(ownerId)) {
                is ApiResult.Success -> {
                    curationId?.let { detailCache.evictCuration(it) }
                    _blocked.send("Blocked ${_uiState.value.curation?.curatorName ?: "user"}")
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isBlocking = false, actionError = r.userMessage("Couldn't block this user. Please try again."))
                }
            }
        }
    }

    // ── Send-message invitation ────────────────────────────────────────────────

    fun onInviteSheetOpened() {
        // Refresh on open (covers the case where the initial load resolution is still in flight).
        resolveSendMode(_uiState.value.curation?.authorId)
    }

    /** Invitation to the curator (text-only — a curation has no artwork image_id). */
    fun onSendInvite(text: String) {
        val ownerId = _uiState.value.curation?.authorId ?: return
        if (text.isBlank() || _uiState.value.isSendingInvite) return
        _uiState.update { it.copy(isSendingInvite = true, actionError = null) }
        viewModelScope.launch {
            val cid = UUID.randomUUID().toString()
            when (val res = messagesRepository.sendMessage(ownerId, currentUserId, text, clientMessageId = cid)) {
                is ApiResult.Success -> _uiState.update {
                    val left = if (it.sendMode == SendMode.INVITE)
                        it.invitationsLeft?.let { n -> (n - 1).coerceAtLeast(0) } else it.invitationsLeft
                    it.copy(isSendingInvite = false, inviteSent = true, invitationsLeft = left)
                }
                // Surface the server's real reason (e.g. 403 "Cannot message blocked user").
                is ApiResult.Error -> _uiState.update {
                    it.copy(isSendingInvite = false,
                        actionError = res.userMessage("Couldn't send your invitation. Please try again."))
                }
            }
        }
    }

    fun onInviteSheetClosed() = _uiState.update { it.copy(isSendingInvite = false, inviteSent = false) }

    fun onReportSheetClosed() = _uiState.update {
        it.copy(isReporting = false, reportSent = false, isBlocking = false, actionError = null)
    }

    fun onActionErrorShown() = _uiState.update { it.copy(actionError = null) }
}
