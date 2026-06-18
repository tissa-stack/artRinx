package com.rinx.artRINXapp.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure-logic coverage for [DeepLinkParser.fromPush] routing. Cases that depend on `android.net.Uri`
 * parsing (the `url` branch, e.g. `rinxart://chat/<id>`) are exercised via manual verification on a
 * device — `Uri.parse` returns defaults under the plain-JVM unit-test runtime.
 */
class DeepLinkParserTest {

    @Test
    fun `follow push routes to the follower profile via actor id`() {
        val result = DeepLinkParser.fromPush(
            route = null, url = null, kind = null, type = "follow", actorId = "42",
        )
        assertEquals(DeepLinkTarget.Route(NavRoutes.userProfile("42")), result)
    }

    @Test
    fun `route path resolves to art detail`() {
        val result = DeepLinkParser.fromPush(
            route = "/artworks/9", url = null, kind = null, type = "artwork_like",
        )
        assertEquals(DeepLinkTarget.Route(NavRoutes.artDetail("9")), result)
    }

    @Test
    fun `resource type and id resolve to curation detail`() {
        val result = DeepLinkParser.fromPush(
            route = null, url = null, kind = null, type = "curation_share",
            resourceType = "curation", resourceId = "7",
        )
        assertEquals(DeepLinkTarget.Route(NavRoutes.curationDetail("7")), result)
    }

    @Test
    fun `gallery enterprise notice is home only`() {
        val result = DeepLinkParser.fromPush(
            route = null, url = null, kind = "gallery_enterprise_notice", type = null,
        )
        assertEquals(DeepLinkTarget.HomeOnly, result)
    }

    @Test
    fun `unresolvable push returns null so the caller can fall back to Notifications`() {
        val result = DeepLinkParser.fromPush(
            route = null, url = null, kind = null, type = "artwork_like",
            resourceType = null, resourceId = null, actorId = null,
        )
        assertNull(result)
    }
}
