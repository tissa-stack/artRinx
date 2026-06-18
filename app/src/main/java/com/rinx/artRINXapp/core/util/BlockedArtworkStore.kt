package com.rinx.artRINXapp.core.util

import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session-wide record of artwork ids the user has blocked. The data layer filters every artwork list
 * (feed, shop, recommendations, search, profile grids, curation decks — cached AND freshly fetched)
 * through this, so a blocked artwork can never reappear on any screen — including back-navigation or
 * a re-seed from cache — even before the backend's own filtering catches up. Pairs with
 * [BlockedArtworkBus], which removes it from already-on-screen lists instantly.
 *
 * In-memory for the session; cleared on logout/account-delete (see LocalDataCleaner). The backend
 * excludes blocked art on subsequent fetches, so it need not persist across restarts.
 */
@Singleton
class BlockedArtworkStore @Inject constructor() {

    private val ids = Collections.synchronizedSet(mutableSetOf<Int>())

    fun add(id: Int) {
        ids.add(id)
    }

    fun isBlocked(id: Int): Boolean = ids.contains(id)

    /** Convenience for string ids (domain models carry artwork ids as strings). */
    fun isBlocked(id: String?): Boolean {
        val parsed = id?.toIntOrNull() ?: return false
        return ids.contains(parsed)
    }

    fun clear() {
        ids.clear()
    }
}
