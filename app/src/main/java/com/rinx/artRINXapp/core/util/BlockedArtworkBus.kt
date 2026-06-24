package com.rinx.artRINXapp.core.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide "this artwork was blocked" signal. Emits a blocked artwork id so any live screen showing
 * it (home feed, search results, profile grids, curation card stacks) can drop it from its in-memory
 * list immediately — the user shouldn't have to refresh for blocked art to disappear.
 *
 * Idempotent by construction: receivers just filter the id out of their lists, so re-applying the
 * same event (or an emitter seeing its own) is a no-op. The detail cache is evicted separately so a
 * later re-open doesn't resurrect it.
 */
@Singleton
class BlockedArtworkBus @Inject constructor() {

    private val _events = MutableSharedFlow<Int>(extraBufferCapacity = 16)
    val events: SharedFlow<Int> = _events.asSharedFlow()

    /** Counterpart "this artwork was unblocked" signal. The store is cleared of the id separately,
     *  so receivers just re-fetch their current content and the art reappears in server order. */
    private val _unblocked = MutableSharedFlow<Int>(extraBufferCapacity = 16)
    val unblocked: SharedFlow<Int> = _unblocked.asSharedFlow()

    fun signal(artworkId: Int) {
        _events.tryEmit(artworkId)
    }

    fun signalUnblock(artworkId: Int) {
        _unblocked.tryEmit(artworkId)
    }
}
