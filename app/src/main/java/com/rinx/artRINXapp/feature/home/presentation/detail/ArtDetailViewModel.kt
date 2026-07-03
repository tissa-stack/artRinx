package com.rinx.artRINXapp.feature.home.presentation.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.network.userMessage
import com.rinx.artRINXapp.core.paging.ListPage
import com.rinx.artRINXapp.core.paging.toLoadMoreMessage
import com.rinx.artRINXapp.core.util.LikeBus
import com.rinx.artRINXapp.core.util.ProfileRefreshBus
import com.rinx.artRINXapp.feature.home.data.local.DetailCache
import com.rinx.artRINXapp.feature.home.domain.model.ArtworkItem
import com.rinx.artRINXapp.feature.home.domain.model.SendMode
import com.rinx.artRINXapp.feature.home.domain.model.ShoppablePost
import com.rinx.artRINXapp.feature.home.domain.repository.HomeRepository
import com.rinx.artRINXapp.feature.notifications.domain.repository.MessagesRepository
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import com.rinx.artRINXapp.feature.upload.domain.EditTargetStore
import com.rinx.artRINXapp.feature.upload.domain.repository.UploadRepository
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
    /** Horizontal infinite-scroll state for the "More like this" rail. */
    val moreLikeThisPaging: ListPage = ListPage(),
    val isLoading: Boolean = true,
    val error: Boolean = false,
    /** True when the current user owns this artwork → show Edit/Delete instead of Report. */
    val isOwn: Boolean = false,
    /** True when this detail was opened from the user's own Profile tab. Edit/Delete are shown ONLY
     *  for own artwork opened from there; from any other flow (feed/search/curation) no action shows. */
    val isFromProfile: Boolean = false,
    /** False until ownership is known. The top-bar action stays hidden until then so we never
     *  flash Report on the user's own art before [isOwn] resolves. */
    val ownershipResolved: Boolean = false,
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
    /** Drives the send sheet's framing (invite vs plain message vs disabled reason). */
    val sendMode: SendMode = SendMode.INVITE,
    /** False until the owner conversation state is resolved — sheet shows a loader, not INVITE. */
    val sendModeReady: Boolean = false,
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
    private val likeBus: LikeBus,
    private val likeManager: com.rinx.artRINXapp.feature.home.domain.LikeManager,
    private val blockedArtworkBus: com.rinx.artRINXapp.core.util.BlockedArtworkBus,
    private val blockedUserBus: com.rinx.artRINXapp.core.util.BlockedUserBus,
    private val blockedArtworkStore: com.rinx.artRINXapp.core.util.BlockedArtworkStore,
    private val blockedUsersStore: com.rinx.artRINXapp.core.util.BlockedUsersStore,
    private val detailCache: DetailCache,
) : ViewModel() {

    private val artworkId: Int? = savedStateHandle.get<String>("postId")?.toIntOrNull()
    private val source: String? = savedStateHandle.get<String>("source")
    private val isFromProfile: Boolean = source == "profile"

    private var currentUserId: Int = 0

    /** Non-null while the user has a like toggle outstanding — keeps a stale refresh from clobbering it. */
    private var pendingLike: Boolean? = null

    // Seed synchronously from cache so a re-open renders instantly with no shimmer (SWR).
    private val _uiState = MutableStateFlow(seedFromCache())
    val uiState: StateFlow<ArtDetailUiState> = _uiState.asStateFlow()

    private fun seedFromCache(): ArtDetailUiState {
        val id = artworkId ?: return ArtDetailUiState(isLoading = true)
        val cached = detailCache.peekArtwork(id) ?: return ArtDetailUiState(isLoading = true)
        return ArtDetailUiState(
            post = cached.post,
            moreLikeThis = cached.similar,
            isLoading = false,
            isOwn = cached.isOwn,
            isFromProfile = isFromProfile,
            ownershipResolved = true,
        )
    }

    /** One-shot: emitted after a successful delete so the screen can pop back. */
    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted = _deleted.receiveAsFlow()

    /** One-shot: emitted when the artwork is gone server-side (404) so the screen toasts + pops. */
    private val _gone = Channel<Unit>(Channel.BUFFERED)
    val gone = _gone.receiveAsFlow()

    /** One-shot block outcome so the screen toasts, closes the sheet, and leaves. [wasUserBlock]
     *  distinguishes blocking the USER (→ exit to a safe tab, since the previous screen may be the
     *  blocked artist's now-broken art/detail) from blocking a single ART (→ plain one-level back;
     *  the artist isn't blocked so the previous screen is still valid). */
    data class BlockOutcome(val message: String, val wasUserBlock: Boolean)
    private val _blocked = Channel<BlockOutcome>(Channel.BUFFERED)
    val blocked = _blocked.receiveAsFlow()

    /** Reasons chosen on the report step, reused as the message when blocking the art. */
    private var lastReportMessage: String = ""

    /** True while THIS screen's own [blockArt] is in flight. It signals [blockedArtworkBus] to notify
     *  other screens, and this VM also observes that bus — so it would receive its own emission and
     *  fire a second navigation (`_gone`) on top of the intended `_blocked` one, double-popping the
     *  back stack into a blank screen. The flag lets [observeBlocks] skip that self-echo; an external
     *  block of this same artwork (flag false) still auto-closes the back-stacked detail. */
    private var selfBlockingArt = false

    /** Same self-echo guard for blocking the artist. [ProfileRepository.blockUser] itself signals
     *  [blockedUserBus] (so it fires before we even see Success), and this VM observes that bus — its
     *  self-echo would double-pop the back stack. Set before the repo call, consumed by [observeBlocks],
     *  and reset on failure so a later external block of this artist still auto-closes the detail. */
    private var selfBlockingUser = false

    init {
        load()
        observeBlocks()
        observeLikes()
    }

    /** Reflect a like/unlike outcome (optimistic, confirm, or the async revert dispatched by
     *  LikeManager after the network resolves) for THIS artwork, even though the toggle ran off-screen. */
    private fun observeLikes() {
        viewModelScope.launch {
            likeBus.events.collect { u ->
                if (u.id != artworkId) return@collect
                _uiState.update { st ->
                    val p = st.post ?: return@update st
                    st.copy(post = p.copy(isLiked = u.isLiked, likeCount = u.likeCount))
                }
            }
        }
    }

    /** Drop a blocked artwork — or any art uploaded by a blocked user — from the "More like this"
     *  rail immediately, so a block made elsewhere (e.g. on a rail item's own detail) is reflected
     *  the moment we return here, without waiting for a reload. */
    private fun observeBlocks() {
        viewModelScope.launch {
            blockedArtworkBus.events.collect { blockedId ->
                // THIS artwork was blocked → treat as gone: evict + leave the screen.
                if (blockedId == artworkId) {
                    // Skip our own echo: blockArt() already navigates via `_blocked` (with the right
                    // "Art blocked" toast). Reacting here too would double-pop into a blank screen.
                    if (selfBlockingArt) {
                        selfBlockingArt = false
                        return@collect
                    }
                    artworkId?.let { detailCache.evictArtwork(it) }
                    _gone.send(Unit)
                    return@collect
                }
                val idStr = blockedId.toString()
                _uiState.update { st ->
                    if (st.moreLikeThis.none { it.id == idStr }) st
                    else st.copy(moreLikeThis = st.moreLikeThis.filterNot { it.id == idStr })
                }
            }
        }
        viewModelScope.launch {
            blockedUserBus.events.collect { blockedUserId ->
                // The UPLOADER of THIS artwork was blocked → the artwork is gone too, so the
                // back-stacked detail auto-closes to the neutral "not available" instead of lingering.
                val post = _uiState.value.post
                if (post != null && post.ownerId == blockedUserId) {
                    detailCache.evictByOwner(blockedUserId)
                    // Skip our own echo: blockUser() already navigates via `_blocked`. Cache is still
                    // evicted above; only the redundant `_gone` (second pop → blank screen) is skipped.
                    if (selfBlockingUser) {
                        selfBlockingUser = false
                        return@collect
                    }
                    _gone.send(Unit)
                    return@collect
                }
                _uiState.update { st ->
                    val filtered = st.moreLikeThis.filterNot { it.ownerId == blockedUserId }
                    if (filtered.size == st.moreLikeThis.size) st else st.copy(moreLikeThis = filtered)
                }
            }
        }
    }

    private fun load() {
        val id = artworkId
        if (id == null) {
            _uiState.update { it.copy(isLoading = false, error = true) }
            return
        }
        val hasCache = _uiState.value.post != null
        viewModelScope.launch {
            // Cache hit → already rendered; revalidate silently (don't flip isLoading/shimmer).
            if (!hasCache) _uiState.update { it.copy(isLoading = true, error = false) }
            val detailJob = async { repository.getArtworkDetail(id) }
            // Opened from Profile → no "More like this" (it should read like a clean preview).
            val similarJob = if (isFromProfile) null else async { repository.getSimilarArtworks(id, 1, SIMILAR_SIZE) }
            val meJob = async { profileRepository.getMyProfile() }
            val detailRes = detailJob.await()
            val similarRes = similarJob?.await()
            val meId = (meJob.await() as? ApiResult.Success)?.data?.id
            currentUserId = meId ?: 0

            if (detailRes is ApiResult.Success) {
                var post = detailRes.data
                // Never let a stale server read overwrite the user's just-made like.
                pendingLike?.let { liked ->
                    post = post.copy(isLiked = liked, likeCount = _uiState.value.post?.likeCount ?: post.likeCount)
                }
                // Keep prior similar list if this refresh's similar call failed/absent.
                val similarPage = (similarRes as? ApiResult.Success)?.data
                val similar = similarPage?.items ?: _uiState.value.moreLikeThis
                val isOwn = meId != null && post.ownerId == meId
                detailCache.putArtwork(id, post, similar, isOwn)
                _uiState.update {
                    it.copy(
                        isLoading = false, error = false, post = post, moreLikeThis = similar,
                        // Reset the rail's paging to page 1 when we got a fresh page; else keep prior.
                        moreLikeThisPaging = if (similarPage != null) {
                            ListPage(page = 1, hasMore = !similarPage.endReached)
                        } else {
                            it.moreLikeThisPaging
                        },
                        isOwn = isOwn, isFromProfile = isFromProfile, ownershipResolved = true,
                    )
                }
                // Resolve the conversation state with the owner so the send sheet shows the right
                // framing (plain message for an active chat, not a fresh invitation).
                if (!isOwn) post.ownerId?.let { resolveSendMode(it) }
            } else if (detailRes is ApiResult.Error.NotFound) {
                // Definitively gone (deleted/removed server-side) — drop any stale cache so the feed
                // and a future re-open don't resurrect it, then leave the screen with a toast.
                detailCache.evictArtwork(id)
                _gone.send(Unit)
            } else if (!hasCache) {
                _uiState.update { it.copy(isLoading = false, error = true) }
            }
            // else: transient refresh failure with a cache showing → keep it silently (no error/spinner).
        }
    }

    /** Fetch the owner's public profile to derive the [SendMode] + remaining new-chat count. */
    private fun resolveSendMode(ownerId: Int) {
        // I've blocked this owner → resolve instantly, no network. Avoids the send sheet spinning
        // forever if the owner's profile endpoint is slow/500 (common on a mutual block).
        if (blockedUsersStore.isBlocked(ownerId)) {
            _uiState.update { it.copy(sendMode = SendMode.BLOCKED_BY_ME, sendModeReady = true) }
            return
        }
        viewModelScope.launch {
            val pub = (profileRepository.getPublicProfile(ownerId) as? ApiResult.Success)?.data
            _uiState.update {
                if (pub == null) it.copy(sendModeReady = true) // fallback: keep default INVITE
                else it.copy(
                    sendMode = SendMode.resolve(
                        canMessage = pub.canMessage,
                        chatroomId = pub.chatroomId,
                        iBlocked = pub.iBlocked,
                        theyBlocked = pub.theyBlocked,
                        blockReason = pub.blockReason,
                    ),
                    sendModeReady = true,
                )
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
                is ApiResult.Success -> {
                    detailCache.evictArtwork(id)
                    _deleted.send(Unit)
                }
                is ApiResult.Error -> _uiState.update { it.copy(isDeleting = false) }
            }
        }
    }

    fun onLikeToggled() {
        val id = artworkId ?: return
        val post = _uiState.value.post ?: return
        val nowLiked = !post.isLiked
        pendingLike = nowLiked // guards the initial load() from clobbering a like tapped mid-load
        setLiked(nowLiked)     // instant local feedback
        // Durable state + network run on LikeManager's app scope so leaving this screen can't cancel
        // them; a hard-failure revert (or "already liked" convergence) comes back via observeLikes.
        likeManager.toggleArtwork(id, nowLiked, _uiState.value.post?.likeCount ?: post.likeCount)
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
            when (val r = profileRepository.reportArtwork(id, message)) {
                is ApiResult.Success -> {
                    lastReportMessage = message
                    _uiState.update { it.copy(isReporting = false, reportSent = true) }
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isReporting = false, actionError = r.userMessage("Couldn't send the report. Please try again."))
                }
            }
        }
    }

    fun blockArt() {
        val id = artworkId ?: return
        if (_uiState.value.isBlocking) return
        _uiState.update { it.copy(isBlocking = true, actionError = null) }
        viewModelScope.launch {
            when (val r = profileRepository.blockArtwork(id, lastReportMessage.ifBlank { "Reported from app" })) {
                is ApiResult.Success -> {
                    // Record session-wide so the data layer filters it out of EVERY list (cached or
                    // fresh, any screen, on back-navigation) — not just the currently-live ones.
                    blockedArtworkStore.add(id)
                    detailCache.evictArtwork(id)
                    // Drop it from every live on-screen list immediately (no refresh wait). Mark the
                    // self-block first so observeBlocks() ignores our own echo (avoids a double pop).
                    selfBlockingArt = true
                    blockedArtworkBus.signal(id)
                    profileRefreshBus.signal()
                    _blocked.send(BlockOutcome("Art blocked", wasUserBlock = false))
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isBlocking = false, actionError = r.userMessage("Couldn't block this art. Please try again."))
                }
            }
        }
    }

    fun blockUser() {
        val ownerId = _uiState.value.post?.ownerId ?: return
        if (_uiState.value.isBlocking) return
        _uiState.update { it.copy(isBlocking = true, actionError = null) }
        viewModelScope.launch {
            // blockUser() signals blockedUserBus inside the repo, so arm the self-echo guard first.
            selfBlockingUser = true
            when (val r = profileRepository.blockUser(ownerId)) {
                is ApiResult.Success -> {
                    artworkId?.let { detailCache.evictArtwork(it) }
                    profileRefreshBus.signal()
                    _blocked.send(BlockOutcome("Blocked ${_uiState.value.post?.artistName ?: "user"}", wasUserBlock = true))
                }
                is ApiResult.Error -> {
                    // No bus signal fired on failure → disarm so a later external block isn't swallowed.
                    selfBlockingUser = false
                    _uiState.update {
                        it.copy(isBlocking = false, actionError = r.userMessage("Couldn't block this user. Please try again."))
                    }
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
            when (val res = messagesRepository.sendMessage(ownerId, currentUserId, text, imageId = artworkId, clientMessageId = cid)) {
                is ApiResult.Success -> _uiState.update {
                    // Only a brand-new invite spends from the monthly new-chat quota.
                    val left = if (it.sendMode == SendMode.INVITE)
                        it.invitationsLeft?.let { n -> (n - 1).coerceAtLeast(0) } else it.invitationsLeft
                    it.copy(isSendingInvite = false, inviteSent = true, invitationsLeft = left)
                }
                // Surface the server's real reason (e.g. 403 "Cannot message blocked user") instead
                // of a generic string; covers Blocked and every other error uniformly.
                is ApiResult.Error -> _uiState.update {
                    it.copy(isSendingInvite = false,
                        actionError = res.userMessage("Couldn't send your invitation. Please try again."))
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

    /** Load the next page of "More like this" as the rail scrolls right (append, dedupe by id). */
    fun loadMoreSimilar() {
        val id = artworkId ?: return
        val st = _uiState.value
        if (st.moreLikeThisPaging.blocked) return
        _uiState.update { it.copy(moreLikeThisPaging = it.moreLikeThisPaging.copy(isLoadingMore = true)) }
        viewModelScope.launch {
            val next = st.moreLikeThisPaging.page + 1
            when (val res = repository.getSimilarArtworks(id, next, SIMILAR_SIZE)) {
                is ApiResult.Success -> _uiState.update { s ->
                    val seen = s.moreLikeThis.mapTo(HashSet()) { it.id }
                    val merged = s.moreLikeThis + res.data.items.filter { seen.add(it.id) }
                    s.copy(
                        moreLikeThis = merged,
                        moreLikeThisPaging = s.moreLikeThisPaging.copy(
                            page = next, isLoadingMore = false,
                            hasMore = !res.data.endReached, loadMoreError = null,
                        ),
                    )
                }
                is ApiResult.Error -> _uiState.update { s ->
                    s.copy(moreLikeThisPaging = s.moreLikeThisPaging.copy(
                        isLoadingMore = false, loadMoreError = res.toLoadMoreMessage(),
                    ))
                }
            }
        }
    }

    /** Rail "Retry": clear the error and try the same next page again. */
    fun retryLoadMoreSimilar() {
        _uiState.update { it.copy(moreLikeThisPaging = it.moreLikeThisPaging.copy(loadMoreError = null)) }
        loadMoreSimilar()
    }

    private companion object {
        const val SIMILAR_SIZE = 10
    }
}
