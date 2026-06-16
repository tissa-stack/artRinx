package com.rinx.artRINXapp.feature.home

import com.rinx.artRINXapp.feature.home.data.local.DetailCache
import com.rinx.artRINXapp.feature.home.domain.model.ArtworkItem
import com.rinx.artRINXapp.feature.home.domain.model.CurationItem
import com.rinx.artRINXapp.feature.home.domain.model.ShoppablePost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [DetailCache] is the SWR detail cache: round-trip storage, write-through like updates, eviction,
 * a bounded access-order LRU (cap = 20 per type), and full clear on logout.
 */
class DetailCacheTest {

    private val cache = DetailCache()

    private fun post(id: String, liked: Boolean = false, likes: Int = 0) = ShoppablePost(
        id = id, artistName = "Artist", artistHandle = "@artist", imageUrl = "", title = "Title",
        medium = "Oil", description = "Desc", likeCount = likes, isLiked = liked,
    )

    private fun art(id: String) = ArtworkItem(id = id, imageUrl = "", title = "T", artistName = "A")

    private fun curation(id: String, liked: Boolean = false, likes: Int = 0) = CurationItem(
        id = id, title = "C", curatorHandle = "@c", artworkUrls = emptyList(), likeCount = likes, isLiked = liked,
    )

    // ── Round trip ────────────────────────────────────────────────────────────

    @Test
    fun `put then peek returns the stored artwork entry`() {
        cache.putArtwork(1, post("1"), listOf(art("2"), art("3")), isOwn = true)
        val entry = cache.peekArtwork(1)
        assertNotNull(entry)
        assertEquals("1", entry!!.post.id)
        assertEquals(2, entry.similar.size)
        assertTrue(entry.isOwn)
    }

    @Test
    fun `peek of an absent id is null`() {
        assertNull(cache.peekArtwork(999))
        assertNull(cache.peekCuration(999))
    }

    // ── Write-through like ─────────────────────────────────────────────────────

    @Test
    fun `updateArtworkLike writes through onto the cached post`() {
        cache.putArtwork(5, post("5", liked = false, likes = 1), emptyList(), isOwn = false)
        cache.updateArtworkLike(5, isLiked = true, likeCount = 2)
        val entry = cache.peekArtwork(5)!!
        assertTrue(entry.post.isLiked)
        assertEquals(2, entry.post.likeCount)
    }

    @Test
    fun `updateArtworkLike is a no-op when the id is not cached`() {
        cache.updateArtworkLike(404, isLiked = true, likeCount = 9) // must not throw / create an entry
        assertNull(cache.peekArtwork(404))
    }

    @Test
    fun `updateCurationLike writes through onto the cached curation`() {
        cache.putCuration(7, curation("7", liked = false, likes = 0), emptyList())
        cache.updateCurationLike(7, isLiked = true, likeCount = 3)
        val entry = cache.peekCuration(7)!!
        assertTrue(entry.curation.isLiked)
        assertEquals(3, entry.curation.likeCount)
    }

    // ── Eviction & clear ────────────────────────────────────────────────────────

    @Test
    fun `evict removes a single entry`() {
        cache.putArtwork(1, post("1"), emptyList(), isOwn = false)
        cache.evictArtwork(1)
        assertNull(cache.peekArtwork(1))
    }

    @Test
    fun `clear wipes both artwork and curation entries`() {
        cache.putArtwork(1, post("1"), emptyList(), isOwn = false)
        cache.putCuration(1, curation("1"), emptyList())
        cache.clear()
        assertNull(cache.peekArtwork(1))
        assertNull(cache.peekCuration(1))
    }

    @Test
    fun `cache is a bounded access-order LRU that evicts the least-recently-used entry`() {
        // Fill to capacity (20).
        for (i in 1..20) cache.putArtwork(i, post(i.toString()), emptyList(), isOwn = false)
        // Touch id 1 so it becomes most-recently-used; the eldest is now id 2.
        assertNotNull(cache.peekArtwork(1))
        // Insert a 21st → exactly one eviction, and it's the LRU (id 2), not the just-touched id 1.
        cache.putArtwork(21, post("21"), emptyList(), isOwn = false)
        assertNull("LRU entry should have been evicted", cache.peekArtwork(2))
        assertNotNull("recently-touched entry should survive", cache.peekArtwork(1))
        assertNotNull(cache.peekArtwork(21))
    }

    @Test
    fun `artwork and curation caches are independent`() {
        cache.putArtwork(1, post("1"), emptyList(), isOwn = false)
        assertNull(cache.peekCuration(1))
        assertFalse(cache.peekArtwork(1) == null)
    }
}
