package com.rinx.artRINXapp.feature.notifications.data.repository

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.network.serverMessageOrNull
import com.rinx.artRINXapp.core.util.BlockedArtworkStore
import com.rinx.artRINXapp.core.util.BlockedUsersStore
import com.rinx.artRINXapp.feature.notifications.data.remote.NotificationsApiService
import com.rinx.artRINXapp.feature.notifications.data.remote.dto.NotificationDto
import com.rinx.artRINXapp.feature.notifications.domain.model.NotificationItem
import com.rinx.artRINXapp.feature.notifications.domain.model.NotificationKind
import com.rinx.artRINXapp.feature.notifications.domain.repository.NotificationsRepository
import retrofit2.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class NotificationsRepositoryImpl @Inject constructor(
    private val apiService: NotificationsApiService,
    private val blockedUsersStore: BlockedUsersStore,
    private val blockedArtworkStore: BlockedArtworkStore,
) : NotificationsRepository {

    override suspend fun getNotifications(): ApiResult<List<NotificationItem>> = safeCall {
        val response = apiService.getNotifications()
        if (response.isSuccessful) {
            // Drop rows for a blocked user's activity (they followed/liked/shared) or a blocked
            // artwork — parity with the feed/search/profile lists, which all filter through the
            // blocked stores. The stores are seeded app-wide (Home entry).
            ApiResult.Success(
                response.body()?.data?.notifications.orEmpty()
                    .mapNotNull { it.toItem() }
                    .filterNot { it.isHidden() },
            )
        } else {
            errorFor(response)
        }
    }

    /** True if this row belongs to a blocked user (actor/organizer) or a blocked artwork. */
    private fun NotificationItem.isHidden(): Boolean {
        if (isUserBlocked(actorId?.toInt(), organizerId?.toInt())) return true
        // Only artwork-kind rows carry an ARTWORK target id (curation rows carry a curation id).
        if ((kind == NotificationKind.ARTWORK_LIKE || kind == NotificationKind.ARTWORK_SHARE) &&
            blockedArtworkStore.isBlocked(targetId?.toString())
        ) {
            return true
        }
        return false
    }

    /** True if any of the given user ids belongs to a user I've blocked (nulls ignored). */
    private fun isUserBlocked(vararg ids: Int?): Boolean =
        ids.any { it != null && blockedUsersStore.isBlocked(it) }

    override suspend fun markRead(id: String): ApiResult<Unit> = safeCall {
        val nid = id.toLongOrNull() ?: return@safeCall ApiResult.Error.Validation("Invalid notification id")
        val response = apiService.markRead(nid)
        if (response.isSuccessful) ApiResult.Success(Unit) else errorFor(response)
    }

    // ── Mapping ──────────────────────────────────────────────────────────────────

    private fun NotificationDto.toItem(): NotificationItem? {
        val nid = id ?: return null
        val resolvedKind = NotificationKind.from(type)
        // Organizer id is parsed from the trailing path segment of organizer_profile_url
        // (e.g. "https://www.artrinx.com/profile/167" or "rinxart://profile/167"), falling back
        // to the actor id which carries the same organizer on event rows.
        val organizerId = parseTrailingId(organizerProfileUrl) ?: actor?.id
        return NotificationItem(
            id = nid.toString(),
            message = message.orEmpty(),
            timeAgo = relativeTime(timestamp),
            isRead = isRead ?: false,
            // Likes/comments carry a square artwork/curation thumbnail; follows/shares carry an
            // actor avatar. The UI prefers the thumbnail when present, else the avatar.
            thumbnailUrl = if (resolvedKind.isEvent) eventImageUrl else target?.thumbnailUrl,
            avatarUrl = actor?.profileImageUrl,
            actorName = actor?.name,
            type = type,
            kind = resolvedKind,
            actorId = actor?.id,
            targetId = target?.id,
            targetType = target?.type,
            targetTitle = target?.title,
            targetName = target?.name,
            eventId = eventId,
            eventImageUrl = eventImageUrl,
            organizerId = organizerId,
            organizerName = organizerName,
            organizerHandle = organizerHandle,
        )
    }

    /** Trailing numeric path segment of a profile url, e.g. ".../profile/167" → 167. */
    private fun parseTrailingId(url: String?): Long? {
        if (url.isNullOrBlank()) return null
        return url.trim().trimEnd('/').substringAfterLast('/').toLongOrNull()
    }

    // ── Relative time ──────────────────────────────────────────────────────────────

    /** ISO 8601 → "2h ago" / "3d ago" / "just now". Returns "" if unparseable. */
    private fun relativeTime(iso: String?): String {
        val date = parseIso(iso) ?: return ""
        val diffMs = System.currentTimeMillis() - date.time
        if (diffMs < MINUTE_MS) return "just now"
        val minutes = diffMs / MINUTE_MS
        val hours = diffMs / HOUR_MS
        val days = diffMs / DAY_MS
        return when {
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            days < 30 -> "${days / 7}w ago"
            days < 365 -> "${days / 30}mo ago"
            else -> "${days / 365}y ago"
        }
    }

    /**
     * Server stamps may carry 6-digit microseconds and/or no timezone (e.g.
     * "2026-05-29T13:39:50.489176"). `SimpleDateFormat.SSS` would misread microseconds as
     * milliseconds, so take just the seconds-precision prefix and treat it as UTC.
     */
    private fun parseIso(iso: String?): Date? {
        if (iso.isNullOrBlank()) return null
        val base = iso.trim()
        val truncated = if (base.length >= 19) base.substring(0, 19) else return null
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(truncated)
        } catch (_: Exception) {
            null
        }
    }

    // ── Error handling (mirrors MessagesRepositoryImpl) ────────────────────────────

    private inline fun <T> safeCall(block: () -> ApiResult<T>): ApiResult<T> = try {
        block()
    } catch (e: IOException) {
        ApiResult.Error.Network(e)
    } catch (e: Exception) {
        ApiResult.Error.Unknown(e)
    }

    private fun errorFor(response: Response<*>): ApiResult.Error {
        val body = runCatching { response.errorBody()?.string() }.getOrNull()
        return when (response.code()) {
            403 -> ApiResult.Error.Blocked(body?.take(300) ?: "Action not allowed")
            404 -> ApiResult.Error.NotFound("Not found")
            in 400..499 -> ApiResult.Error.Validation(
                serverMessageOrNull(body) ?: "Request failed (${response.code()})",
            )
            in 500..599 -> ApiResult.Error.Server(response.code())
            else -> ApiResult.Error.Unknown(RuntimeException("HTTP ${response.code()}"))
        }
    }

    private companion object {
        const val MINUTE_MS = 60_000L
        const val HOUR_MS = 3_600_000L
        const val DAY_MS = 86_400_000L
    }
}
