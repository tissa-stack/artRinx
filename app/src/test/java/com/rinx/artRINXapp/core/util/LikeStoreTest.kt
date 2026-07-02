package com.rinx.artRINXapp.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Unit guards for [LikeStore]: set/read/clear semantics and artwork↔curation namespace isolation. */
class LikeStoreTest {

    @Test
    fun `set and read the intended like state`() {
        val store = LikeStore()
        store.setArtwork(1, true)
        store.setArtwork(2, false)
        assertEquals(true, store.artwork(1))
        assertEquals(false, store.artwork(2))
        assertNull(store.artwork(3)) // untouched → no override
    }

    @Test
    fun `artwork and curation ids are separate namespaces`() {
        val store = LikeStore()
        store.setArtwork(5, true)
        store.setCuration(5, false)
        assertEquals(true, store.artwork(5))
        assertEquals(false, store.curation(5))
    }

    @Test
    fun `clearArtwork drops only that entry`() {
        val store = LikeStore()
        store.setArtwork(1, true)
        store.setArtwork(2, true)
        store.clearArtwork(1)
        assertNull(store.artwork(1))
        assertEquals(true, store.artwork(2))
    }

    @Test
    fun `clear wipes both namespaces`() {
        val store = LikeStore()
        store.setArtwork(1, true)
        store.setCuration(2, true)
        store.clear()
        assertNull(store.artwork(1))
        assertNull(store.curation(2))
    }
}
