package com.rinx.artRINXapp.feature.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.network.ChatEvent
import com.rinx.artRINXapp.core.network.ChatWebSocketManager
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
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val messagesRepository: MessagesRepository,
    private val notificationsRepository: NotificationsRepository,
    private val profileRepository: ProfileRepository,
    private val webSocket: ChatWebSocketManager,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

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
                is ApiResult.Success -> _state.update {
                    it.copy(notifications = res.data, isLoadingNotifications = false)
                }
                else -> _state.update { it.copy(isLoadingNotifications = false) }
            }
        }
    }

    fun onMessageQueryChange(q: String) = _state.update { it.copy(messageQuery = q) }

    /** [isUserRefresh] = true when triggered by pull-to-refresh, so the drag spinner is shown. */
    fun refreshConversations(isUserRefresh: Boolean = false) {
        if (isUserRefresh) _state.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            when (val res = messagesRepository.getChatrooms()) {
                is ApiResult.Success -> _state.update {
                    it.copy(conversations = res.data, isLoadingConversations = false, isRefreshing = false)
                }
                else -> _state.update { it.copy(isLoadingConversations = false, isRefreshing = false) }
            }
        }
    }

    fun loadInvitationCount() {
        viewModelScope.launch {
            val info = profileRepository.getInviteInfo()
            if (info is ApiResult.Success) {
                _state.update { it.copy(invitationCount = info.data.remainingInvites ?: 0) }
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

    /** No bulk "mark chat read" endpoint — clear the unread badge locally (optimistic). */
    fun onMarkConversationRead(item: ConversationItem) {
        _state.update {
            it.copy(conversations = it.conversations.map { c ->
                if (c.id == item.id) c.copy(isUnread = false, unreadCount = 0) else c
            })
        }
    }

    // ── Notifications ──────────────────────────────────────────────────────────────

    /** No delete endpoint exists (§8) — remove locally only. */
    fun onDeleteNotification(id: String) = _state.update {
        it.copy(notifications = it.notifications.filter { n -> n.id != id })
    }

    /** Optimistic local read, then PATCH /api/notifications/{id}/read (§8.1). */
    fun onMarkNotificationRead(id: String) {
        _state.update {
            it.copy(notifications = it.notifications.map { n ->
                if (n.id == id) n.copy(isRead = true) else n
            })
        }
        viewModelScope.launch { notificationsRepository.markRead(id) }
    }
}
