package com.example.artrinx.core.network

import com.google.gson.JsonObject

/** WebSocket connection lifecycle (API §12.3 — "connected" only flips on the `ready` frame). */
enum class WsConnectionState { DISCONNECTED, CONNECTING, CONNECTED }

/**
 * Decoded server → client WebSocket events (API §12.3). The `chat`/`notification` payloads are kept
 * as raw [JsonObject] so this core type stays free of feature DTOs — the consuming feature parses
 * them with the same DTO + mapper it uses for REST. Edit/delete/read carry small, stable fields.
 */
sealed interface ChatEvent {
    /** `chat` — a new message arrived. `data` is the full message object (§7.4). */
    data class NewMessage(val data: JsonObject) : ChatEvent

    /** `chat_edit` — the other party edited a message. */
    data class Edit(
        val messageId: String,
        val chatroomId: String?,
        val text: String?,
        val isEdited: Boolean,
        val editedAt: String?,
    ) : ChatEvent

    /** `chat_delete` — the other party deleted a message. */
    data class Delete(
        val messageId: String,
        val chatroomId: String?,
    ) : ChatEvent

    /** `chat_read` — a message you sent was marked read. */
    data class Read(
        val messageId: String,
        val chatroomId: String?,
        val readerId: Int?,
    ) : ChatEvent

    /** `notification` — a new notification (REST shape §8.3). */
    data class IncomingNotification(val data: JsonObject) : ChatEvent
}
