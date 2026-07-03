package com.rinx.artRINXapp.feature.home.data.local

import com.rinx.artRINXapp.core.util.BlockedArtworkStore
import com.rinx.artRINXapp.core.util.BlockedUsersStore
import com.rinx.artRINXapp.feature.home.domain.model.ArtworkItem
import com.rinx.artRINXapp.feature.home.domain.model.CurationItem
import com.rinx.artRINXapp.feature.home.domain.model.ShoppablePost
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cached artwork-detail payload: the post, its "more like this" list, and whether the current user
 * owns it. [isOwn] is cached so a re-open renders the correct top-bar action (Edit/Delete vs Report)
 * immediately, with no Report→Edit flash while ownership re-resolves over the network.
 */
data class ArtDetailEntry(val post: ShoppablePost, val similar: List<ArtworkItem>, val isOwn: Boolean = false)

/** Cached curation-detail payload: the (post-reorder) curation, its "more like this" list, and
 *  whether the current user owns it (cached so a re-open shows Edit/Delete instantly, no delay). */
data class CurationDetailEntry(val curation: CurationItem, val more: List<CurationItem>, val isOwn: Boolean = false)

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
class DetailCache @Inject constructor(
    private val blockedStore: BlockedArtworkStore,
    private val blockedUsersStore: BlockedUsersStore,
) {

    private val art = lru<ArtDetailEntry>()
    private val cur = lru<CurationDetailEntry>()

    // ── Artwork ──────────────────────────────────────────────────────────────
    // Never re-serve a blocked artwork — OR one uploaded by a user I've blocked — from cache
    // (e.g. re-opening it after blocking that uploader from their profile). Art merely crediting a
    // blocked user as artist but uploaded by someone else stays (parity with the list filters).
    fun peekArtwork(id: Int): ArtDetailEntry? = synchronized(art) {
        val entry = art[id] ?: return@synchronized null
        if (blockedStore.isBlocked(id) || isOwnerBlocked(entry.post.ownerId)) {
            null
        } else {
            entry
        }
    }

    fun putArtwork(id: Int, post: ShoppablePost, similar: List<ArtworkItem>, isOwn: Boolean) =
        synchronized(art) { art[id] = ArtDetailEntry(post, similar, isOwn) }

    /** Write-through a like toggle onto the cached post (no-op if not cached). */
    fun updateArtworkLike(id: Int, isLiked: Boolean, likeCount: Int) = synchronized(art) {
        art[id]?.let { art[id] = it.copy(post = it.post.copy(isLiked = isLiked, likeCount = likeCount)) }
    }

    fun evictArtwork(id: Int) = synchronized(art) { art.remove(id); Unit }

    /** Drop every cached artwork uploaded by a now-blocked user. */
    fun evictByOwner(ownerId: Int) = synchronized(art) {
        art.entries.removeAll { it.value.post.ownerId == ownerId }
    }

    private fun isOwnerBlocked(vararg ids: Int?): Boolean =
        ids.any { it != null && blockedUsersStore.isBlocked(it) }

    // ── Curation ─────────────────────────────────────────────────────────────
    // Strip any blocked artwork from the cached curation deck before serving it.
    fun peekCuration(id: Int): CurationDetailEntry? = synchronized(cur) {
        cur[id]?.let { e ->
            e.copy(curation = e.curation.stripBlocked(), more = e.more.map { it.stripBlocked() })
        }
    }

    private fun CurationItem.stripBlocked(): CurationItem {
        if (artworkUrls.isEmpty()) return this
        val keptUrls = ArrayList<String>(artworkUrls.size)
        val keptIds = ArrayList<String>(artworkIds.size)
        artworkUrls.indices.forEach { i ->
            val artId = artworkIds.getOrNull(i)
            if (!blockedStore.isBlocked(artId)) {
                keptUrls += artworkUrls[i]
                keptIds += (artId ?: "")
            }
        }
        return copy(artworkUrls = keptUrls, artworkIds = keptIds)
    }

    fun putCuration(id: Int, curation: CurationItem, more: List<CurationItem>, isOwn: Boolean = false) =
        synchronized(cur) { cur[id] = CurationDetailEntry(curation, more, isOwn) }

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
