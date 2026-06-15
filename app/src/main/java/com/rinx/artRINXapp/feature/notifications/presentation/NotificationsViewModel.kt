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
import com.rinx.artRINXapp.feature.notifications.domain.UnreadNotificationsStore
import com.rinx.artRINXapp.feature.notifications.domain.model.ConversationItem
import com.rinx.artRINXapp.feature.notifications.domain.model.NotifTab
import com.rinx.artRINXapp.feature.notifications.domain.model.NotificationItem
import com.rinx.artRINXapp.feature.notifications.domain.repository.MessagesRepository
import com.rinx.artRINXapp.feature.notifications.domain.repository.NotificationsRepository
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val activeTab: NotifTab = NotifTab.NOTIFICATIONS,
    val notifications: List<NotificationItem> = emptyList(),
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

    init {
        loadNotifications()
        refreshConversations()
        loadInvitationCount()
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
                    _state.update {
                        it.copy(notifications = res.data, isLoadingNotifications = false, notificationsError = null)
                    }
                    unreadStore.set(res.data.count { !it.isRead })
                }
                is ApiResult.Error -> _state.update {
                    it.copy(isLoadingNotifications = false, notificationsError = res.userMessage())
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
                is ApiResult.Error -> _state.update { it.copy(isRefreshingNotifications = false) }
            }
        }
    }

    /** Re-show the loading state, then re-fetch — wired to the error view's Retry button. */
    fun retryNotifications() {
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
}
