package com.example.artrinx.feature.notifications.domain.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.example.artrinx.R

// ── Notifications ─────────────────────────────────────────────────────────────

@Immutable
data class NotificationItem(
    val id: String,
    val message: String,
    val timeAgo: String,
    val isRead: Boolean = true,
    @param:DrawableRes val thumbnailRes: Int? = null,  // square art image (mock/local)
    @param:DrawableRes val avatarRes: Int? = null,     // circular person avatar (mock/local)
    val thumbnailUrl: String? = null,                  // square art image (from API target)
    val avatarUrl: String? = null,                     // circular person avatar (from API actor)
    val type: String? = null,                          // raw API type, for future tap routing
)

// ── Messages / Conversations ──────────────────────────────────────────────────

enum class ConversationState { INVITATION_PENDING, ACTIVE }

@Immutable
data class ConversationItem(
    val id: String,                    // the OTHER user's id (as String) — the chat nav key
    val userName: String,
    val userHandle: String,
    val userRole: String = "Artist",
    @param:DrawableRes val avatarRes: Int? = null,
    val avatarUrl: String? = null,
    val lastMessage: String,
    val timestamp: String,
    val isUnread: Boolean = false,
    val unreadCount: Int = 0,
    val state: ConversationState = ConversationState.ACTIVE,
)

// ── Chat messages ─────────────────────────────────────────────────────────────

/** Local delivery state for optimistic send (not on the wire). */
enum class SendStatus { SENDING, SENT, FAILED }

/**
 * The current "what can I do in this chat" gate, derived from message authorship + block flags
 * (see plan: the invitation booleans are ambiguous, so authorship + the send-time 403 are the
 * authoritative signals).
 */
enum class ChatGate { FRESH_INVITE, INVITE_SENT_WAITING, INVITE_RECEIVED, ACTIVE, BLOCKED }

@Immutable
data class ChatMessage(
    val id: String,                    // server uuid, or temp client id while sending
    val content: String,
    val isSent: Boolean,               // true = sent by current user (senderId == currentUserId)
    val timestamp: String,             // display string ("Mon, Oct 28 at 4:43 PM")
    val createdAtIso: String = "",     // raw ISO — used for sorting
    val createdAtEpochMs: Long = 0L,   // parsed epoch — used for the 15-min edit window
    val clientMessageId: String? = null,
    val isRead: Boolean = false,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val sendStatus: SendStatus = SendStatus.SENT,
    // Shared-artwork card (populated for share-an-artwork messages).
    val artworkTitle: String? = null,
    val artworkImageUrl: String? = null,
    val sharedArtistName: String? = null,
    val sharedArtistAvatarUrl: String? = null,
    // Legacy flags kept for the mock data; no longer drive UI.
    val isInvitation: Boolean = false,
    val isInvitationAccepted: Boolean = false,
)

// ── Repository result models ──────────────────────────────────────────────────

@Immutable
data class ChatThread(
    val messages: List<ChatMessage>,
    val invitationStatus: Boolean,
    val isActive: Boolean,
    val iBlocked: Boolean,
    val theyBlocked: Boolean,
    val nextCursor: String?,
)

@Immutable
data class ChatroomResolution(
    val exists: Boolean,
    val chatroomId: String?,
    val invitationStatus: Boolean,
    val isActive: Boolean,
    val iBlocked: Boolean,
    val theyBlocked: Boolean,
    val remainingInvites: Int?,
)

@Immutable
data class SendResult(
    val message: ChatMessage,
    val chatroomId: String?,
    val invitationStatus: Boolean,
    val isActive: Boolean,
    val iBlocked: Boolean,
    val theyBlocked: Boolean,
)

// ── New message user list ─────────────────────────────────────────────────────

@Immutable
data class UserContact(
    val id: String,                    // the user's id (chat nav key)
    val name: String,
    val handle: String,
    @param:DrawableRes val avatarRes: Int? = null,
    val avatarUrl: String? = null,
)

// ── Notification tab enum ─────────────────────────────────────────────────────

enum class NotifTab { NOTIFICATIONS, MESSAGES }

// ── Mock data ─────────────────────────────────────────────────────────────────

object MockNotificationData {

    val notifications = listOf(
        NotificationItem(
            id = "n1",
            message = "Hayley AG shared Wade Huston's profile with you",
            timeAgo = "1h ago", isRead = false,
            thumbnailRes = null, avatarRes = R.drawable.art_sample_street_poster,
        ),
        NotificationItem(
            id = "n2",
            message = "\"art title\" got 50 likes",
            timeAgo = "2h ago", isRead = true,
            thumbnailRes = R.drawable.art_sample_cosmic_swirl, avatarRes = null,
        ),
        NotificationItem(
            id = "n3",
            message = "\"Curation title\" got 50 likes",
            timeAgo = "2h ago", isRead = true,
            thumbnailRes = R.drawable.art_sample_fluid_purple, avatarRes = null,
        ),
        NotificationItem(
            id = "n4",
            message = "Catherine D shared \"Coral leaf\" as their curation with public",
            timeAgo = "2h ago", isRead = true,
            thumbnailRes = null, avatarRes = R.drawable.art_sample_artist_outdoors,
        ),
        NotificationItem(
            id = "n5",
            message = "\"Blue1 four four four four four four four\" got 50 likes",
            timeAgo = "3 days ago", isRead = true,
            thumbnailRes = R.drawable.art_sample_neon_corridor, avatarRes = null,
        ),
        NotificationItem(
            id = "n6",
            message = "Sarina Charugundla Ashokugundlas shared \"Blue1 four four four four four four four\" with you.",
            timeAgo = "3 months ago", isRead = true,
            thumbnailRes = null, avatarRes = R.drawable.art_sample_painted_hands,
        ),
    )

    val conversations = listOf(
        ConversationItem(
            id = "c1", userName = "Wade Huston", userHandle = "@workbywade", userRole = "Artist",
            avatarRes = null,
            lastMessage = "Hi Wade,\nI'm interested in this piece you post...",
            timestamp = "Oct 28", isUnread = false, state = ConversationState.INVITATION_PENDING,
        ),
        ConversationItem(
            id = "c2", userName = "Sophia Ahamed", userHandle = "@sophahem", userRole = "Artist",
            avatarRes = null,
            lastMessage = "Yes, this is for sale on my website!\nHere's the link, let me know if you...",
            timestamp = "Sept 7", isUnread = true, state = ConversationState.ACTIVE,
        ),
        ConversationItem(
            id = "c3", userName = "Hayley AG", userHandle = "@hayleyag", userRole = "Artist",
            avatarRes = null,
            lastMessage = "Thought you might enjoy this, the theme is really cool :) 🎨",
            timestamp = "Dec 2023", isUnread = false, state = ConversationState.ACTIVE,
        ),
    )

    val chatMessages: Map<String, List<ChatMessage>> = mapOf(
        "c1" to listOf(
            ChatMessage(
                id = "m1", isSent = false, isInvitation = true,
                timestamp = "Mon, Oct 28 at 4:43 PM",
                content = "Hi Wade,\nHow are you doing? It's fun seeing you on RINX! It was nice meeting you at the event the other day.\n\nDo you have any new art available to buy?",
            ),
            ChatMessage(
                id = "m2", isSent = true, isInvitationAccepted = true,
                timestamp = "Mon, Oct 28 at 8:20 PM",
                content = "Hi Hayley,\nI am doing well, thanks! It is fun! Lovely meeting you too, excited to stay in touch and follow your art journey!",
            ),
        ),
        "c2" to listOf(
            ChatMessage(
                id = "m3", isSent = true,
                timestamp = "Sept 7 at 2:14 PM",
                content = "Hi Sophia! Loved your latest piece — is it available for sale?",
            ),
            ChatMessage(
                id = "m4", isSent = false,
                timestamp = "Sept 7 at 3:01 PM",
                content = "Yes, this is for sale on my website!\nHere's the link, let me know if you have questions.",
            ),
        ),
        "c3" to listOf(
            ChatMessage(
                id = "m5", isSent = false,
                timestamp = "Dec 2023",
                content = "Thought you might enjoy this, the theme is really cool :) 🎨",
            ),
        ),
    )

    val contacts = listOf(
        UserContact("u1", "Hayley G",      "@hayleyag",    null),
        UserContact("u2", "Sophia Ahamed", "@sophahem",    null),
        UserContact("u3", "Wade Hutson",   "@workbywade",  null),
        UserContact("u4", "Kent C",        "@kentoo",      null),
        UserContact("u5", "Sarina Char",   "@sarchar",     null),
        UserContact("u6", "Hayley G",      "@hayleyag",    null),
        UserContact("u7", "Hayley G",      "@hayleyag",    null),
    )

    val invitationCount: Map<String, Int> = mapOf(
        "basic"   to 2,
        "premium" to 12,
    )
}
