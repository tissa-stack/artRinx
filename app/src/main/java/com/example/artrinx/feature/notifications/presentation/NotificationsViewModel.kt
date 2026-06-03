package com.example.artrinx.feature.notifications.presentation

import androidx.lifecycle.ViewModel
import com.example.artrinx.feature.notifications.domain.model.ConversationItem
import com.example.artrinx.feature.notifications.domain.model.MockNotificationData
import com.example.artrinx.feature.notifications.domain.model.NotifTab
import com.example.artrinx.feature.notifications.domain.model.NotificationItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class NotificationsUiState(
    val activeTab: NotifTab = NotifTab.NOTIFICATIONS,
    val notifications: List<NotificationItem> = MockNotificationData.notifications,
    val conversations: List<ConversationItem> = MockNotificationData.conversations,
    val messageQuery: String = "",
    val invitationCount: Int = 2,
)

@HiltViewModel
class NotificationsViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    fun onTabSelected(tab: NotifTab) = _state.update { it.copy(activeTab = tab) }

    fun onMessageQueryChange(q: String) = _state.update { it.copy(messageQuery = q) }

    fun onDeleteNotification(id: String) = _state.update {
        it.copy(notifications = it.notifications.filter { n -> n.id != id })
    }

    fun onMarkNotificationRead(id: String) = _state.update {
        it.copy(notifications = it.notifications.map { n ->
            if (n.id == id) n.copy(isRead = true) else n
        })
    }
}
