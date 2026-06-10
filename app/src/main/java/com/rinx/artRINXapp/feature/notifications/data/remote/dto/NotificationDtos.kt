package com.rinx.artRINXapp.feature.notifications.data.remote.dto

import com.google.gson.annotations.SerializedName

// ── Notifications (API §8) ──────────────────────────────────────────────────────
// camelCase nested fields (profileImageUrl/thumbnailUrl) are documented; snake_case
// alternates are accepted defensively (the backend has mixed casing elsewhere).

/** `GET /api/notifications` → data. */
data class NotificationListDto(
    @SerializedName("notifications") val notifications: List<NotificationDto>? = null,
)

/** A single notification (§8.3). Unknown `type` values are tolerated — we render `message`. */
data class NotificationDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName(value = "is_read", alternate = ["isRead"]) val isRead: Boolean? = null,
    @SerializedName(value = "timestamp", alternate = ["created_at", "createdAt"]) val timestamp: String? = null,
    @SerializedName("actor") val actor: NotificationActorDto? = null,
    @SerializedName("action") val action: String? = null,
    @SerializedName("target") val target: NotificationTargetDto? = null,
    // ── Event-only fields (null on every non-event type) ──────────────────────────
    @SerializedName(value = "organizer_name", alternate = ["organizerName"]) val organizerName: String? = null,
    @SerializedName(value = "organizer_handle", alternate = ["organizerHandle"]) val organizerHandle: String? = null,
    @SerializedName(value = "organizer_profile_url", alternate = ["organizerProfileUrl"]) val organizerProfileUrl: String? = null,
    @SerializedName(value = "event_id", alternate = ["eventId"]) val eventId: Long? = null,
    @SerializedName(value = "event_image_url", alternate = ["eventImageUrl"]) val eventImageUrl: String? = null,
)

data class NotificationActorDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName(value = "profileImageUrl", alternate = ["profile_image_url"]) val profileImageUrl: String? = null,
)

data class NotificationTargetDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName(value = "thumbnailUrl", alternate = ["thumbnail_url"]) val thumbnailUrl: String? = null,
)
