package com.rinx.artRINXapp.core.util

import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session-wide record of the user's OWN like intent per artwork/curation. The data layer overlays
 * every like-bearing list (Home feeds + artwork/curation detail — cached AND freshly fetched)
 * through this, so a like/unlike the user just made survives a silent revalidate, pull-to-refresh
 * or ViewModel recreation — even before the backend's own read reflects it. Pairs with [LikeBus],
 * which patches already-on-screen lists instantly.
 *
 * Stores the intended `isLiked` boolean only; the visible like *count* is derived by the overlay
 * (server/cached base ± the user's own contribution) so it never drifts or double-counts.
 *
 * Self-clearing: once a fresh network read AGREES with the stored intent the entry is dropped, so
 * the override is only transient — after the server catches up it becomes authoritative again (a
 * later change made on another device is then respected). Artwork and curation ids live in separate
 * namespaces. In-memory for the session; cleared on logout/account-delete (see LocalDataCleaner).
 */
@Singleton
class LikeStore @Inject constructor() {

    private val artwork = Collections.synchronizedMap(mutableMapOf<Int, Boolean>())
    private val curation = Collections.synchronizedMap(mutableMapOf<Int, Boolean>())

    fun setArtwork(id: Int, liked: Boolean) { artwork[id] = liked }
    fun artwork(id: Int): Boolean? = artwork[id]
    fun clearArtwork(id: Int) { artwork.remove(id) }

    fun setCuration(id: Int, liked: Boolean) { curation[id] = liked }
    fun curation(id: Int): Boolean? = curation[id]
    fun clearCuration(id: Int) { curation.remove(id) }

    fun clear() {
        artwork.clear()
        curation.clear()
    }
}
