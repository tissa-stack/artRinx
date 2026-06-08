package com.rinx.artRINXapp.feature.notifications.data.repository

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.notifications.data.remote.MessagesApiService
import com.rinx.artRINXapp.feature.notifications.data.remote.dto.ChatroomPreviewDto
import com.rinx.artRINXapp.feature.notifications.data.remote.dto.EditMessageRequest
import com.rinx.artRINXapp.feature.notifications.data.remote.dto.MessageDto
import com.rinx.artRINXapp.feature.notifications.data.remote.dto.ResolveChatroomRequest
import com.rinx.artRINXapp.feature.notifications.data.remote.dto.SendMessageRequest
import com.rinx.artRINXapp.feature.notifications.domain.model.ChatMessage
import com.rinx.artRINXapp.feature.notifications.domain.model.ChatThread
import com.rinx.artRINXapp.feature.notifications.domain.model.ChatroomResolution
import com.rinx.artRINXapp.feature.notifications.domain.model.ConversationItem
import com.rinx.artRINXapp.feature.notifications.domain.model.ConversationState
import com.rinx.artRINXapp.feature.notifications.domain.model.SendResult
import com.rinx.artRINXapp.feature.notifications.domain.model.SendStatus
import com.rinx.artRINXapp.feature.notifications.domain.repository.MessagesRepository
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import retrofit2.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class MessagesRepositoryImpl @Inject constructor(
    private val apiService: MessagesApiService,
) : MessagesRepository {

    private val gson = Gson()

    override suspend fun resolveChatroom(userId: Int): ApiResult<ChatroomResolution> = safeCall {
        val response = apiService.resolveChatroom(ResolveChatroomRequest(userId))
        val dto = response.body()?.data
        if (response.isSuccessful && dto != null) {
            ApiResult.Success(
                ChatroomResolution(
                    exists = dto.exists ?: false,
                    chatroomId = dto.chatroomId,
                    invitationStatus = dto.invitationStatus ?: false,
                    isActive = dto.isActive ?: true,
                    iBlocked = dto.iBlocked ?: false,
                    theyBlocked = dto.theyBlocked ?: false,
                    remainingInvites = dto.remainingInvites,
                ),
            )
        } else {
            errorFor(response)
        }
    }

    override suspend fun getThread(
        userId: Int,
        currentUserId: Int,
        before: String?,
        limit: Int,
    ): ApiResult<ChatThread> = safeCall {
        val response = apiService.getThread(userId, before, limit)
        val dto = response.body()?.data
        if (response.isSuccessful && dto != null) {
            ApiResult.Success(
                ChatThread(
                    messages = dto.messages.orEmpty().map { it.toChatMessage(currentUserId) },
                    invitationStatus = dto.invitationStatus ?: false,
                    isActive = dto.isActive ?: true,
                    iBlocked = dto.iBlocked ?: false,
                    theyBlocked = dto.theyBlocked ?: false,
                    nextCursor = dto.nextCursor,
                ),
            )
        } else {
            errorFor(response)
        }
    }

    override suspend fun sendMessage(
        receiverId: Int,
        currentUserId: Int,
        text: String,
        imageId: Int?,
        clientMessageId: String,
    ): ApiResult<SendResult> = safeCall {
        val response = apiService.sendMessage(
            SendMessageRequest(
                receiverId = receiverId,
                text = text,
                imageId = imageId,
                clientMessageId = clientMessageId,
            ),
        )
        val dto = response.body()?.data
        if (response.isSuccessful && dto?.message != null) {
            ApiResult.Success(
                SendResult(
                    message = dto.message.toChatMessage(currentUserId),
                    chatroomId = dto.chatroomId,
                    invitationStatus = dto.invitationStatus ?: false,
                    isActive = dto.isActive ?: true,
                    iBlocked = dto.iBlocked ?: false,
                    theyBlocked = dto.theyBlocked ?: false,
                ),
            )
        } else {
            errorFor(response)
        }
    }

    override suspend fun editMessage(
        messageId: String,
        currentUserId: Int,
        text: String,
    ): ApiResult<ChatMessage> = safeCall {
        val response = apiService.editMessage(messageId, EditMessageRequest(text))
        val dto = response.body()?.data
        if (response.isSuccessful && dto != null) {
            ApiResult.Success(dto.toChatMessage(currentUserId))
        } else {
            // 400 with "Edit window expired" → surface the server message so the UI can toast it.
            val raw = response.errorBody()?.string()
            if (response.code() == 400 && raw?.contains("Edit window expired", ignoreCase = true) == true) {
                ApiResult.Error.Validation("Edit window expired")
            } else {
                errorFor(response)
            }
        }
    }

    override suspend fun deleteMessage(messageId: String): ApiResult<Unit> = safeCall {
        val response = apiService.deleteMessage(messageId)
        if (response.isSuccessful) ApiResult.Success(Unit) else errorFor(response)
    }

    override suspend fun markRead(messageId: String): ApiResult<Unit> = safeCall {
        val response = apiService.markRead(messageId)
        if (response.isSuccessful) ApiResult.Success(Unit) else errorFor(response)
    }

    override suspend fun getChatrooms(): ApiResult<List<ConversationItem>> = safeCall {
        val response = apiService.getChatrooms()
        if (response.isSuccessful) {
            ApiResult.Success(response.body()?.data.orEmpty().mapNotNull { it.toConversationItem() })
        } else {
            errorFor(response)
        }
    }

    override suspend fun deleteChat(userId: Int): ApiResult<Unit> = safeCall {
        val response = apiService.deleteChat(userId)
        if (response.isSuccessful) ApiResult.Success(Unit) else errorFor(response)
    }

    override fun parseIncomingMessage(json: JsonObject, currentUserId: Int): ChatMessage? = try {
        gson.fromJson(json, MessageDto::class.java)?.toChatMessage(currentUserId)
    } catch (_: Exception) {
        null
    }

    // ── Mappers ────────────────────────────────────────────────────────────────

    private fun MessageDto.toChatMessage(currentUserId: Int): ChatMessage = ChatMessage(
        id = id ?: clientMessageId ?: createdAt.orEmpty(),
        content = text.orEmpty(),
        isSent = senderId == currentUserId,
        timestamp = formatMessageStamp(createdAt),
        createdAtIso = createdAt.orEmpty(),
        createdAtEpochMs = parseIso(createdAt)?.time ?: 0L,
        clientMessageId = clientMessageId,
        isRead = isRead ?: false,
        isEdited = isEdited ?: false,
        isDeleted = isDeleted ?: false,
        sendStatus = SendStatus.SENT,
        artworkTitle = artworkTitle,
        artworkImageUrl = imageUrl?.takeIf { imageId != null },
        sharedArtistName = mediaUserDisplayName,
        sharedArtistAvatarUrl = mediaUserProfilePictureUrl,
    )

    private fun ChatroomPreviewDto.toConversationItem(): ConversationItem? {
        val otherUser = otherUser ?: user
        val otherId = otherUser?.id ?: otherUser?.userId ?: userId ?: userIdSnake ?: return null
        val displayName = otherUser?.displayName ?: otherUser?.displayNameSnake
            ?: displayName ?: displayNameSnake ?: username ?: otherUser?.username ?: "User"
        val handle = (otherUser?.username ?: username)?.let { "@$it" } ?: ""
        val role = otherUser?.profileTypeName ?: otherUser?.profileTypeNameSnake
            ?: profileTypeName ?: profileTypeNameSnake ?: "Artist"
        val avatar = otherUser?.profilePictureUrl ?: otherUser?.profilePictureUrlSnake
            ?: profilePictureUrl ?: profilePictureUrlSnake
        val unread = unreadCount ?: unreadCountSnake ?: 0
        val lastMsgEl = lastMessage ?: lastMessageSnake
        val lastText = extractLastMessageText(lastMsgEl)
        val stamp = formatPreviewStamp(lastMessageAt ?: lastMessageAtSnake ?: lastMessageCreatedAt(lastMsgEl))
        val invitation = invitationStatus ?: invitationStatusSnake ?: false

        return ConversationItem(
            id = otherId.toString(),
            userName = displayName,
            userHandle = handle,
            userRole = role,
            avatarUrl = avatar,
            lastMessage = lastText,
            timestamp = stamp,
            isUnread = unread > 0,
            unreadCount = unread,
            // INVITATION_PENDING only flags the list badge; the chat screen recomputes its own gate.
            // invitationStatus=true means the invite was accepted / chat is active (verified 2026-06-08),
            // so the "invitation pending" badge shows whenever it is NOT yet accepted. (Read state is
            // not used — reading an invite is not the same as replying to it.)
            state = if (!invitation) ConversationState.INVITATION_PENDING
                    else ConversationState.ACTIVE,
        )
    }

    /** `lastMessage` may be a bare string or a full message object. */
    private fun extractLastMessageText(el: JsonElement?): String = when {
        el == null || el.isJsonNull -> ""
        el.isJsonPrimitive -> el.asString
        el.isJsonObject -> {
            val obj = el.asJsonObject
            when {
                obj.has("isDeleted") && obj.get("isDeleted").let { !it.isJsonNull && it.asBoolean } -> "Message deleted"
                obj.has("text") && !obj.get("text").isJsonNull -> obj.get("text").asString
                obj.has("artworkTitle") && !obj.get("artworkTitle").isJsonNull -> "Shared an artwork"
                else -> ""
            }
        }
        else -> ""
    }

    private fun lastMessageCreatedAt(el: JsonElement?): String? {
        if (el == null || !el.isJsonObject) return null
        val obj = el.asJsonObject
        return obj.get("createdAt")?.takeIf { !it.isJsonNull }?.asString
    }

    // ── Date formatting ──────────────────────────────────────────────────────────

    /** ISO 8601 → "Mon, Oct 28 at 4:43 PM" (device-local). Falls back to "" on parse failure. */
    private fun formatMessageStamp(iso: String?): String {
        val date = parseIso(iso) ?: return ""
        return SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.US).format(date)
    }

    /** ISO 8601 → "MMM d" (e.g. "Oct 28") for inbox previews. */
    private fun formatPreviewStamp(iso: String?): String {
        val date = parseIso(iso) ?: return ""
        return SimpleDateFormat("MMM d", Locale.US).format(date)
    }

    private fun parseIso(iso: String?): Date? {
        if (iso.isNullOrBlank()) return null
        for (pattern in ISO_PATTERNS) {
            try {
                val fmt = SimpleDateFormat(pattern, Locale.US)
                fmt.timeZone = TimeZone.getTimeZone("UTC")
                return fmt.parse(iso)
            } catch (_: Exception) {
                // try next
            }
        }
        return null
    }

    // ── Error handling (mirrors SearchRepositoryImpl) ─────────────────────────────

    private inline fun <T> safeCall(block: () -> ApiResult<T>): ApiResult<T> = try {
        block()
    } catch (e: IOException) {
        ApiResult.Error.Network(e)
    } catch (e: Exception) {
        ApiResult.Error.Unknown(e)
    }

    /** 403 is an action-denied (block / waiting-for-accept / invite-limit) — NOT a sign-out (§7.2). */
    private fun errorFor(response: Response<*>): ApiResult.Error = when (response.code()) {
        403 -> ApiResult.Error.Blocked(response.errorBody()?.string()?.take(300) ?: "Action not allowed")
        404 -> ApiResult.Error.NotFound("Not found")
        in 400..499 -> ApiResult.Error.Validation("Request failed (${response.code()})")
        in 500..599 -> ApiResult.Error.Server(response.code())
        else -> ApiResult.Error.Unknown(RuntimeException("HTTP ${response.code()}"))
    }

    private companion object {
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
