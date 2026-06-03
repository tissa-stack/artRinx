package com.example.artrinx.feature.notifications.presentation.messages

import androidx.lifecycle.ViewModel
import com.example.artrinx.feature.notifications.domain.model.ChatMessage
import com.example.artrinx.feature.notifications.domain.model.ConversationItem
import com.example.artrinx.feature.notifications.domain.model.ConversationState
import com.example.artrinx.feature.notifications.domain.model.MockNotificationData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ChatUiState(
    val conversation: ConversationItem? = null,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val showReportSheet: Boolean = false,
    val showBlockedDialog: Boolean = false,
)

@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun loadConversation(userId: String) {
        // Try existing conversations first; fall back to contacts (from NewMessageScreen flow)
        val conv = MockNotificationData.conversations.find { it.id == userId }
            ?: MockNotificationData.contacts.find { it.id == userId }?.let { contact ->
                ConversationItem(
                    id          = userId,
                    userName    = contact.name,
                    userHandle  = contact.handle,
                    avatarRes   = contact.avatarRes,
                    lastMessage = "",
                    timestamp   = "",
                    state       = ConversationState.ACTIVE,
                )
            }
        val msgs = MockNotificationData.chatMessages[userId] ?: emptyList()
        _state.update { it.copy(conversation = conv, messages = msgs) }
    }

    fun onInputChange(text: String) = _state.update { it.copy(inputText = text) }

    fun onSend() {
        val text = _state.value.inputText.trim()
        if (text.isEmpty()) return
        val newMsg = ChatMessage(
            id        = "sent_${System.nanoTime()}",
            content   = text,
            isSent    = true,
            timestamp = "Just now",
        )
        // Also mark invitation as accepted if conversation was pending
        val conv = _state.value.conversation?.let {
            if (it.state == ConversationState.INVITATION_PENDING)
                it.copy(state = ConversationState.ACTIVE)
            else it
        }
        _state.update {
            it.copy(
                messages  = it.messages + newMsg,
                inputText = "",
                conversation = conv,
            )
        }
    }

    fun onShowReport()   = _state.update { it.copy(showReportSheet = true) }
    fun onDismissReport() = _state.update { it.copy(showReportSheet = false) }
    fun onShowBlocked()  = _state.update { it.copy(showBlockedDialog = true) }
    fun onDismissBlocked() = _state.update { it.copy(showBlockedDialog = false) }
}
