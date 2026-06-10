package com.rinx.artRINXapp.feature.notifications

import com.rinx.artRINXapp.feature.notifications.domain.model.NotificationItem
import com.rinx.artRINXapp.feature.notifications.domain.model.NotificationKind
import com.rinx.artRINXapp.feature.notifications.presentation.notifications.routeNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationRoutingTest {

    private class Captured {
        var art: Long? = null
        var curation: Long? = null
        var profile: Long? = null
    }

    private fun route(item: NotificationItem): Captured {
        val c = Captured()
        routeNotification(item, { c.art = it }, { c.curation = it }, { c.profile = it })
        return c
    }

    private fun item(kind: NotificationKind, targetId: Long? = 10L, actorId: Long? = 20L) =
        NotificationItem(
            id = "1", message = "m", timeAgo = "1h ago",
            kind = kind, targetId = targetId, actorId = actorId,
        )

    @Test
    fun `artwork like and share route to art detail by target id`() {
        assertEquals(10L, route(item(NotificationKind.ARTWORK_LIKE)).art)
        assertEquals(10L, route(item(NotificationKind.ARTWORK_SHARE)).art)
    }

    @Test
    fun `curation like and share route to curation detail by target id`() {
        assertEquals(10L, route(item(NotificationKind.CURATION_LIKE)).curation)
        assertEquals(10L, route(item(NotificationKind.CURATION_SHARE)).curation)
    }

    @Test
    fun `follow and profile share route to actor profile`() {
        assertEquals(20L, route(item(NotificationKind.FOLLOW)).profile)
        assertEquals(20L, route(item(NotificationKind.PROFILE_SHARE)).profile)
    }

    @Test
    fun `unknown kind is a no-op`() {
        val c = route(item(NotificationKind.UNKNOWN))
        assertNull(c.art); assertNull(c.curation); assertNull(c.profile)
    }

    @Test
    fun `missing target id makes content rows non-tappable`() {
        assertNull(route(item(NotificationKind.ARTWORK_LIKE, targetId = null)).art)
        assertNull(route(item(NotificationKind.CURATION_LIKE, targetId = null)).curation)
    }

    @Test
    fun `event kinds are not handled by row routing`() {
        // Event rows route via their own row (handle vs body), not this helper.
        val c = route(item(NotificationKind.EVENT_REMINDER_3H))
        assertNull(c.art); assertNull(c.curation); assertNull(c.profile)
    }
}
