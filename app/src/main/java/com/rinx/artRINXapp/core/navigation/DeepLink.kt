package com.rinx.artRINXapp.core.navigation

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** A resolved deep-link / push-tap destination. */
sealed interface DeepLinkTarget {
    /** rinxart://invite/<CODE> or https://(www.)artrinx.com/invite/<CODE> — pre-fills the invite field. */
    data class Invite(val code: String) : DeepLinkTarget

    /** A resolved in-app nav route (chat / art / curation / profile). */
    data class Route(val navRoute: String) : DeepLinkTarget

    /** rinxart://event/<ID> (or universal link / event_* push) — opens the event popup on the Notifications tab. */
    data class Event(val id: String) : DeepLinkTarget

    /** gallery_enterprise_notice: open the app to Home only, no navigation/CTA (anti-steering). */
    data object HomeOnly : DeepLinkTarget

    /** A notification push that couldn't be resolved to a specific screen → open the Notifications tab. */
    data object Notifications : DeepLinkTarget
}

/**
 * Resolves incoming intents (custom-scheme + universal links) and push-tap extras into a
 * [DeepLinkTarget]. Mirrors the iOS DeepLinkParser referenced in the handout.
 */
object DeepLinkParser {

    /** Parse an ACTION_VIEW data Uri (invite deep links + universal content links). */
    fun fromViewUri(uri: Uri?): DeepLinkTarget? {
        uri ?: return null
        // Custom scheme: rinxart://<type>/<id> (e.g. rinxart://event/175, rinxart://invite/<CODE>).
        // The type lives in the host and the id is the first path segment.
        if (uri.scheme.equals("rinxart", true)) {
            val type = uri.host?.lowercase()
            val first = uri.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }
            if (type == "invite") return first?.let { DeepLinkTarget.Invite(it) }
            if (type != null && first != null) return fromPath("/$type/$first")
            return null
        }
        // https://(www.)artrinx.com/invite/<CODE>
        val host = uri.host?.lowercase()
        if (host == "artrinx.com" || host == "www.artrinx.com") {
            val segs = uri.pathSegments
            val i = segs.indexOf("invite")
            if (i >= 0 && i + 1 < segs.size && segs[i + 1].isNotBlank()) {
                return DeepLinkTarget.Invite(segs[i + 1])
            }
        }
        return fromPath(uri.path)
    }

    /**
     * Parse push-tap extras: `kind` (special-case), then `route`/`url`. As a fallback (no url
     * provided), an `event_*` [type] with an [eventId] constructs the event destination locally —
     * mirrors the iOS handler that builds `rinxart://event/{event_id}` when the payload omits `url`.
     */
    fun fromPush(
        route: String?,
        url: String?,
        kind: String?,
        type: String? = null,
        eventId: String? = null,
        resourceType: String? = null,
        resourceId: String? = null,
        actorId: String? = null,
    ): DeepLinkTarget? {
        if (kind.equals("gallery_enterprise_notice", true)) return DeepLinkTarget.HomeOnly
        // `route` is a plain path (e.g. "/artworks/362") → match directly.
        route?.takeIf { it.isNotBlank() }?.let { fromPath(it)?.let { t -> return t } }
        // `url` may be a full custom-scheme or universal URL. Parse it via [fromViewUri] (NOT just
        // .path) so `rinxart://chat/<id>` — where the TYPE is the HOST, not a path segment — resolves
        // correctly; a bare `Uri.parse(url).path` would drop "chat" and lose the destination.
        url?.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching { Uri.parse(raw) }.getOrNull()?.let { uri ->
                fromViewUri(uri)?.let { t -> return t }
            }
        }
        // Fallback: engagement pushes (like/follow/share) may carry `resource_type` + `resource_id`
        // instead of a route/url (e.g. resource_type="artwork", resource_id="362").
        if (!resourceType.isNullOrBlank() && !resourceId.isNullOrBlank()) {
            fromPath("/$resourceType/$resourceId")?.let { return it }
        }
        // A follow push usually carries no content resource — just the follower (`actor_id`); route
        // to that profile, mirroring the in-app "X started following you" → actor profile behavior.
        if (type.equals("follow", true) && !actorId.isNullOrBlank()) {
            return DeepLinkTarget.Route(NavRoutes.userProfile(actorId))
        }
        if (type?.startsWith("event_", ignoreCase = true) == true && !eventId.isNullOrBlank()) {
            return DeepLinkTarget.Event(eventId)
        }
        return null
    }

    private fun fromPath(path: String?): DeepLinkTarget? {
        val segs = path?.trim('/')?.split('/')?.filter { it.isNotBlank() } ?: return null
        if (segs.size < 2) return null
        val id = segs[1]
        return when (segs[0].lowercase()) {
            "chat" -> DeepLinkTarget.Route(NavRoutes.chat(id))
            "curations", "curation" -> DeepLinkTarget.Route(NavRoutes.curationDetail(id))
            "artworks", "artwork", "art" -> DeepLinkTarget.Route(NavRoutes.artDetail(id))
            "users", "user", "profile" -> DeepLinkTarget.Route(NavRoutes.userProfile(id))
            "events", "event" -> DeepLinkTarget.Event(id)
            else -> null
        }
    }
}

/**
 * App-scoped holder for a pending deep-link target. [MainActivity] posts; the nav graph consumes
 * [Route]/[HomeOnly] navigations and the invite screen consumes a pending [Invite] code.
 */
@Singleton
class DeepLinkRouter @Inject constructor() {
    private val _target = MutableStateFlow<DeepLinkTarget?>(null)
    val target: StateFlow<DeepLinkTarget?> = _target.asStateFlow()

    /**
     * Pending event id parked when an event deep-link/push is resolved. The nav graph switches to
     * the Notifications tab and posts the id here; [NotificationsViewModel] observes it, fetches the
     * event, opens the popup, and clears it via [consumeEventId].
     */
    private val _pendingEventId = MutableStateFlow<String?>(null)
    val pendingEventId: StateFlow<String?> = _pendingEventId.asStateFlow()

    fun post(target: DeepLinkTarget?) {
        if (target != null) _target.value = target
    }

    fun postEventId(id: String) {
        _pendingEventId.value = id
    }

    /** Pull + clear a pending invite code (called by the invite screen on first composition). */
    fun consumeInviteCode(): String? =
        (_target.value as? DeepLinkTarget.Invite)?.code?.also { _target.value = null }

    /** Pull + clear a pending event id (called by the notifications VM once it opens the popup). */
    fun consumeEventId(): String? = _pendingEventId.value?.also { _pendingEventId.value = null }

    fun consume() {
        _target.value = null
    }
}
