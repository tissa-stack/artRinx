package com.example.artrinx.feature.notifications.domain.repository

import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.notifications.domain.model.ChatMessage
import com.example.artrinx.feature.notifications.domain.model.ChatThread
import com.example.artrinx.feature.notifications.domain.model.ChatroomResolution
import com.example.artrinx.feature.notifications.domain.model.ConversationItem
import com.example.artrinx.feature.notifications.domain.model.SendResult
import com.google.gson.JsonObject

interface MessagesRepository {

    /** Resolve a user id → chatroom existence + invitation/block state + remaining invites (§7.9). */
    suspend fun resolveChatroom(userId: Int): ApiResult<ChatroomResolution>

    /** Fetch a thread page. [currentUserId] flags each message's `isSent`. (§7.3) */
    suspend fun getThread(
        userId: Int,
        currentUserId: Int,
        before: String? = null,
        limit: Int = DEFAULT_LIMIT,
    ): ApiResult<ChatThread>

    /** Send a message / invitation (§7.2). A 403 (waiting-for-accept / block / invite-limit) maps
     *  to [ApiResult.Error.Blocked] — never a sign-out. */
    suspend fun sendMessage(
        receiverId: Int,
        currentUserId: Int,
        text: String,
        imageId: Int? = null,
        clientMessageId: String,
    ): ApiResult<SendResult>

    /** Edit text within the 15-min window (§7.5). */
    suspend fun editMessage(messageId: String, currentUserId: Int, text: String): ApiResult<ChatMessage>

    /** Soft-delete a message (§7.6). */
    suspend fun deleteMessage(messageId: String): ApiResult<Unit>

    /** Mark a message read (§7.7). */
    suspend fun markRead(messageId: String): ApiResult<Unit>

    /** Inbox previews (§7.8). */
    suspend fun getChatrooms(): ApiResult<List<ConversationItem>>

    /** Delete a chat from the current user's side only (§7.10). */
    suspend fun deleteChat(userId: Int): ApiResult<Unit>

    /** Map a raw WebSocket `chat` payload into a domain message. Returns null if unparseable. */
    fun parseIncomingMessage(json: JsonObject, currentUserId: Int): ChatMessage?

    companion object {
        const val DEFAULT_LIMIT = 50
    }
}
