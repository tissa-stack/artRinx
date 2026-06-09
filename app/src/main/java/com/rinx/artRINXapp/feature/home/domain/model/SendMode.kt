package com.rinx.artRINXapp.feature.home.domain.model

/**
 * Drives the art/curation "Send message" sheet so it matches the real conversation state with the
 * owner, instead of always framing the message as an invitation (handout §Chat — invitation only
 * applies to the FIRST message that creates the chatroom).
 */
enum class SendMode {
    INVITE,          // no chatroom yet → this message is the invitation (quota applies)
    ACTIVE,          // accepted, active chat → a normal message (no quota, no invite copy)
    PENDING,         // I invited; waiting for them to respond → can't send another yet
    BLOCKED_BY_ME,   // I blocked them
    BLOCKED_BY_THEM, // they blocked me
    RATE_LIMITED;    // sending not allowed right now (other reason)

    /** Only INVITE and ACTIVE permit sending; the rest render a disabled reason. */
    val canSend: Boolean get() = this == INVITE || this == ACTIVE

    companion object {
        /** Map the owner's public-profile conversation fields to a [SendMode] (block-reason priority). */
        fun resolve(
            canMessage: Boolean,
            chatroomId: String?,
            iBlocked: Boolean,
            theyBlocked: Boolean,
            blockReason: String?,
        ): SendMode = when {
            iBlocked -> BLOCKED_BY_ME
            theyBlocked -> BLOCKED_BY_THEM
            !canMessage && blockReason == "invite_pending" -> PENDING
            !canMessage && blockReason == "rate_limited" -> RATE_LIMITED
            !canMessage -> RATE_LIMITED
            chatroomId != null -> ACTIVE
            else -> INVITE
        }
    }
}
