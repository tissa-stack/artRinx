package com.rinx.artRINXapp.feature.notifications.data.local

import com.rinx.artRINXapp.feature.notifications.domain.model.ChatGate
import com.rinx.artRINXapp.feature.notifications.domain.model.ChatMessage
import javax.inject.Inject
import javax.inject.Singleton

/** Last-known conversation snapshot, keyed by partner user id, for instant chat re-open (SWR). */
data class ChatSnapshot(
    val messages: List<ChatMessage>,
    val partnerName: String,
    val partnerRole: String,
    val partnerAvatarUrl: String?,
    val gate: ChatGate,
    val remainingInvites: Int?,
    val invitationStatus: Boolean,
    val isActive: Boolean,
    val iBlocked: Boolean,
    val theyBlocked: Boolean,
    val blockReason: String?,
    val canMessage: Boolean?,
    val chatroomId: String?,
    val nextCursor: String?,
)

/**
 * In-memory, app-lifetime cache of recently-opened chats so reopening renders instantly (no shimmer);
 * the ViewModel seeds from here then silently revalidates. Bounded LRU. Cleared on logout/delete
 * (see core/auth/LocalDataCleaner). Mirrors feature/home/data/local/DetailCache.
 */
@Singleton
class ChatCache @Inject constructor() {

    private val map = object : LinkedHashMap<Int, ChatSnapshot>(16, 0.75f, /* accessOrder = */ true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, ChatSnapshot>?): Boolean = size > CAP
    }

    @Synchronized fun get(partnerId: Int): ChatSnapshot? = map[partnerId]

    @Synchronized fun put(partnerId: Int, snapshot: ChatSnapshot) {
        map[partnerId] = snapshot
    }

    @Synchronized fun clear() = map.clear()

    private companion object {
        const val CAP = 20
    }
}
