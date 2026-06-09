package com.rinx.artRINXapp.feature.home.data.local

import com.rinx.artRINXapp.feature.home.domain.model.ArtworkItem
import com.rinx.artRINXapp.feature.home.domain.model.CurationItem
import com.rinx.artRINXapp.feature.home.domain.model.ShoppablePost
import javax.inject.Inject
import javax.inject.Singleton

/** Cached artwork-detail payload: the post plus its "more like this" list. */
data class ArtDetailEntry(val post: ShoppablePost, val similar: List<ArtworkItem>)

/** Cached curation-detail payload: the (post-reorder) curation plus its "more like this" list. */
data class CurationDetailEntry(val curation: CurationItem, val more: List<CurationItem>)

/**
 * In-memory, app-lifetime cache of the last-viewed artwork/curation details so reopening a screen
 * renders instantly (no spinner) — stale-while-revalidate: the ViewModel seeds from here, then
 * silently re-fetches. Likes are written through ([updateArtworkLike]/[updateCurationLike]) so the
 * user's latest interaction always survives navigation.
 *
 * Bounded LRU (access-order [LinkedHashMap], [CAP] per type) so memory can't grow unbounded.
 * Cleared on logout/account-delete (see core/auth/LocalDataCleaner).
 */
@Singleton
class DetailCache @Inject constructor() {

    private val art = lru<ArtDetailEntry>()
    private val cur = lru<CurationDetailEntry>()

    // ── Artwork ──────────────────────────────────────────────────────────────
    fun peekArtwork(id: Int): ArtDetailEntry? = synchronized(art) { art[id] }

    fun putArtwork(id: Int, post: ShoppablePost, similar: List<ArtworkItem>) =
        synchronized(art) { art[id] = ArtDetailEntry(post, similar) }

    /** Write-through a like toggle onto the cached post (no-op if not cached). */
    fun updateArtworkLike(id: Int, isLiked: Boolean, likeCount: Int) = synchronized(art) {
        art[id]?.let { art[id] = it.copy(post = it.post.copy(isLiked = isLiked, likeCount = likeCount)) }
    }

    fun evictArtwork(id: Int) = synchronized(art) { art.remove(id); Unit }

    // ── Curation ─────────────────────────────────────────────────────────────
    fun peekCuration(id: Int): CurationDetailEntry? = synchronized(cur) { cur[id] }

    fun putCuration(id: Int, curation: CurationItem, more: List<CurationItem>) =
        synchronized(cur) { cur[id] = CurationDetailEntry(curation, more) }

    fun updateCurationLike(id: Int, isLiked: Boolean, likeCount: Int) = synchronized(cur) {
        cur[id]?.let { cur[id] = it.copy(curation = it.curation.copy(isLiked = isLiked, likeCount = likeCount)) }
    }

    fun evictCuration(id: Int) = synchronized(cur) { cur.remove(id); Unit }

    /** Wipe everything — call on logout / account deletion. */
    fun clear() {
        synchronized(art) { art.clear() }
        synchronized(cur) { cur.clear() }
    }

    private fun <V> lru(): MutableMap<Int, V> =
        object : LinkedHashMap<Int, V>(16, 0.75f, /* accessOrder = */ true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, V>?): Boolean = size > CAP
        }

    private companion object {
        const val CAP = 20
    }
}
