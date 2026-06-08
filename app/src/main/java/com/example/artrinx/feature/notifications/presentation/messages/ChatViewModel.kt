package com.example.artrinx.feature.notifications.presentation.messages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.core.network.ChatEvent
import com.example.artrinx.core.network.ChatWebSocketManager
import com.example.artrinx.feature.notifications.domain.model.ChatGate
import com.example.artrinx.feature.notifications.domain.model.ChatMessage
import com.example.artrinx.feature.notifications.domain.model.SendStatus
import com.example.artrinx.feature.notifications.domain.repository.MessagesRepository
import com.example.artrinx.feature.profile.domain.repository.ProfileRepository
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val partnerName: String = "",
    val partnerRole: String = "Artist",
    val partnerAvatarUrl: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val gate: ChatGate = ChatGate.ACTIVE,
    val isLoading: Boolean = true,
    val error: Boolean = false,
) {
    val canSend: Boolean
        get() = gate == ChatGate.FRESH_INVITE || gate == ChatGate.INVITE_RECEIVED || gate == ChatGate.ACTIVE
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messagesRepository: MessagesRepository,
    private val profileRepository: ProfileRepository,
    private val webSocket: ChatWebSocketManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val partnerUserId: Int = savedStateHandle.get<String>("userId")?.toIntOrNull() ?: 0

    private var currentUserId: Int = 0
    private var chatroomId: String? = null

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val _toasts = Channel<String>(Channel.BUFFERED)
    val toasts = _toasts.receiveAsFlow()

    init {
        viewModelScope.launch {
            webSocket.events.collect { handleWsEvent(it) }
        }
    }

    fun loadConversation() {
        if (partnerUserId == 0) {
            _state.update { it.copy(isLoading = false, error = true) }
            return
        }
        _state.update { it.copy(isLoading = true, error = false) }
        viewModelScope.launch {
            val meResult = profileRepository.getMyProfile()
            val me = (meResult as? ApiResult.Success)?.data
            if (me == null) {
                _state.update { it.copy(isLoading = false, error = true) }
                return@launch
            }
            currentUserId = me.id

            val (profile, resolution, thread) = coroutineScope {
                val p = async { profileRepository.getPublicProfile(partnerUserId) }
                val r = async { messagesRepository.resolveChatroom(partnerUserId) }
                val t = async { messagesRepository.getThread(partnerUserId, currentUserId) }
                Triple(p.await(), r.await(), t.await())
            }

            val pub = (profile as? ApiResult.Success)?.data
            val res = (resolution as? ApiResult.Success)?.data
            chatroomId = res?.chatroomId ?: pub?.chatroomId

            val threadData = (thread as? ApiResult.Success)?.data
            if (threadData == null && pub == null) {
                _state.update { it.copy(isLoading = false, error = true) }
                return@launch
            }

            val messages = threadData?.messages.orEmpty().sortedBy { it.createdAtEpochMs }
            val iBlocked = threadData?.iBlocked ?: pub?.iBlocked ?: false
            val theyBlocked = threadData?.theyBlocked ?: pub?.theyBlocked ?: false

            _state.update {
                it.copy(
                    partnerName = pub?.displayName?.ifBlank { it.partnerName } ?: it.partnerName,
                    partnerRole = pub?.role?.ifBlank { "Artist" } ?: "Artist",
                    partnerAvatarUrl = pub?.avatarUrl,
                    messages = messages,
                    gate = deriveGate(messages, iBlocked, theyBlocked),
                    isLoading = false,
                    error = false,
                )
            }
            markIncomingRead(messages)
        }
    }

    fun onInputChange(text: String) = _state.update { it.copy(inputText = text) }

    fun onSend() {
        val text = _state.value.inputText.trim()
        if (text.isEmpty() || !_state.value.canSend || partnerUserId == 0) return

        val cid = UUID.randomUUID().toString()
        val optimistic = ChatMessage(
            id = cid,
            content = text,
            isSent = true,
            timestamp = "",
            clientMessageId = cid,
            sendStatus = SendStatus.SENDING,
            createdAtEpochMs = System.currentTimeMillis(),
        )
        _state.update {
            val msgs = it.messages + optimistic
            it.copy(messages = msgs, inputText = "", gate = deriveGate(msgs, iBlocked = false, theyBlocked = false))
        }
        sendInternal(cid, text)
    }

    fun retryMessage(message: ChatMessage) {
        val cid = message.clientMessageId ?: return
        _state.update {
            it.copy(messages = it.messages.map { m -> if (m.clientMessageId == cid) m.copy(sendStatus = SendStatus.SENDING) else m })
        }
        sendInternal(cid, message.content)
    }

    private fun sendInternal(cid: String, text: String) {
        viewModelScope.launch {
            when (val res = messagesRepository.sendMessage(partnerUserId, currentUserId, text, clientMessageId = cid)) {
                is ApiResult.Success -> {
                    chatroomId = res.data.chatroomId ?: chatroomId
                    _state.update { st ->
                        val replaced = st.messages.map {
                            if (it.clientMessageId == cid) res.data.message.copy(clientMessageId = cid) else it
                        }
                        st.copy(messages = replaced, gate = deriveGate(replaced, res.data.iBlocked, res.data.theyBlocked))
                    }
                }
                is ApiResult.Error.Blocked -> {
                    // 403: waiting-for-accept / block / invite-limit. Drop the optimistic bubble and
                    // reflect the "invitation pending" gate (input disabled). Never sign out.
                    _state.update { st ->
                        val msgs = st.messages.filterNot { it.clientMessageId == cid }
                        st.copy(messages = msgs, gate = ChatGate.INVITE_SENT_WAITING)
                    }
                    _toasts.trySend("Your invitation is pending — you can message again once they respond.")
                }
                else -> {
                    _state.update { st ->
                        st.copy(messages = st.messages.map {
                            if (it.clientMessageId == cid) it.copy(sendStatus = SendStatus.FAILED) else it
                        })
                    }
                    _toasts.trySend("Couldn't send message. Tap the message to retry.")
                }
            }
        }
    }

    fun onEditMessage(message: ChatMessage, newText: String) {
        val trimmed = newText.trim()
        if (trimmed.isEmpty() || trimmed == message.content) return
        viewModelScope.launch {
            when (val res = messagesRepository.editMessage(message.id, currentUserId, trimmed)) {
                is ApiResult.Success -> _state.update { st ->
                    st.copy(messages = st.messages.map { if (it.id == message.id) res.data else it })
                }
                is ApiResult.Error.Validation -> _toasts.trySend(res.message)
                else -> _toasts.trySend("Couldn't edit message.")
            }
        }
    }

    fun onDeleteMessage(message: ChatMessage) {
        viewModelScope.launch {
            when (messagesRepository.deleteMessage(message.id)) {
                is ApiResult.Success -> _state.update { st ->
                    st.copy(messages = st.messages.map { if (it.id == message.id) it.copy(isDeleted = true) else it })
                }
                else -> _toasts.trySend("Couldn't delete message.")
            }
        }
    }

    // ── WebSocket events ──────────────────────────────────────────────────────────

    private fun handleWsEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.NewMessage -> onIncomingMessage(event.data)
            is ChatEvent.Edit -> if (hasMessage(event.messageId)) _state.update { st ->
                st.copy(messages = st.messages.map {
                    if (it.id == event.messageId) it.copy(content = event.text ?: it.content, isEdited = true) else it
                })
            }
            is ChatEvent.Delete -> if (hasMessage(event.messageId)) _state.update { st ->
                st.copy(messages = st.messages.map {
                    if (it.id == event.messageId) it.copy(isDeleted = true) else it
                })
            }
            is ChatEvent.Read -> _state.update { st ->
                // A message I sent was read — mark it (and everything older I sent) read.
                st.copy(messages = st.messages.map {
                    if (it.id == event.messageId && it.isSent) it.copy(isRead = true) else it
                })
            }
            is ChatEvent.IncomingNotification -> Unit // handled elsewhere
        }
    }

    private fun onIncomingMessage(data: JsonObject) {
        if (currentUserId == 0) return
        // The socket fans out in snake_case, so accept both casings here.
        val cr = data.stringOrNull("chatroomId", "chatroom_id")
        val senderId = data.intOrNull("senderId", "sender_id")
        val receiverId = data.intOrNull("receiverId", "receiver_id")
        val relevant = (chatroomId != null && cr == chatroomId) ||
            senderId == partnerUserId || receiverId == partnerUserId
        if (!relevant) return

        val msg = messagesRepository.parseIncomingMessage(data, currentUserId) ?: return
        if (cr != null) chatroomId = cr

        _state.update { st ->
            // Dedup: reconcile our own optimistic/echoed message, skip duplicates by id.
            val byClient = msg.clientMessageId
            if (byClient != null && st.messages.any { it.clientMessageId == byClient }) {
                val replaced = st.messages.map { if (it.clientMessageId == byClient) msg.copy(clientMessageId = byClient) else it }
                return@update st.copy(messages = replaced, gate = deriveGate(replaced, false, false))
            }
            if (st.messages.any { it.id == msg.id }) return@update st
            val msgs = (st.messages + msg).sortedBy { it.createdAtEpochMs }
            st.copy(messages = msgs, gate = deriveGate(msgs, false, false))
        }
        if (!msg.isSent) markReadFireAndForget(msg.id)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private fun hasMessage(id: String): Boolean = _state.value.messages.any { it.id == id }

    private fun markIncomingRead(messages: List<ChatMessage>) {
        messages.filter { !it.isSent && !it.isRead && !it.isDeleted }
            .forEach { markReadFireAndForget(it.id) }
    }

    private fun markReadFireAndForget(messageId: String) {
        viewModelScope.launch { messagesRepository.markRead(messageId) }
    }

    /**
     * Authorship-based gate (robust to the ambiguous `invitationStatus` boolean):
     * both sent → ACTIVE; only they sent → I'm the invited party; only I sent → waiting; none → fresh.
     */
    private fun deriveGate(messages: List<ChatMessage>, iBlocked: Boolean, theyBlocked: Boolean): ChatGate {
        if (iBlocked || theyBlocked) return ChatGate.BLOCKED
        val real = messages.filter { it.sendStatus != SendStatus.FAILED }
        if (real.isEmpty()) return ChatGate.FRESH_INVITE
        val theyHaveSent = real.any { !it.isSent }
        val iHaveSent = real.any { it.isSent }
        return when {
            theyHaveSent && iHaveSent -> ChatGate.ACTIVE
            theyHaveSent -> ChatGate.INVITE_RECEIVED
            else -> ChatGate.INVITE_SENT_WAITING
        }
    }

    private fun JsonObject.stringOrNull(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { get(it)?.takeIf { el -> !el.isJsonNull }?.asString }

    private fun JsonObject.intOrNull(vararg keys: String): Int? =
        keys.firstNotNullOfOrNull { get(it)?.takeIf { el -> !el.isJsonNull }?.asInt }
}
