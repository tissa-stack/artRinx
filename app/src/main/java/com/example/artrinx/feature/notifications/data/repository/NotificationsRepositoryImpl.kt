package com.example.artrinx.feature.notifications.data.repository

import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.notifications.data.remote.NotificationsApiService
import com.example.artrinx.feature.notifications.data.remote.dto.NotificationDto
import com.example.artrinx.feature.notifications.domain.model.NotificationItem
import com.example.artrinx.feature.notifications.domain.repository.NotificationsRepository
import retrofit2.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class NotificationsRepositoryImpl @Inject constructor(
    private val apiService: NotificationsApiService,
) : NotificationsRepository {

    override suspend fun getNotifications(): ApiResult<List<NotificationItem>> = safeCall {
        val response = apiService.getNotifications()
        if (response.isSuccessful) {
            ApiResult.Success(response.body()?.data?.notifications.orEmpty().mapNotNull { it.toItem() })
        } else {
            errorFor(response)
        }
    }

    override suspend fun markRead(id: String): ApiResult<Unit> = safeCall {
        val nid = id.toLongOrNull() ?: return@safeCall ApiResult.Error.Validation("Invalid notification id")
        val response = apiService.markRead(nid)
        if (response.isSuccessful) ApiResult.Success(Unit) else errorFor(response)
    }

    // ── Mapping ──────────────────────────────────────────────────────────────────

    private fun NotificationDto.toItem(): NotificationItem? {
        val nid = id ?: return null
        return NotificationItem(
            id = nid.toString(),
            message = message.orEmpty(),
            timeAgo = relativeTime(timestamp),
            isRead = isRead ?: false,
            // Likes/comments carry a square artwork/curation thumbnail; follows/shares carry an
            // actor avatar. The UI prefers the thumbnail when present, else the avatar.
            thumbnailUrl = target?.thumbnailUrl,
            avatarUrl = actor?.profileImageUrl,
            type = type,
        )
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

    private fun errorFor(response: Response<*>): ApiResult.Error = when (response.code()) {
        403 -> ApiResult.Error.Blocked(response.errorBody()?.string()?.take(300) ?: "Action not allowed")
        404 -> ApiResult.Error.NotFound("Not found")
        in 400..499 -> ApiResult.Error.Validation("Request failed (${response.code()})")
        in 500..599 -> ApiResult.Error.Server(response.code())
        else -> ApiResult.Error.Unknown(RuntimeException("HTTP ${response.code()}"))
    }

    private companion object {
        const val MINUTE_MS = 60_000L
        const val HOUR_MS = 3_600_000L
        const val DAY_MS = 86_400_000L
    }
}
