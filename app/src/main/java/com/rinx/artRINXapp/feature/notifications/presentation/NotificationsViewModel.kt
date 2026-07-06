package com.rinx.artRINXapp.feature.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.navigation.DeepLinkRouter
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.network.userMessage
import com.rinx.artRINXapp.core.network.ChatEvent
import com.rinx.artRINXapp.core.network.ChatWebSocketManager
import com.rinx.artRINXapp.feature.events.domain.model.EventDetail
import com.rinx.artRINXapp.feature.events.domain.repository.EventsRepository
import com.rinx.artRINXapp.feature.home.data.local.DetailCache
import com.rinx.artRINXapp.feature.home.domain.repository.HomeRepository
import com.rinx.artRINXapp.feature.notifications.domain.UnreadNotificationsStore
import com.rinx.artRINXapp.feature.notifications.domain.model.ConversationItem
import com.rinx.artRINXapp.feature.notifications.domain.model.NotifTab
import com.rinx.artRINXapp.feature.notifications.domain.model.NotificationItem
import com.rinx.artRINXapp.feature.notifications.domain.model.NotificationKind
import com.rinx.artRINXapp.feature.notifications.domain.repository.MessagesRepository
import com.rinx.artRINXapp.feature.notifications.domain.repository.NotificationsRepository
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lazily-resolved preview for a share/like notification (the payload only carries the target id).
 * For a curation: [imageUrls] are the first few artwork images (the card fan) and [ownerId]/[ownerName]
 * the curator. For an artwork: [imageUrls] is the single image and [ownerId]/[ownerName] the uploader.
 */
data class SharedContentPreview(
    val ownerId: Long? = null,
    val ownerName: String? = null,
    val title: String? = null,
    val imageUrls: List<String> = emptyList(),
)

data class NotificationsUiState(
    val activeTab: NotifTab = NotifTab.NOTIFICATIONS,
    val notifications: List<NotificationItem> = emptyList(),
    /** Resolved share/like previews keyed by target id (curation/artwork). */
    val previews: Map<Long, SharedContentPreview> = emptyMap(),
    val conversations: List<ConversationItem> = emptyList(),
    val messageQuery: String = "",
    val invitationCount: Int = 0,
    val isLoadingConversations: Boolean = true,
    val isLoadingNotifications: Boolean = true,
    val isRefreshing: Boolean = false,
    /** Pull-to-refresh spinner for the Notifications tab (separate from the Messages one). */
    val isRefreshingNotifications: Boolean = false,
    // Set when a load fails AND there is nothing cached to show; cleared on the next success.
    // The UI surfaces these only when the matching list is empty, so a flaky refresh never
    // blanks an already-populated tab (SWR).
    val notificationsError: String? = null,
    val conversationsError: String? = null,
    /**
     * One-shot message for a failed PULL-TO-REFRESH while content is already on screen (the inline error
     * view stays hidden in that case — SWR — so we'd otherwise fail silently). Shown as a toast, then
     * cleared via [consumeRefreshError].
     */
    val refreshError: String? = null,
    // ── Event popup ──────────────────────────────────────────────────────────────
    /** Non-null while the popup is open (null content + isEventLoading = fetching). */
    val isEventPopupOpen: Boolean = false,
    val isEventLoading: Boolean = false,
    val eventPopup: EventDetail? = null,
    /** One-shot error message for a failed/missing event fetch; cleared via [consumeEventError]. */
    val eventError: String? = null,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val messagesRepository: MessagesRepository,
    private val notificationsRepository: NotificationsRepository,
    private val profileRepository: ProfileRepository,
    private val eventsRepository: EventsRepository,
    private val deepLinkRouter: DeepLinkRouter,
    private val unreadStore: UnreadNotificationsStore,
    private val webSocket: ChatWebSocketManager,
    private val homeRepository: HomeRepository,
    private val detailCache: DetailCache,
    private val blockedUserBus: com.rinx.artRINXapp.core.util.BlockedUserBus,
    private val blockedArtworkBus: com.rinx.artRINXapp.core.util.BlockedArtworkBus,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    /**
     * Optimistic "mark read" has no server endpoint, so a server refresh (fired on every WS event)
     * would otherwise resurrect the unread badge. Track each locally-read conversation with the
     * unread count it had when marked read; keep suppressing it across refreshes until the server
     * shows MORE unread than that baseline (a genuinely new message), or the convo disappears.
     */
    private val locallyRead = mutableMapOf<String, Int>()

    /** One-shot guard so a failed notifications load auto-retries at most once (see loadNotifications). */
    private var notificationsAutoRetried = false

    init {
        loadNotifications()
        refreshConversations()
        loadInvitationCount()
        observeUserBlocks()
        observeBlocks()
        viewModelScope.launch {
            webSocket.events.collect { event ->
                when (event) {
                    // Inbox previews / unread badges change on any new or read message — refresh.
                    is ChatEvent.NewMessage, is ChatEvent.Read -> refreshConversations()
                    // A new notification arrived over the socket — refresh the list.
                    is ChatEvent.IncomingNotification -> loadNotifications()
                    else -> Unit
                }
            }
        }
        // A deep-link/push event parks its id here; switch to Notifications + open the popup.
        viewModelScope.launch {
            deepLinkRouter.pendingEventId.collect { id ->
                if (id != null) {
                    deepLinkRouter.consumeEventId()
                    id.toLongOrNull()?.let { eid ->
                        _state.update { it.copy(activeTab = NotifTab.NOTIFICATIONS) }
                        onOpenEvent(eid)
                    }
                }
            }
        }
    }

    fun onTabSelected(tab: NotifTab) {
        _state.update { it.copy(activeTab = tab) }
        when (tab) {
            NotifTab.MESSAGES -> refreshConversations()
            NotifTab.NOTIFICATIONS -> loadNotifications()
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            when (val res = notificationsRepository.getNotifications()) {
                is ApiResult.Success -> {
                    notificationsAutoRetried = false
                    _state.update {
                        it.copy(notifications = res.data, isLoadingNotifications = false, notificationsError = null)
                    }
                    unreadStore.set(res.data.count { !it.isRead })
                }
                is ApiResult.Error -> {
                    // A transient auth/network blip (e.g. a cold-backend token refresh that just timed
                    // out) can leave the list empty. The refresh coordinator caches a failed refresh for
                    // ~5s, so an instant retry is useless — auto-retry ONCE just past that window, by
                    // which time the backend is warm and the refresh succeeds, healing the blip with no
                    // manual tap. Keep the shimmer up during the wait instead of flashing the error.
                    val listEmpty = _state.value.notifications.isEmpty()
                    if (listEmpty && !notificationsAutoRetried) {
                        notificationsAutoRetried = true
                        _state.update { it.copy(isLoadingNotifications = true, notificationsError = null) }
                        delay(NOTIFICATIONS_AUTO_RETRY_DELAY_MS)
                        loadNotifications()
                    } else {
                        _state.update {
                            it.copy(isLoadingNotifications = false, notificationsError = res.userMessage())
                        }
                    }
                }
            }
        }
    }

    /** Pull-to-refresh on the Notifications tab — keeps the list visible (SWR) with a spinner. */
    fun refreshNotifications() {
        _state.update { it.copy(isRefreshingNotifications = true) }
        viewModelScope.launch {
            when (val res = notificationsRepository.getNotifications()) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            notifications = res.data,
                            isRefreshingNotifications = false,
                            isLoadingNotifications = false,
                            notificationsError = null,
                        )
                    }
                    unreadStore.set(res.data.count { !it.isRead })
                }
                // Pull-to-refresh failure: surface a one-shot toast instead of silently stopping the spinner.
                is ApiResult.Error -> _state.update {
                    it.copy(isRefreshingNotifications = false, refreshError = res.userMessage())
                }
            }
        }
    }

    /** Re-show the loading state, then re-fetch — wired to the error view's Retry button. */
    fun retryNotifications() {
        // A user-initiated retry re-enables the single auto-retry too.
        notificationsAutoRetried = false
        _state.update { it.copy(isLoadingNotifications = true, notificationsError = null) }
        loadNotifications()
    }

    fun onMessageQueryChange(q: String) = _state.update { it.copy(messageQuery = q) }

    /** [isUserRefresh] = true when triggered by pull-to-refresh, so the drag spinner is shown. */
    fun refreshConversations(isUserRefresh: Boolean = false) {
        if (isUserRefresh) _state.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            when (val res = messagesRepository.getChatrooms()) {
                is ApiResult.Success -> {
                    // Reconcile optimistic local reads: drop a convo from the suppressed set once the
                    // server shows more unread than the marked-read baseline (new message) or it's gone.
                    locallyRead.entries.retainAll { (id, baseline) ->
                        val server = res.data.firstOrNull { it.id == id }
                        server != null && server.unreadCount <= baseline
                    }
                    val reconciled = res.data.map { c ->
                        if (locallyRead.containsKey(c.id)) c.copy(isUnread = false, unreadCount = 0) else c
                    }
                    _state.update {
                        it.copy(
                            conversations = reconciled,
                            isLoadingConversations = false,
                            isRefreshing = false,
                            conversationsError = null,
                        )
                    }
                }
                is ApiResult.Error -> _state.update {
                    it.copy(
                        isLoadingConversations = false,
                        isRefreshing = false,
                        conversationsError = res.userMessage(),
                        // Explicit pull-to-refresh that fails while the list already has content → toast
                        // (the inline error view only shows when the list is empty, so it'd be silent).
                        refreshError = if (isUserRefresh && it.conversations.isNotEmpty()) {
                            res.userMessage()
                        } else {
                            it.refreshError
                        },
                    )
                }
            }
        }
    }

    fun loadInvitationCount() {
        viewModelScope.launch {
            val info = profileRepository.getInviteInfo()
            if (info is ApiResult.Success) {
                // Messages tab = chat surface → show the new-chats counter, not peer-share invites.
                val newChats = info.data.remainingChatInvites ?: info.data.remainingInvites ?: 0
                _state.update { it.copy(invitationCount = newChats) }
            }
        }
    }

    /**
     * Delete all messages with a user (§7.10). Fire the delete, then refresh from the server so the
     * list reflects the result — the row drops if the server removed it, or stays with a cleared
     * preview if it kept it.
     */
    fun onDeleteConversation(item: ConversationItem) {
        val userId = item.id.toIntOrNull() ?: return
        viewModelScope.launch {
            messagesRepository.deleteChat(userId)
            refreshConversations()
        }
    }

    /** No bulk "mark chat read" endpoint — clear the unread badge locally (optimistic) and remember
     *  the baseline so refreshes don't resurrect it until a genuinely new message arrives. */
    fun onMarkConversationRead(item: ConversationItem) {
        locallyRead[item.id] = item.unreadCount
        _state.update {
            it.copy(conversations = it.conversations.map { c ->
                if (c.id == item.id) c.copy(isUnread = false, unreadCount = 0) else c
            })
        }
    }

    // ── Blocked-user / blocked-artwork live filtering ───────────────────────────────

    /** Drop a blocked user's notification rows (their follow/like/share activity) the moment I block
     *  them — no refresh wait — mirroring HomeViewModel. Unblock re-fetches so the rows return. */
    private fun observeUserBlocks() {
        viewModelScope.launch {
            blockedUserBus.events.collect { blockedUserId ->
                val id = blockedUserId.toLong()
                _state.update { st ->
                    val filtered = st.notifications.filterNot { it.actorId == id || it.organizerId == id }
                    if (filtered.size == st.notifications.size) st else st.copy(notifications = filtered)
                }
                unreadStore.set(_state.value.notifications.count { !it.isRead })
            }
        }
        viewModelScope.launch {
            blockedUserBus.unblocked.collect { loadNotifications() }
        }
    }

    /** Drop a blocked artwork's like/share rows immediately; unblock re-fetches. */
    private fun observeBlocks() {
        viewModelScope.launch {
            blockedArtworkBus.events.collect { blockedArtworkId ->
                val tid = blockedArtworkId.toLong()
                _state.update { st ->
                    val filtered = st.notifications.filterNot {
                        (it.kind == NotificationKind.ARTWORK_LIKE || it.kind == NotificationKind.ARTWORK_SHARE) &&
                            it.targetId == tid
                    }
                    if (filtered.size == st.notifications.size) st else st.copy(notifications = filtered)
                }
                unreadStore.set(_state.value.notifications.count { !it.isRead })
            }
        }
        viewModelScope.launch {
            blockedArtworkBus.unblocked.collect { loadNotifications() }
        }
    }

    // ── Notifications ──────────────────────────────────────────────────────────────

    /** No delete endpoint exists (§8) — remove locally only. */
    fun onDeleteNotification(id: String) {
        _state.update { it.copy(notifications = it.notifications.filter { n -> n.id != id }) }
        unreadStore.set(_state.value.notifications.count { !it.isRead })
    }

    /** Optimistic local read, then PATCH /api/notifications/{id}/read (§8.1). */
    fun onMarkNotificationRead(id: String) {
        _state.update {
            it.copy(notifications = it.notifications.map { n ->
                if (n.id == id) n.copy(isRead = true) else n
            })
        }
        // Keep the bell badge in sync with the optimistic read.
        unreadStore.set(_state.value.notifications.count { !it.isRead })
        viewModelScope.launch { notificationsRepository.markRead(id) }
    }

    /**
     * Resolve a share/like notification's preview (owner + images) from its target id, lazily and
     * deduped. Peeks [DetailCache] first (warm), else fetches the curation/artwork detail. Read-only
     * for artworks (so we don't clobber cached ownership). Silent on failure — the row falls back to
     * the actor avatar + plain message.
     */
    fun loadPreview(item: NotificationItem) {
        val targetId = item.targetId ?: return
        if (_state.value.previews.containsKey(targetId)) return
        val idInt = targetId.toInt()
        viewModelScope.launch {
            val preview: SharedContentPreview? = when (item.kind) {
                NotificationKind.CURATION_SHARE, NotificationKind.CURATION_LIKE -> {
                    val cur = detailCache.peekCuration(idInt)?.curation
                        ?: (homeRepository.getCurationDetail(idInt) as? ApiResult.Success)?.data
                    cur?.let {
                        SharedContentPreview(
                            ownerId = it.authorId?.toLong(),
                            ownerName = it.curatorName,
                            title = it.title.ifBlank { item.targetTitle.orEmpty() }.ifBlank { null },
                            imageUrls = it.artworkUrls.take(3),
                        )
                    }
                }
                NotificationKind.ARTWORK_SHARE, NotificationKind.ARTWORK_LIKE -> {
                    val post = detailCache.peekArtwork(idInt)?.post
                        ?: (homeRepository.getArtworkDetail(idInt) as? ApiResult.Success)?.data
                    post?.let {
                        SharedContentPreview(
                            ownerId = it.ownerId?.toLong(),
                            ownerName = it.ownerName.ifBlank { null },
                            title = it.title.ifBlank { item.targetTitle.orEmpty() }.ifBlank { null },
                            imageUrls = listOfNotNull(item.thumbnailUrl ?: it.imageUrl.ifBlank { null }),
                        )
                    }
                }
                else -> null
            }
            if (preview != null) {
                _state.update { it.copy(previews = it.previews + (targetId to preview)) }
            }
        }
    }

    // ── Event popup ──────────────────────────────────────────────────────────────

    /** Open the popup (spinner first), fetch the event; on failure close + surface a toast. */
    fun onOpenEvent(eventId: Long) {
        _state.update { it.copy(isEventPopupOpen = true, isEventLoading = true, eventPopup = null, eventError = null) }
        viewModelScope.launch {
            when (val res = eventsRepository.getEvent(eventId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(isEventLoading = false, eventPopup = res.data)
                }
                is ApiResult.Error -> _state.update {
                    // Legacy 404 events → "Event not found"; everything else → its message.
                    it.copy(
                        isEventPopupOpen = false,
                        isEventLoading = false,
                        eventPopup = null,
                        eventError = if (res is ApiResult.Error.NotFound) "Event not found" else res.userMessage(),
                    )
                }
            }
        }
    }

    fun dismissEventPopup() = _state.update {
        it.copy(isEventPopupOpen = false, isEventLoading = false, eventPopup = null)
    }

    /** Clear the one-shot event error after the screen has shown it as a toast. */
    fun consumeEventError() = _state.update { it.copy(eventError = null) }

    /** Clear the one-shot pull-to-refresh error after the screen has shown it as a toast. */
    fun consumeRefreshError() = _state.update { it.copy(refreshError = null) }

    private companion object {
        /** Just past the refresh coordinator's ~5s failed-refresh window, so the auto-retry can refresh. */
        const val NOTIFICATIONS_AUTO_RETRY_DELAY_MS = 6_000L
    }
}
