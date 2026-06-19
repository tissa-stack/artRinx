package com.rinx.artRINXapp.core.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide "I just blocked this user" signal. Emits the blocked user id so any live screen showing
 * that user's content (home feed, search results, profile grids, curation card stacks) can drop it
 * from its in-memory list immediately — the user shouldn't have to refresh for a blocked person's
 * art to disappear. The backend already excludes them on the next fetch; this only removes the wait.
 *
 * The sibling of [BlockedArtworkBus]: same idempotent-by-construction contract — receivers just
 * filter the owner id out of their lists, so re-applying the same event (or an emitter seeing its
 * own) is a no-op. The persistent record lives in [BlockedUsersStore], which the data layer also
 * filters cached/SWR reads through so a re-seed never resurfaces the blocked user's content.
 */
@Singleton
class BlockedUserBus @Inject constructor() {

    private val _events = MutableSharedFlow<Int>(extraBufferCapacity = 16)
    val events: SharedFlow<Int> = _events.asSharedFlow()

    fun signal(userId: Int) {
        _events.tryEmit(userId)
    }
}
