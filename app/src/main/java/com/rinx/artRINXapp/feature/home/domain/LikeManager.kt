package com.rinx.artRINXapp.feature.home.domain

import com.rinx.artRINXapp.core.di.ApplicationScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.offline.LiveMutationQueue
import com.rinx.artRINXapp.core.util.LikeBus
import com.rinx.artRINXapp.core.util.LikeStore
import com.rinx.artRINXapp.core.util.ProfileRefreshBus
import com.rinx.artRINXapp.feature.home.data.local.DetailCache
import com.rinx.artRINXapp.feature.home.domain.repository.HomeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single owner of like/unlike toggles for artworks and curations. Two properties the per-screen
 * ViewModels can't provide on their own:
 *
 * 1. **Survives navigation.** The network call runs on the process-lifetime [scope] (not the
 *    caller's viewModelScope), so leaving the screen "soon" after tapping can't cancel the request
 *    or its outcome handling — otherwise the server records the like but the client never resolves
 *    it, and the two drift apart (the "heart empty but already liked" stuck state).
 *
 * 2. **Converges to server truth.** The like/unlike endpoints reject a redundant op with a 4xx
 *    (e.g. 400 "You have already liked this artwork") → [ApiResult.Error.Validation]/Conflict/
 *    NotFound. That means the desired end-state is ALREADY true server-side, so it's treated as
 *    success (kept), never reverted. Only a genuine 5xx/unknown failure reverts; a network failure
 *    keeps the optimistic state and queues an offline replay.
 *
 * The optimistic durable state (LikeStore override + caches + [LikeBus]) is applied synchronously so
 * every live/re-seeded/re-fetched surface is correct immediately; live ViewModels (Home feeds, the
 * detail screens) observe [LikeBus] to pick up the async confirm/revert.
 */
@Singleton
class LikeManager @Inject constructor(
    private val repository: HomeRepository,
    private val likeStore: LikeStore,
    private val likeBus: LikeBus,
    private val detailCache: DetailCache,
    private val liveMutationQueue: LiveMutationQueue,
    private val profileRefreshBus: ProfileRefreshBus,
    @param:ApplicationScope private val scope: CoroutineScope,
) {

    /** Toggle an artwork like. [likeCount] is the optimistic post-toggle count from the caller. */
    fun toggleArtwork(id: Int, like: Boolean, likeCount: Int) {
        // Durable optimistic state — correct across navigation, re-seed and re-fetch.
        applyArtwork(id, like, likeCount)
        scope.launch {
            when (if (like) repository.likeArtwork(id) else repository.unlikeArtwork(id)) {
                // Server confirmed, OR the op was redundant (already in the desired state per the
                // 4xx it returns) → the desired state holds. Drop the override; server is authoritative.
                is ApiResult.Success,
                is ApiResult.Error.Validation,
                is ApiResult.Error.Conflict,
                is ApiResult.Error.NotFound,
                -> {
                    likeStore.clearArtwork(id)
                    profileRefreshBus.signal() // Profile "Liked" tab membership changed.
                }
                // Offline → keep the optimistic state and replay on reconnect.
                is ApiResult.Error.Network -> liveMutationQueue.enqueue("artwork", id, like)
                // Genuine transient failure (5xx / unknown): the change did NOT take → revert.
                is ApiResult.Error.Server,
                is ApiResult.Error.Unknown,
                is ApiResult.Error.RateLimited,
                is ApiResult.Error.Blocked,
                -> {
                    // Revert the optimistic change everywhere, then DROP the override (clear last, since
                    // applyArtwork re-sets it) so the next fetch shows server truth (5xx is ambiguous).
                    applyArtwork(id, !like, (likeCount + if (like) -1 else 1).coerceAtLeast(0))
                    likeStore.clearArtwork(id)
                }
            }
        }
    }

    /** Toggle a curation like. [likeCount] is the optimistic post-toggle count from the caller. */
    fun toggleCuration(id: Int, like: Boolean, likeCount: Int) {
        applyCuration(id, like, likeCount)
        scope.launch {
            when (if (like) repository.likeCuration(id) else repository.unlikeCuration(id)) {
                is ApiResult.Success,
                is ApiResult.Error.Validation,
                is ApiResult.Error.Conflict,
                is ApiResult.Error.NotFound,
                -> likeStore.clearCuration(id)
                is ApiResult.Error.Network -> liveMutationQueue.enqueue("curation", id, like)
                is ApiResult.Error.Server,
                is ApiResult.Error.Unknown,
                is ApiResult.Error.RateLimited,
                is ApiResult.Error.Blocked,
                -> {
                    applyCuration(id, !like, (likeCount + if (like) -1 else 1).coerceAtLeast(0))
                    likeStore.clearCuration(id)
                }
            }
        }
    }

    private fun applyArtwork(id: Int, like: Boolean, likeCount: Int) {
        likeStore.setArtwork(id, like)
        repository.updateCachedLike(id, like, likeCount)
        detailCache.updateArtworkLike(id, like, likeCount)
        likeBus.signal(id, like, likeCount)
    }

    private fun applyCuration(id: Int, like: Boolean, likeCount: Int) {
        likeStore.setCuration(id, like)
        detailCache.updateCurationLike(id, like, likeCount)
        likeBus.signalCuration(id, like, likeCount)
    }
}
