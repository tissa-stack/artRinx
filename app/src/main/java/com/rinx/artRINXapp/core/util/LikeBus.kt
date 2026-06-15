package com.rinx.artRINXapp.core.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide like reconciliation. Emits the ABSOLUTE post-toggle state (isLiked + likeCount) for an
 * artwork so any live screen showing that artwork (Home feed ↔ Art detail) converges instead of
 * disagreeing. Carrying absolute values (never deltas) makes receivers idempotent — re-applying the
 * same event, or an emitter receiving its own event, is a no-op, so there's no double-count or loop.
 *
 * Caches are written through separately (HomeRepository.updateCachedLike + DetailCache) so a screen
 * recreated/re-seeded later is also correct; this bus only patches already-live ViewModels.
 */
@Singleton
class LikeBus @Inject constructor() {
    data class Update(val artworkId: Int, val isLiked: Boolean, val likeCount: Int)

    private val _events = MutableSharedFlow<Update>(extraBufferCapacity = 16)
    val events: SharedFlow<Update> = _events.asSharedFlow()

    fun signal(artworkId: Int, isLiked: Boolean, likeCount: Int) {
        _events.tryEmit(Update(artworkId, isLiked, likeCount))
    }
}
