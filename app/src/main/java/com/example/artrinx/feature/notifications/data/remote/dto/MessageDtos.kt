package com.example.artrinx.feature.notifications.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// ── Message shape (API §7.4 — camelCase on the wire) ──────────────────────────

// camelCase is the primary key (REST contract §7.4); snake_case is accepted as a fallback
// because the WebSocket fans out messages in snake_case (see ChatWebSocketManager). Without
// the snake_case alternates, `senderId` parses to null on the socket path and every message
// is treated as received (isSent == false).
data class MessageDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName(value = "chatroomId", alternate = ["chatroom_id"]) val chatroomId: String? = null,
    @SerializedName(value = "senderId", alternate = ["sender_id"]) val senderId: Int? = null,
    @SerializedName(value = "receiverId", alternate = ["receiver_id"]) val receiverId: Int? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName(value = "imageId", alternate = ["image_id"]) val imageId: Int? = null,
    @SerializedName(value = "imageUrl", alternate = ["image_url"]) val imageUrl: String? = null,
    @SerializedName(value = "isRead", alternate = ["is_read"]) val isRead: Boolean? = null,
    @SerializedName(value = "isDeleted", alternate = ["is_deleted"]) val isDeleted: Boolean? = null,
    @SerializedName(value = "isEdited", alternate = ["is_edited"]) val isEdited: Boolean? = null,
    @SerializedName(value = "createdAt", alternate = ["created_at"]) val createdAt: String? = null,
    @SerializedName(value = "editedAt", alternate = ["edited_at"]) val editedAt: String? = null,
    @SerializedName(value = "clientMessageId", alternate = ["client_message_id"]) val clientMessageId: String? = null,
    // Shared-artwork metadata (populated when imageId references an artwork).
    @SerializedName(value = "mediaUserId", alternate = ["media_user_id"]) val mediaUserId: Int? = null,
    @SerializedName(value = "mediaUserDisplayName", alternate = ["media_user_display_name"]) val mediaUserDisplayName: String? = null,
    @SerializedName(value = "mediaUserProfilePictureUrl", alternate = ["media_user_profile_picture_url"]) val mediaUserProfilePictureUrl: String? = null,
    @SerializedName(value = "artworkTitle", alternate = ["artwork_title"]) val artworkTitle: String? = null,
)

// ── Request bodies (snake_case on the wire) ───────────────────────────────────

data class SendMessageRequest(
    @SerializedName("receiver_id") val receiverId: Int,
    @SerializedName("text") val text: String,
    @SerializedName("image_id") val imageId: Int? = null,
    @SerializedName("client_message_id") val clientMessageId: String,
)

data class EditMessageRequest(
    @SerializedName("text") val text: String,
)

data class ResolveChatroomRequest(
    @SerializedName("user_id") val userId: Int,
)

// ── Response data shapes ──────────────────────────────────────────────────────

/** `POST /api/messages/` → data (API §7.2). Block/invitation flags are camelCase. */
data class SendMessageResponseDto(
    @SerializedName("message") val message: MessageDto? = null,
    @SerializedName(value = "chatroomId", alternate = ["chatroom_id"]) val chatroomId: String? = null,
    @SerializedName(value = "invitationStatus", alternate = ["invitation_status"]) val invitationStatus: Boolean? = null,
    @SerializedName(value = "iBlocked", alternate = ["i_blocked"]) val iBlocked: Boolean? = null,
    @SerializedName(value = "theyBlocked", alternate = ["they_blocked"]) val theyBlocked: Boolean? = null,
    @SerializedName(value = "isActive", alternate = ["is_active"]) val isActive: Boolean? = null,
)

/** `GET /api/messages/with/{userId}` → data (API §7.3). */
data class ChatThreadDto(
    @SerializedName(value = "invitationStatus", alternate = ["invitation_status"]) val invitationStatus: Boolean? = null,
    @SerializedName("messages") val messages: List<MessageDto>? = null,
    @SerializedName(value = "iBlocked", alternate = ["i_blocked"]) val iBlocked: Boolean? = null,
    @SerializedName(value = "theyBlocked", alternate = ["they_blocked"]) val theyBlocked: Boolean? = null,
    @SerializedName(value = "isActive", alternate = ["is_active"]) val isActive: Boolean? = null,
    @SerializedName(value = "nextCursor", alternate = ["next_cursor"]) val nextCursor: String? = null,
)

/** `POST /api/chatroom_id` → data (API §7.9 — snake_case). */
data class ChatroomResolveDto(
    @SerializedName("exists") val exists: Boolean? = null,
    @SerializedName("chatroom_id") val chatroomId: String? = null,
    @SerializedName("invitation_status") val invitationStatus: Boolean? = null,
    @SerializedName("is_active") val isActive: Boolean? = null,
    @SerializedName("i_blocked") val iBlocked: Boolean? = null,
    @SerializedName("they_blocked") val theyBlocked: Boolean? = null,
    @SerializedName("remaining_invites") val remainingInvites: Int? = null,
)

/**
 * `GET /api/my/chatrooms` → list item (API §7.8). The exact shape isn't pinned in the contract,
 * so this is intentionally defensive: it accepts both nested (`otherUser`/`lastMessage`) and flat
 * variants, and both camelCase and snake_case keys. `lastMessage` is a [JsonElement] because the
 * backend may send it as either a string or a full message object — extracted defensively in the
 * repository. Confirm/trim the real shape at runtime.
 */
data class ChatroomPreviewDto(
    @SerializedName("chatroomId") val chatroomId: String? = null,
    @SerializedName("chatroom_id") val chatroomIdSnake: String? = null,

    @SerializedName("otherUser") val otherUser: ChatPreviewUserDto? = null,
    @SerializedName("user") val user: ChatPreviewUserDto? = null,

    // Flat fallbacks for the other participant.
    @SerializedName("userId") val userId: Int? = null,
    @SerializedName("user_id") val userIdSnake: Int? = null,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("display_name") val displayNameSnake: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("profileTypeName") val profileTypeName: String? = null,
    @SerializedName("profile_type_name") val profileTypeNameSnake: String? = null,
    @SerializedName("profilePictureUrl") val profilePictureUrl: String? = null,
    @SerializedName("profile_picture_url") val profilePictureUrlSnake: String? = null,

    @SerializedName("lastMessage") val lastMessage: JsonElement? = null,
    @SerializedName("last_message") val lastMessageSnake: JsonElement? = null,
    @SerializedName("lastMessageAt") val lastMessageAt: String? = null,
    @SerializedName("last_message_at") val lastMessageAtSnake: String? = null,

    @SerializedName("unreadCount") val unreadCount: Int? = null,
    @SerializedName("unread_count") val unreadCountSnake: Int? = null,

    @SerializedName("invitationStatus") val invitationStatus: Boolean? = null,
    @SerializedName("invitation_status") val invitationStatusSnake: Boolean? = null,
    @SerializedName("isActive") val isActive: Boolean? = null,
    @SerializedName("is_active") val isActiveSnake: Boolean? = null,
)

data class ChatPreviewUserDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("user_id") val userId: Int? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("display_name") val displayNameSnake: String? = null,
    @SerializedName("profileTypeName") val profileTypeName: String? = null,
    @SerializedName("profile_type_name") val profileTypeNameSnake: String? = null,
    @SerializedName("profilePictureUrl") val profilePictureUrl: String? = null,
    @SerializedName("profile_picture_url") val profilePictureUrlSnake: String? = null,
)
