package com.rinx.artRINXapp.feature.notifications.domain

import com.rinx.artRINXapp.core.di.ApplicationScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.notifications.domain.model.ChatMessage
import com.rinx.artRINXapp.feature.notifications.domain.model.SendResult
import com.rinx.artRINXapp.feature.notifications.domain.model.SendStatus
import com.rinx.artRINXapp.feature.notifications.domain.repository.MessagesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Result of an app-scoped send, surfaced so a live ChatViewModel can adopt gate inputs / toast. */
sealed interface SendOutcome {
    val partnerId: Int
    val cid: String
    data class Success(override val partnerId: Int, override val cid: String, val result: SendResult) : SendOutcome
    data class Blocked(override val partnerId: Int, override val cid: String, val message: String) : SendOutcome
    data class Failed(override val partnerId: Int, override val cid: String) : SendOutcome
}

/**
 * App-scoped outbox for outgoing chat messages. The network send runs on the application scope, so
 * leaving the chat screen never cancels an in-flight send (the previous bug: a send on viewModelScope
 * was cancelled on back-navigation and the optimistic bubble — never cached — vanished).
 *
 * Holds only UNCONFIRMED bubbles (SENDING / FAILED) per partner id. A confirmed send is removed here
 * and re-appears as a normal message via the WebSocket echo or the next thread fetch; the live
 * ViewModel also adopts it immediately from [outcomes]. Mirrors core/offline/LiveMutationQueue.
 */
@Singleton
class OutgoingMessageStore @Inject constructor(
    private val messagesRepository: MessagesRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _outgoing = MutableStateFlow<Map<Int, List<ChatMessage>>>(emptyMap())
    val outgoing: StateFlow<Map<Int, List<ChatMessage>>> = _outgoing.asStateFlow()

    private val _outcomes = MutableSharedFlow<SendOutcome>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val outcomes: SharedFlow<SendOutcome> = _outcomes.asSharedFlow()

    fun pendingFor(partnerId: Int): List<ChatMessage> = _outgoing.value[partnerId].orEmpty()

    /** Queue + dispatch a new outgoing message. */
    fun send(partnerId: Int, currentUserId: Int, cid: String, text: String) {
        upsert(
            partnerId,
            ChatMessage(
                id = cid,
                content = text,
                isSent = true,
                timestamp = "",
                clientMessageId = cid,
                sendStatus = SendStatus.SENDING,
                createdAtEpochMs = System.currentTimeMillis(),
            ),
        )
        dispatch(partnerId, currentUserId, cid, text)
    }

    /** Re-dispatch a FAILED (or restored-orphan) message. */
    fun retry(partnerId: Int, currentUserId: Int, cid: String) {
        val existing = _outgoing.value[partnerId]?.firstOrNull { it.clientMessageId == cid } ?: return
        upsert(partnerId, existing.copy(sendStatus = SendStatus.SENDING))
        dispatch(partnerId, currentUserId, cid, existing.content)
    }

    /**
     * Re-own unconfirmed bubbles recovered from the display cache (e.g. after process death) as
     * FAILED + retryable — but never clobber a cid that is already tracked here (an in-flight send
     * after an in-session reopen). Confirmed (SENT) messages are not passed in.
     */
    fun restoreAsFailed(partnerId: Int, messages: List<ChatMessage>) {
        if (messages.isEmpty()) return
        _outgoing.update { map ->
            val existing = map[partnerId].orEmpty()
            val existingCids = existing.mapNotNull { it.clientMessageId }.toSet()
            val add = messages
                .filter { (it.clientMessageId ?: it.id) !in existingCids }
                .map { it.copy(sendStatus = SendStatus.FAILED) }
            if (add.isEmpty()) map else map + (partnerId to existing + add)
        }
    }

    /** Drop any pending entries whose message is now confirmed by the server (dedup by client id). */
    fun removeConfirmed(partnerId: Int, confirmedClientIds: Set<String>) {
        if (confirmedClientIds.isEmpty()) return
        _outgoing.update { map ->
            val list = map[partnerId]?.filterNot { it.clientMessageId in confirmedClientIds } ?: return@update map
            if (list.isEmpty()) map - partnerId else map + (partnerId to list)
        }
    }

    fun clear() {
        _outgoing.value = emptyMap()
    }

    private fun dispatch(partnerId: Int, currentUserId: Int, cid: String, text: String) {
        scope.launch {
            when (val res = messagesRepository.sendMessage(partnerId, currentUserId, text, clientMessageId = cid)) {
                is ApiResult.Success -> {
                    remove(partnerId, cid)
                    _outcomes.emit(SendOutcome.Success(partnerId, cid, res.data))
                }
                is ApiResult.Error.Blocked -> {
                    remove(partnerId, cid)
                    _outcomes.emit(SendOutcome.Blocked(partnerId, cid, res.message))
                }
                else -> {
                    markFailed(partnerId, cid)
                    _outcomes.emit(SendOutcome.Failed(partnerId, cid))
                }
            }
        }
    }

    private fun remove(partnerId: Int, cid: String) {
        _outgoing.update { map ->
            val list = map[partnerId]?.filterNot { it.clientMessageId == cid }.orEmpty()
            if (list.isEmpty()) map - partnerId else map + (partnerId to list)
        }
    }

    private fun upsert(partnerId: Int, msg: ChatMessage) {
        _outgoing.update { map ->
            val list = map[partnerId].orEmpty().filterNot { it.clientMessageId == msg.clientMessageId } + msg
            map + (partnerId to list)
        }
    }

    private fun markFailed(partnerId: Int, cid: String) {
        _outgoing.update { map ->
            val list = map[partnerId]?.map {
                if (it.clientMessageId == cid) it.copy(sendStatus = SendStatus.FAILED) else it
            }.orEmpty()
            map + (partnerId to list)
        }
    }
}
