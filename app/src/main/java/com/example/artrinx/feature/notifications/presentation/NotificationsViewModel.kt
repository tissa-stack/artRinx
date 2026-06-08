package com.example.artrinx.feature.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.core.network.ChatEvent
import com.example.artrinx.core.network.ChatWebSocketManager
import com.example.artrinx.feature.notifications.domain.model.ConversationItem
import com.example.artrinx.feature.notifications.domain.model.MockNotificationData
import com.example.artrinx.feature.notifications.domain.model.NotifTab
import com.example.artrinx.feature.notifications.domain.model.NotificationItem
import com.example.artrinx.feature.notifications.domain.repository.MessagesRepository
import com.example.artrinx.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val activeTab: NotifTab = NotifTab.NOTIFICATIONS,
    val notifications: List<NotificationItem> = MockNotificationData.notifications,
    val conversations: List<ConversationItem> = emptyList(),
    val messageQuery: String = "",
    val invitationCount: Int = 0,
    val isLoadingConversations: Boolean = true,
    val isRefreshing: Boolean = false,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val messagesRepository: MessagesRepository,
    private val profileRepository: ProfileRepository,
    private val webSocket: ChatWebSocketManager,
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        refreshConversations()
        loadInvitationCount()
        viewModelScope.launch {
            webSocket.events.collect { event ->
                // Inbox previews / unread badges change on any new or read message — refresh.
                if (event is ChatEvent.NewMessage || event is ChatEvent.Read) refreshConversations()
            }
        }
    }

    fun onTabSelected(tab: NotifTab) {
        _state.update { it.copy(activeTab = tab) }
        if (tab == NotifTab.MESSAGES) refreshConversations()
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

    private fun loadInvitationCount() {
        viewModelScope.launch {
            val info = profileRepository.getInviteInfo()
            if (info is ApiResult.Success) {
                _state.update { it.copy(invitationCount = info.data.remainingInvites ?: 0) }
            }
        }
    }

    /** Delete a conversation from the current user's side (§7.10). [item.id] is the other user's id. */
    fun onDeleteConversation(item: ConversationItem) {
        val userId = item.id.toIntOrNull() ?: return
        _state.update { it.copy(conversations = it.conversations.filterNot { c -> c.id == item.id }) }
        viewModelScope.launch { messagesRepository.deleteChat(userId) }
    }

    /** No bulk "mark chat read" endpoint — clear the unread badge locally (optimistic). */
    fun onMarkConversationRead(item: ConversationItem) {
        _state.update {
            it.copy(conversations = it.conversations.map { c ->
                if (c.id == item.id) c.copy(isUnread = false, unreadCount = 0) else c
            })
        }
    }

    // ── Notifications (still mock — out of scope for the chat task) ────────────────

    fun onDeleteNotification(id: String) = _state.update {
        it.copy(notifications = it.notifications.filter { n -> n.id != id })
    }

    fun onMarkNotificationRead(id: String) = _state.update {
        it.copy(notifications = it.notifications.map { n ->
            if (n.id == id) n.copy(isRead = true) else n
        })
    }
}
