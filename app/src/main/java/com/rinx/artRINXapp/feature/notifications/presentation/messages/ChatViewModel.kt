package com.rinx.artRINXapp.feature.notifications.presentation.messages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.network.ChatEvent
import com.rinx.artRINXapp.core.network.ChatWebSocketManager
import com.rinx.artRINXapp.core.network.WsConnectionState
import com.rinx.artRINXapp.feature.notifications.domain.OutgoingMessageStore
import com.rinx.artRINXapp.feature.notifications.domain.SendOutcome
import com.rinx.artRINXapp.feature.notifications.domain.model.ChatGate
import com.rinx.artRINXapp.feature.notifications.domain.model.ChatMessage
import com.rinx.artRINXapp.feature.notifications.domain.model.SendStatus
import com.rinx.artRINXapp.feature.notifications.domain.repository.MessagesRepository
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    /**
     * True once the server has confirmed the relationship for this screen-open. A cache-seeded reopen
     * starts false: the cached gate may be a stale invite/waiting state for a chat that's since become
     * active, so the invite banner is held back until the silent revalidation confirms the real gate.
     */
    val gateConfirmed: Boolean = false,
    /** profile.remaining_chat_invites — drives the "You have N new chats this month" footnote. */
    val remainingInvites: Int? = null,
    val isLoading: Boolean = true,
    /** Pull-to-refresh in flight — a lightweight indicator, not the full-screen shimmer. */
    val isRefreshing: Boolean = false,
    val error: Boolean = false,
    /** Pagination: more older messages exist + a load is in flight. */
    val canLoadEarlier: Boolean = false,
    val isLoadingEarlier: Boolean = false,
    /** Non-null while inline-editing one of my own messages (drives the X / ✓ compose UI). */
    val editingMessageId: String? = null,
    /** True while the other participant is typing (drives the "typing…" bubble). */
    val partnerIsTyping: Boolean = false,
) {
    val canSend: Boolean
        get() = when (gate) {
            // A fresh invite consumes a new-chat (message-request) quota slot; block it at the cap.
            ChatGate.FRESH_INVITE -> (remainingInvites ?: Int.MAX_VALUE) > 0
            ChatGate.INVITE_RECEIVED, ChatGate.ACTIVE -> true
            else -> false
        }

    /** A new chat can't be started because the monthly new-chat quota is exhausted. */
    val isNewChatCapReached: Boolean
        get() = gate == ChatGate.FRESH_INVITE && (remainingInvites ?: Int.MAX_VALUE) <= 0
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messagesRepository: MessagesRepository,
    private val profileRepository: ProfileRepository,
    private val webSocket: ChatWebSocketManager,
    private val chatCache: com.rinx.artRINXapp.feature.notifications.data.local.ChatCache,
    private val blockedUsersStore: com.rinx.artRINXapp.core.util.BlockedUsersStore,
    private val outgoingStore: OutgoingMessageStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val partnerUserId: Int = savedStateHandle.get<String>("userId")?.toIntOrNull() ?: 0

    private var currentUserId: Int = 0
    private var chatroomId: String? = null
    private var nextCursor: String? = null

    // Typing indicator: inbound clear-after-idle job, outbound stop-after-idle job + throttle clock.
    private var typingClearJob: Job? = null
    private var typingStopJob: Job? = null
    private var outboundTypingActive = false
    private var lastTypingPingMs = 0L

    /**
     * Server/WS-confirmed messages (the durable list). The displayed list = [confirmed] merged with
     * the app-scoped [outgoingStore]'s unconfirmed (SENDING/FAILED) bubbles for this partner, so an
     * in-flight or failed send survives leaving and returning to the screen. See [displayMessages].
     */
    private var confirmed: List<ChatMessage> = emptyList()

    // Server-driven gate inputs (handout 5-state tree), updated from thread / resolution / send.
    private var invitationStatus: Boolean = false
    private var isActive: Boolean = true
    private var iBlocked: Boolean = false
    private var theyBlocked: Boolean = false
    private var canMessage: Boolean? = null
    private var blockReason: String? = null

    // Seed synchronously from cache so reopening a chat renders instantly with no shimmer (SWR).
    private val _state = MutableStateFlow(seedFromCache())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private fun seedFromCache(): ChatUiState {
        val snap = chatCache.get(partnerUserId) ?: return ChatUiState(isLoading = true)
        // Restore the gate inputs so deriveGate + sending behave correctly before the silent refresh.
        invitationStatus = snap.invitationStatus
        isActive = snap.isActive
        // If I've blocked this user, that always wins (even over a stale cached gate / failed endpoints).
        iBlocked = snap.iBlocked || blockedUsersStore.isBlocked(partnerUserId)
        theyBlocked = snap.theyBlocked
        blockReason = snap.blockReason
        canMessage = snap.canMessage
        chatroomId = snap.chatroomId
        nextCursor = snap.nextCursor
        // Split the cached display list: confirmed (server) messages vs optimistic (SENDING/FAILED)
        // bubbles. Re-own the unconfirmed ones in the app-scoped outbox as retryable FAILED, unless an
        // in-flight send for the same cid is already tracked there (an in-session reopen).
        confirmed = snap.messages.filter { it.sendStatus == SendStatus.SENT }
        outgoingStore.restoreAsFailed(partnerUserId, snap.messages.filter { it.sendStatus != SendStatus.SENT })
        val msgs = displayMessages()
        return ChatUiState(
            partnerName = snap.partnerName,
            partnerRole = snap.partnerRole,
            partnerAvatarUrl = snap.partnerAvatarUrl,
            messages = msgs,
            gate = deriveGate(msgs),
            remainingInvites = snap.remainingInvites,
            isLoading = false,
            canLoadEarlier = snap.nextCursor != null,
        )
    }

    /** The displayed list: [confirmed] server messages merged with the outbox's unconfirmed bubbles
     *  for this partner, deduped by client/server id (a confirmed message always wins its echo). */
    private fun displayMessages(): List<ChatMessage> {
        val pending = outgoingStore.pendingFor(partnerUserId)
        if (pending.isEmpty()) return confirmed.sortedBy { it.createdAtEpochMs }
        val confirmedCids = confirmed.mapNotNull { it.clientMessageId }.toSet()
        val confirmedIds = confirmed.map { it.id }.toSet()
        val extras = pending.filter { it.clientMessageId !in confirmedCids && it.id !in confirmedIds }
        return (confirmed + extras).sortedBy { it.createdAtEpochMs }
    }

    /** Recompute the merged list + gate from [confirmed] + the outbox, and write through to cache. */
    private fun publishMessages() {
        val msgs = displayMessages()
        _state.update { it.copy(messages = msgs, gate = deriveGate(msgs)) }
        cacheSnapshot(_state.value)
    }

    private fun cacheSnapshot(st: ChatUiState) {
        if (partnerUserId == 0) return
        chatCache.put(
            partnerUserId,
            com.rinx.artRINXapp.feature.notifications.data.local.ChatSnapshot(
                // Don't persist in-flight (SENDING) bubbles: after process death the dispatch coroutine
                // is gone, so a reopen would wrongly resurrect them as FAILED. A send that actually
                // landed reappears via the server thread fetch; FAILED bubbles are still cached (retryable).
                messages = st.messages.filter { it.sendStatus != SendStatus.SENDING },
                partnerName = st.partnerName,
                partnerRole = st.partnerRole,
                partnerAvatarUrl = st.partnerAvatarUrl,
                gate = st.gate,
                remainingInvites = st.remainingInvites,
                invitationStatus = invitationStatus,
                isActive = isActive,
                iBlocked = iBlocked,
                theyBlocked = theyBlocked,
                blockReason = blockReason,
                canMessage = canMessage,
                chatroomId = chatroomId,
                nextCursor = nextCursor,
            ),
        )
    }

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
        // The outbox owns in-flight/failed sends (app-scoped → survives leaving the screen).
        // Re-render whenever this partner's pending bubbles change, and react to send outcomes.
        viewModelScope.launch {
            outgoingStore.outgoing
                .map { it[partnerUserId].orEmpty() }
                .distinctUntilChanged()
                .collect { publishMessages() }
        }
        viewModelScope.launch {
            outgoingStore.outcomes.collect { outcome ->
                if (outcome.partnerId != partnerUserId) return@collect
                handleSendOutcome(outcome)
            }
        }
    }

    /** Pull-to-refresh: reload without the full-screen shimmer (lightweight pull indicator). */
    fun refresh() = loadConversation(isRefresh = true)

    fun loadConversation(isRefresh: Boolean = false) {
        if (partnerUserId == 0) {
            _state.update { it.copy(isLoading = false, isRefreshing = false, error = true) }
            return
        }
        // First load shows the shimmer; a refresh shows the pull indicator; a cache-seeded reopen
        // revalidates silently (no shimmer/spinner — content is already on screen).
        val hasContent = _state.value.messages.isNotEmpty() || _state.value.partnerName.isNotBlank()
        _state.update {
            when {
                isRefresh -> it.copy(isRefreshing = true)
                hasContent -> it
                else -> it.copy(isLoading = true, error = false)
            }
        }
        viewModelScope.launch {
            val meResult = profileRepository.getMyProfile()
            val me = (meResult as? ApiResult.Success)?.data
            if (me == null) {
                _state.update { it.copy(isLoading = false, isRefreshing = false, error = !isRefresh && !hasContent) }
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
            // Only a true total failure errors out — resolveChatroom (res) alone is enough to render
            // (its iBlocked/theyBlocked/blockReason drive the gate below).
            if (threadData == null && pub == null && res == null) {
                // Could be a mutual block (their profile/thread 500). Check MY blocked list — works
                // regardless — to show the correct BLOCKED_BY_ME state instead of "Couldn't load".
                val iBlockedThem = blockedUsersStore.isBlocked(partnerUserId) ||
                    (profileRepository.getBlockedUsers(1, 200) as? ApiResult.Success)
                        ?.data?.any { it.userId == partnerUserId } == true
                if (iBlockedThem) {
                    iBlocked = true
                    _state.update {
                        it.copy(isLoading = false, isRefreshing = false, error = false, gate = ChatGate.BLOCKED_BY_ME, gateConfirmed = true)
                    }
                } else {
                    _state.update { it.copy(isLoading = false, isRefreshing = false, error = !isRefresh && !hasContent) }
                }
                return@launch
            }

            val messages = threadData?.messages.orEmpty().sortedBy { it.createdAtEpochMs }
            // Server fields drive the gate (handout tree). Prefer the thread, fall back to the
            // chatroom-resolution / public-profile payloads, then to safe defaults.
            // I-blocked-them is authoritative locally too — covers profile/thread 500s on a mutual block.
            iBlocked = (threadData?.iBlocked ?: res?.iBlocked ?: pub?.iBlocked ?: false) ||
                blockedUsersStore.isBlocked(partnerUserId)
            theyBlocked = threadData?.theyBlocked ?: res?.theyBlocked ?: pub?.theyBlocked ?: false
            invitationStatus = threadData?.invitationStatus ?: res?.invitationStatus ?: false
            isActive = threadData?.isActive ?: res?.isActive ?: true
            // Server-authoritative invite/relationship reason — survives even when messages are
            // deleted, so the gate can still show the pending-invite state.
            blockReason = threadData?.blockReason ?: res?.blockReason
            canMessage = threadData?.canMessage ?: res?.canMessage
            val remaining = res?.remainingInvites ?: threadData?.remainingInvites
            nextCursor = threadData?.nextCursor

            // Server thread is the confirmed list; drop any outbox bubble it now confirms (by client id)
            // and merge the rest so an in-flight/failed send isn't clobbered by the reload.
            confirmed = messages
            outgoingStore.removeConfirmed(partnerUserId, messages.mapNotNull { it.clientMessageId }.toSet())
            val merged = displayMessages()
            _state.update {
                it.copy(
                    partnerName = pub?.displayName?.ifBlank { it.partnerName } ?: it.partnerName,
                    partnerRole = pub?.role?.ifBlank { "Artist" } ?: "Artist",
                    partnerAvatarUrl = pub?.avatarUrl,
                    messages = merged,
                    gate = deriveGate(merged),
                    gateConfirmed = true,
                    remainingInvites = remaining ?: it.remainingInvites,
                    isLoading = false,
                    isRefreshing = false,
                    error = false,
                    canLoadEarlier = nextCursor != null,
                )
            }
            cacheSnapshot(_state.value) // SWR write-through for instant reopen
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
            iBlocked = server.iBlocked || blockedUsersStore.isBlocked(partnerUserId)
            theyBlocked = server.theyBlocked
            blockReason = server.blockReason
            canMessage = server.canMessage
            if (server.nextCursor != null) nextCursor = server.nextCursor
            confirmed = mergeById(confirmed, server.messages)
            outgoingStore.removeConfirmed(partnerUserId, server.messages.mapNotNull { it.clientMessageId }.toSet())
            val merged = displayMessages()
            _state.update { st ->
                st.copy(
                    messages = merged,
                    gate = deriveGate(merged),
                    gateConfirmed = true,
                    remainingInvites = server.remainingInvites ?: st.remainingInvites,
                    canLoadEarlier = nextCursor != null,
                )
            }
            cacheSnapshot(_state.value) // keep the SWR cache fresh after a reconnect backfill
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
        // Page by the server's authoritative cursor (not the oldest visible message's timestamp — an
        // optimistic bubble has no server time, and a raw timestamp can skip/re-fetch the boundary).
        val before = nextCursor ?: run { _state.update { it.copy(canLoadEarlier = false) }; return }
        _state.update { it.copy(isLoadingEarlier = true) }
        viewModelScope.launch {
            val res = messagesRepository.getThread(partnerUserId, currentUserId, before = before)
            if (res !is ApiResult.Success) {
                _state.update { it.copy(isLoadingEarlier = false) }
                return@launch
            }
            nextCursor = res.data.nextCursor
            val existing = confirmed.associateBy { it.id }
            val newOnes = res.data.messages.filter { it.id !in existing }
            confirmed = (newOnes + confirmed).sortedBy { it.createdAtEpochMs }
            val merged = displayMessages()
            _state.update { stt ->
                stt.copy(
                    messages = merged,
                    isLoadingEarlier = false,
                    // Stop when the server signals the start OR this page added nothing new — otherwise
                    // a parked-at-top scroll would re-poll the same page forever.
                    canLoadEarlier = res.data.nextCursor != null && newOnes.isNotEmpty(),
                )
            }
        }
    }

    fun onInputChange(text: String) {
        // Strip URLs in real time (the backend rejects them); other text + whitespace pass through.
        val cleaned = text.replace(URL_REGEX, "")
        _state.update { it.copy(inputText = cleaned) }
        // Outbound typing signal (not while inline-editing an existing message).
        if (cleaned.isNotEmpty() && _state.value.editingMessageId == null) onLocalTypingActivity()
        else stopLocalTyping()
    }

    /** Throttled outbound typing: send is_typing=true ≤1 per [TYPING_PING_INTERVAL_MS]; auto-stop after idle. */
    private fun onLocalTypingActivity() {
        val room = chatroomId ?: return
        val now = System.currentTimeMillis()
        if (now - lastTypingPingMs >= TYPING_PING_INTERVAL_MS) {
            webSocket.sendTyping(room, true)
            lastTypingPingMs = now
            outboundTypingActive = true
        }
        typingStopJob?.cancel()
        typingStopJob = viewModelScope.launch {
            delay(TYPING_IDLE_STOP_MS)
            stopLocalTyping()
        }
    }

    private fun stopLocalTyping() {
        typingStopJob?.cancel()
        if (outboundTypingActive) {
            chatroomId?.let { webSocket.sendTyping(it, false) }
            outboundTypingActive = false
        }
        lastTypingPingMs = 0L
    }

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

        // Hand the send to the app-scoped outbox so it survives leaving the screen. The optimistic
        // bubble appears via the outgoing observer; clear the input now.
        val cid = UUID.randomUUID().toString()
        _state.update { it.copy(inputText = "") }
        stopLocalTyping() // we're done typing the moment we send
        outgoingStore.send(partnerUserId, currentUserId, cid, text)
    }

    fun retryMessage(message: ChatMessage) {
        val cid = message.clientMessageId ?: return
        outgoingStore.retry(partnerUserId, currentUserId, cid)
    }

    /** React to a send result from the outbox (the bubble's SENDING/FAILED state is owned there). */
    private fun handleSendOutcome(outcome: SendOutcome) {
        when (outcome) {
            is SendOutcome.Success -> {
                val data = outcome.result
                chatroomId = data.chatroomId ?: chatroomId
                // Adopt the authoritative gate inputs from the send response.
                invitationStatus = data.invitationStatus
                isActive = data.isActive
                iBlocked = data.iBlocked
                theyBlocked = data.theyBlocked
                // Adopt the confirmed message immediately (dedup by id / client id); the WS echo or a
                // later thread fetch would otherwise be the only source.
                val msg = data.message.copy(clientMessageId = outcome.cid)
                if (confirmed.none { it.id == msg.id || (it.clientMessageId != null && it.clientMessageId == msg.clientMessageId) }) {
                    confirmed = (confirmed + msg).sortedBy { it.createdAtEpochMs }
                }
                publishMessages()
            }
            is SendOutcome.Blocked -> {
                // 403 (block / waiting-for-accept / invite-limit). The outbox already dropped the
                // bubble; show the server's reason and refetch so the gate reflects the true state.
                _toasts.trySend(outcome.message)
                backfill()
            }
            is SendOutcome.Failed -> {
                _toasts.trySend("Couldn't send message. Tap the message to retry.")
            }
        }
    }

    fun onEditMessage(message: ChatMessage, newText: String) {
        val trimmed = newText.trim()
        if (trimmed.isEmpty() || trimmed == message.content) return
        viewModelScope.launch {
            when (val res = messagesRepository.editMessage(message.id, currentUserId, trimmed)) {
                is ApiResult.Success -> {
                    confirmed = confirmed.map { if (it.id == message.id) res.data else it }
                    publishMessages()
                }
                is ApiResult.Error.Validation -> _toasts.trySend(res.message)
                else -> _toasts.trySend("Couldn't edit message.")
            }
        }
    }

    fun onDeleteMessage(message: ChatMessage) {
        viewModelScope.launch {
            when (messagesRepository.deleteMessage(message.id)) {
                is ApiResult.Success -> {
                    confirmed = confirmed.map { if (it.id == message.id) it.copy(isDeleted = true) else it }
                    publishMessages()
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
                confirmed = confirmed.map {
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
                }
                publishMessages()
            }
            is ChatEvent.Delete -> if (hasMessage(event.messageId)) {
                confirmed = confirmed.map { if (it.id == event.messageId) it.copy(isDeleted = true) else it }
                publishMessages()
            }
            is ChatEvent.Read -> {
                // A message I sent was read — mark it read.
                confirmed = confirmed.map {
                    if (it.id == event.messageId && it.isSent) it.copy(isRead = true) else it
                }
                publishMessages()
            }
            is ChatEvent.IncomingNotification -> Unit // handled elsewhere
            is ChatEvent.Typing -> handlePartnerTyping(event)
        }
    }

    /** Inbound: show the "typing…" bubble for the other participant, with a 6s safety auto-clear. */
    private fun handlePartnerTyping(event: ChatEvent.Typing) {
        val room = chatroomId
        if (room == null || event.chatroomId != room || event.userId != partnerUserId) return
        typingClearJob?.cancel()
        if (event.isTyping) {
            _state.update { it.copy(partnerIsTyping = true) }
            typingClearJob = viewModelScope.launch {
                delay(TYPING_AUTO_CLEAR_MS)
                _state.update { it.copy(partnerIsTyping = false) }
            }
        } else {
            _state.update { it.copy(partnerIsTyping = false) }
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
        // A received message means the partner replied. If I had a pending sent invite — either a
        // local sent message, OR the server-tracked pending invite (block_reason == "invite_pending",
        // e.g. after block→delete→unblock left no local messages) — that reply accepts it → activate.
        val iHadPendingInvite = blockReason == "invite_pending" ||
            _state.value.messages.any { it.isSent && it.sendStatus != SendStatus.FAILED }
        if (!msg.isSent && iHadPendingInvite) {
            invitationStatus = true
            isActive = true
            // Clear the stale pending-invite lock so the gate unlocks (otherwise
            // block_reason == "invite_pending" keeps the field disabled).
            blockReason = null
            canMessage = true
        }
        // Dedup: reconcile our own optimistic/echoed message, skip duplicates by id.
        val byClient = msg.clientMessageId
        val matchedClient = byClient != null && confirmed.any { it.clientMessageId == byClient }
        confirmed = when {
            matchedClient -> confirmed.map { if (it.clientMessageId == byClient) msg.copy(clientMessageId = byClient) else it }
            confirmed.any { it.id == msg.id } -> confirmed
            else -> (confirmed + msg).sortedBy { it.createdAtEpochMs }
        }
        // If this echoes one of our outbox bubbles, drop it there so it isn't shown twice.
        if (byClient != null) outgoingStore.removeConfirmed(partnerUserId, setOf(byClient))
        publishMessages()
        if (!msg.isSent) markReadFireAndForget(msg.id)
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private fun hasMessage(id: String): Boolean = confirmed.any { it.id == id }

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
            // A sent invite still pending server-side (block_reason authoritative) → keep the field
            // disabled even if the messages were deleted (block → delete → unblock leaves none).
            blockReason == "invite_pending" -> ChatGate.INVITE_SENT_WAITING
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

    override fun onCleared() {
        super.onCleared()
        // Best-effort "stopped typing" when leaving the thread.
        stopLocalTyping()
        typingClearJob?.cancel()
    }

    private companion object {
        /** http://, https://, and www. URLs — stripped from compose input (handout link-stripping). */
        val URL_REGEX = Regex("""(?i)\b(?:https?://|www\.)\S+""")
        /** Throttle outbound is_typing=true (server rate-limits ≤1 per 2s — stay above that). */
        const val TYPING_PING_INTERVAL_MS = 3_000L
        /** No keystroke for this long → send is_typing=false. */
        const val TYPING_IDLE_STOP_MS = 5_000L
        /** Safety net: clear the inbound "typing…" bubble if no fresh frame arrives. */
        const val TYPING_AUTO_CLEAR_MS = 6_000L
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
