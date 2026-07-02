package com.rinx.artRINXapp.core.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide like reconciliation. Emits the ABSOLUTE post-toggle state (isLiked + likeCount) for an
 * artwork or curation so any live screen showing it (Home feed ↔ Art/Curation detail) converges
 * instead of disagreeing. Carrying absolute values (never deltas) makes receivers idempotent —
 * re-applying the same event, or an emitter receiving its own event, is a no-op, so there's no
 * double-count or loop.
 *
 * Artworks and curations are separate id namespaces, so they use separate streams. Caches are
 * written through separately ([LikeStore] + HomeRepository.updateCachedLike + DetailCache) so a
 * screen recreated/re-seeded later is also correct; this bus only patches already-live ViewModels.
 */
@Singleton
class LikeBus @Inject constructor() {
    data class Update(val id: Int, val isLiked: Boolean, val likeCount: Int)

    private val _events = MutableSharedFlow<Update>(extraBufferCapacity = 16)
    val events: SharedFlow<Update> = _events.asSharedFlow()

    private val _curationEvents = MutableSharedFlow<Update>(extraBufferCapacity = 16)
    val curationEvents: SharedFlow<Update> = _curationEvents.asSharedFlow()

    fun signal(artworkId: Int, isLiked: Boolean, likeCount: Int) {
        _events.tryEmit(Update(artworkId, isLiked, likeCount))
    }

    fun signalCuration(curationId: Int, isLiked: Boolean, likeCount: Int) {
        _curationEvents.tryEmit(Update(curationId, isLiked, likeCount))
    }
}
