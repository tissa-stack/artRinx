package com.rinx.artRINXapp.feature.notifications.presentation.messages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.network.ChatEvent
import com.rinx.artRINXapp.core.network.ChatWebSocketManager
import com.rinx.artRINXapp.core.network.WsConnectionState
import com.rinx.artRINXapp.feature.notifications.domain.model.ChatGate
import com.rinx.artRINXapp.feature.notifications.domain.model.ChatMessage
import com.rinx.artRINXapp.feature.notifications.domain.model.SendStatus
import com.rinx.artRINXapp.feature.notifications.domain.repository.MessagesRepository
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val partnerName: String = "",
    val partnerRole: String = "Artist",
    val partnerAvatarUrl: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val gate: ChatGate = ChatGate.ACTIVE,
    /** profile.remaining_chat_invites — drives the "You have N new chats this month" footnote. */
    val remainingInvites: Int? = null,
    val isLoading: Boolean = true,
    val error: Boolean = false,
    /** Pagination: more older messages exist + a load is in flight. */
    val canLoadEarlier: Boolean = false,
    val isLoadingEarlier: Boolean = false,
    /** Non-null while inline-editing one of my own messages (drives the X / ✓ compose UI). */
    val editingMessageId: String? = null,
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
    private var nextCursor: String? = null

    // Server-driven gate inputs (handout 5-state tree), updated from thread / resolution / send.
    private var invitationStatus: Boolean = false
    private var isActive: Boolean = true
    private var iBlocked: Boolean = false
    private var theyBlocked: Boolean = false

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val _toasts = Channel<String>(Channel.BUFFERED)
    val toasts = _toasts.receiveAsFlow()

    private var hasLoadedOnce = false
    private var lastConnState = WsConnectionState.DISCONNECTED
    /** Message ids already marked read this session — avoids re-POSTing on every viewport change. */
    private val markedRead = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            webSocket.events.collect { handleWsEvent(it) }
        }
        // Reconnect backfill (§12): the server doesn't replay events across an offline window, so
        // each time the socket transitions back to CONNECTED we refetch and merge-by-id.
        viewModelScope.launch {
            webSocket.connectionState.collect { st ->
                if (st == WsConnectionState.CONNECTED && lastConnState != WsConnectionState.CONNECTED && hasLoadedOnce) {
                    backfill()
                }
                lastConnState = st
            }
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
            // Server fields drive the gate (handout tree). Prefer the thread, fall back to the
            // chatroom-resolution / public-profile payloads, then to safe defaults.
            iBlocked = threadData?.iBlocked ?: res?.iBlocked ?: pub?.iBlocked ?: false
            theyBlocked = threadData?.theyBlocked ?: res?.theyBlocked ?: pub?.theyBlocked ?: false
            invitationStatus = threadData?.invitationStatus ?: res?.invitationStatus ?: false
            isActive = threadData?.isActive ?: res?.isActive ?: true
            val remaining = res?.remainingInvites ?: threadData?.remainingInvites
            nextCursor = threadData?.nextCursor

            _state.update {
                it.copy(
                    partnerName = pub?.displayName?.ifBlank { it.partnerName } ?: it.partnerName,
                    partnerRole = pub?.role?.ifBlank { "Artist" } ?: "Artist",
                    partnerAvatarUrl = pub?.avatarUrl,
                    messages = messages,
                    gate = deriveGate(messages),
                    remainingInvites = remaining ?: it.remainingInvites,
                    isLoading = false,
                    error = false,
                    canLoadEarlier = nextCursor != null,
                )
            }
            hasLoadedOnce = true
        }
    }

    /**
     * Mark received-unread messages read as they enter the viewport (handout §on view appear).
     * Deduped via [markedRead]; the per-message [markRead] call is fire-and-forget.
     */
    fun onMessagesVisible(visibleIds: Set<String>) {
        if (visibleIds.isEmpty()) return
        val toMark = _state.value.messages.filter {
            it.id in visibleIds && !it.isSent && !it.isRead && !it.isDeleted && it.id !in markedRead
        }
        toMark.forEach { markedRead.add(it.id); markReadFireAndForget(it.id) }
    }

    /** Refetch the thread and merge-by-id without disturbing in-flight optimistic messages. */
    private fun backfill() {
        if (partnerUserId == 0 || currentUserId == 0) return
        viewModelScope.launch {
            val res = messagesRepository.getThread(partnerUserId, currentUserId)
            if (res !is ApiResult.Success) return@launch
            val server = res.data
            invitationStatus = server.invitationStatus
            isActive = server.isActive
            iBlocked = server.iBlocked
            theyBlocked = server.theyBlocked
            if (server.nextCursor != null) nextCursor = server.nextCursor
            _state.update { st ->
                val merged = mergeById(st.messages, server.messages)
                st.copy(
                    messages = merged,
                    gate = deriveGate(merged),
                    remainingInvites = server.remainingInvites ?: st.remainingInvites,
                    canLoadEarlier = nextCursor != null,
                )
            }
            // Visible messages get marked read by onMessagesVisible after the list recomposes.
        }
    }

    /**
     * Merge server messages into the local list: update mutable fields on matches, insert new ones,
     * and preserve local-only optimistic messages (temp ids not yet known to the server).
     */
    private fun mergeById(local: List<ChatMessage>, server: List<ChatMessage>): List<ChatMessage> {
        val byId = local.associateBy { it.id }.toMutableMap()
        for (s in server) {
            val existing = byId[s.id]
            byId[s.id] = existing?.copy(
                content = s.content,
                isEdited = s.isEdited,
                isDeleted = s.isDeleted,
                isRead = s.isRead,
                editedAtEpochMs = s.editedAtEpochMs,
            ) ?: s
        }
        return byId.values.sortedBy { it.createdAtEpochMs }
    }

    /** Scroll-to-top pagination: fetch the next older page and prepend (dedup by id). */
    fun loadEarlier() {
        val st = _state.value
        if (!st.canLoadEarlier || st.isLoadingEarlier || st.isLoading) return
        val before = st.messages.minByOrNull { it.createdAtEpochMs }?.createdAtIso?.takeIf { it.isNotBlank() }
            ?: nextCursor ?: return
        _state.update { it.copy(isLoadingEarlier = true) }
        viewModelScope.launch {
            val res = messagesRepository.getThread(partnerUserId, currentUserId, before = before)
            if (res !is ApiResult.Success) {
                _state.update { it.copy(isLoadingEarlier = false) }
                return@launch
            }
            nextCursor = res.data.nextCursor
            _state.update { stt ->
                val existing = stt.messages.associateBy { it.id }
                val older = res.data.messages.filter { it.id !in existing }
                val merged = (older + stt.messages).sortedBy { it.createdAtEpochMs }
                stt.copy(
                    messages = merged,
                    isLoadingEarlier = false,
                    canLoadEarlier = nextCursor != null,
                )
            }
        }
    }

    fun onInputChange(text: String) =
        // Strip URLs in real time (the backend rejects them); other text + whitespace pass through.
        _state.update { it.copy(inputText = text.replace(URL_REGEX, "")) }

    /** Enter inline-edit mode for one of my own messages (pre-fills the compose field). */
    fun beginEdit(message: ChatMessage) {
        _state.update { it.copy(editingMessageId = message.id, inputText = message.content) }
    }

    /** Discard an in-progress edit and clear the field. */
    fun cancelEdit() {
        _state.update { it.copy(editingMessageId = null, inputText = "") }
    }

    fun onSend() {
        val editingId = _state.value.editingMessageId
        if (editingId != null) {
            val text = _state.value.inputText.trim()
            val target = _state.value.messages.firstOrNull { it.id == editingId }
            if (target != null && text.isNotEmpty()) onEditMessage(target, text)
            cancelEdit()
            return
        }
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
            it.copy(messages = msgs, inputText = "", gate = deriveGate(msgs))
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
                    // Adopt the authoritative gate inputs from the send response.
                    invitationStatus = res.data.invitationStatus
                    isActive = res.data.isActive
                    iBlocked = res.data.iBlocked
                    theyBlocked = res.data.theyBlocked
                    _state.update { st ->
                        val replaced = st.messages.map {
                            if (it.clientMessageId == cid) res.data.message.copy(clientMessageId = cid) else it
                        }
                        st.copy(messages = replaced, gate = deriveGate(replaced))
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
            is ChatEvent.Edit -> if (hasMessage(event.messageId)) {
                val incomingEditedAt = parseIsoMs(event.editedAt)
                _state.update { st ->
                    st.copy(messages = st.messages.map {
                        when {
                            it.id != event.messageId -> it
                            // Monotonic guard: ignore an edit that isn't newer than what we have.
                            incomingEditedAt != 0L && it.editedAtEpochMs != 0L &&
                                incomingEditedAt <= it.editedAtEpochMs -> it
                            else -> it.copy(
                                content = event.text ?: it.content,
                                isEdited = true,
                                editedAtEpochMs = if (incomingEditedAt != 0L) incomingEditedAt else it.editedAtEpochMs,
                            )
                        }
                    })
                }
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

        // The socket payload doesn't carry invitation_status/is_active. A received message means
        // the partner is participating; if I've also sent one, the invite is accepted and the chat
        // is active. Reconcile the gate inputs so the field unlocks without waiting for a refetch.
        if (!msg.isSent && _state.value.messages.any { it.isSent && it.sendStatus != SendStatus.FAILED }) {
            invitationStatus = true
            isActive = true
        }
        _state.update { st ->
            // Dedup: reconcile our own optimistic/echoed message, skip duplicates by id.
            val byClient = msg.clientMessageId
            if (byClient != null && st.messages.any { it.clientMessageId == byClient }) {
                val replaced = st.messages.map { if (it.clientMessageId == byClient) msg.copy(clientMessageId = byClient) else it }
                return@update st.copy(messages = replaced, gate = deriveGate(replaced))
            }
            if (st.messages.any { it.id == msg.id }) return@update st
            val msgs = (st.messages + msg).sortedBy { it.createdAtEpochMs }
            st.copy(messages = msgs, gate = deriveGate(msgs))
        }
        if (!msg.isSent) markReadFireAndForget(msg.id)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private fun hasMessage(id: String): Boolean = _state.value.messages.any { it.id == id }

    private fun markReadFireAndForget(messageId: String) {
        viewModelScope.launch { messagesRepository.markRead(messageId) }
    }

    /**
     * Server-field gate — the handout's 5-state compose decision tree. Reads the member flags
     * ([iBlocked]/[theyBlocked]/[invitationStatus]/[isActive]) + [chatroomId] + the message list.
     * Priority order matches the spec exactly.
     */
    private fun deriveGate(messages: List<ChatMessage>): ChatGate {
        val real = messages.filter { it.sendStatus != SendStatus.FAILED }
        return when {
            iBlocked -> ChatGate.BLOCKED_BY_ME
            theyBlocked -> ChatGate.BLOCKED_BY_THEM
            // No chat yet — this first message is the invitation.
            real.isEmpty() && chatroomId.isNullOrEmpty() -> ChatGate.FRESH_INVITE
            // They invited me; I haven't replied yet → replying accepts.
            invitationStatus && !isActive && real.isNotEmpty() -> ChatGate.INVITE_RECEIVED
            // I invited them; awaiting their response → field disabled.
            !invitationStatus && !isActive && real.isNotEmpty() -> ChatGate.INVITE_SENT_WAITING
            // Normal active chat (invitationStatus && isActive), or any residual state.
            else -> ChatGate.ACTIVE
        }
    }

    /** Parse an ISO-8601 timestamp to epoch ms; 0L on null/parse failure. */
    private fun parseIsoMs(iso: String?): Long {
        if (iso.isNullOrBlank()) return 0L
        for (pattern in ISO_PATTERNS) {
            try {
                val fmt = SimpleDateFormat(pattern, Locale.US)
                fmt.timeZone = TimeZone.getTimeZone("UTC")
                return fmt.parse(iso)?.time ?: continue
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return 0L
    }

    private fun JsonObject.stringOrNull(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { get(it)?.takeIf { el -> !el.isJsonNull }?.asString }

    private fun JsonObject.intOrNull(vararg keys: String): Int? =
        keys.firstNotNullOfOrNull { get(it)?.takeIf { el -> !el.isJsonNull }?.asInt }

    private companion object {
        /** http://, https://, and www. URLs — stripped from compose input (handout link-stripping). */
        val URL_REGEX = Regex("""(?i)\b(?:https?://|www\.)\S+""")
        val ISO_PATTERNS = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
        )
    }
}
