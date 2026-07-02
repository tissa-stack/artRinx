package com.rinx.artRINXapp.feature.home

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.offline.LiveMutationQueue
import com.rinx.artRINXapp.core.util.LikeBus
import com.rinx.artRINXapp.core.util.LikeStore
import com.rinx.artRINXapp.core.util.ProfileRefreshBus
import com.rinx.artRINXapp.feature.home.data.local.DetailCache
import com.rinx.artRINXapp.feature.home.domain.LikeManager
import com.rinx.artRINXapp.feature.home.domain.repository.HomeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Guards for [LikeManager] — the two properties that fix the "heart empty but already liked, can't
 * like again" stuck state: converge to server truth (never revert a redundant op) and queue offline.
 */
class LikeManagerTest {

    private val repo = mockk<HomeRepository>(relaxed = true)
    private val store = LikeStore()
    private val bus = LikeBus()
    private val detail = mockk<DetailCache>(relaxed = true)
    private val queue = mockk<LiveMutationQueue>(relaxed = true)
    private val refresh = mockk<ProfileRefreshBus>(relaxed = true)

    @Test
    fun `like success clears the override and refreshes profile`() = runTest {
        coEvery { repo.likeArtwork(1) } returns ApiResult.Success(Unit)
        val mgr = LikeManager(repo, store, bus, detail, queue, refresh, this)

        mgr.toggleArtwork(1, like = true, likeCount = 11)
        advanceUntilIdle()

        assertNull(store.artwork(1))                 // confirmed → override dropped
        verify(exactly = 1) { detail.updateArtworkLike(1, true, 11) } // applied once, never reverted
        verify(exactly = 0) { detail.updateArtworkLike(1, false, any()) }
        verify { refresh.signal() }
    }

    @Test
    fun `already-liked 400 is treated as success, not reverted`() = runTest {
        // The live duplicate-like response: HTTP 400 → ApiResult.Error.Validation.
        coEvery { repo.likeArtwork(1) } returns ApiResult.Error.Validation("You have already liked this artwork.")
        val mgr = LikeManager(repo, store, bus, detail, queue, refresh, this)

        mgr.toggleArtwork(1, like = true, likeCount = 11)
        advanceUntilIdle()

        assertNull(store.artwork(1))                                   // override dropped (server agrees)
        verify(exactly = 1) { detail.updateArtworkLike(1, true, 11) }  // stays liked
        verify(exactly = 0) { detail.updateArtworkLike(1, false, any()) } // NEVER bounced to unliked
    }

    @Test
    fun `network failure keeps optimistic state and enqueues for replay`() = runTest {
        coEvery { repo.likeArtwork(1) } returns ApiResult.Error.Network(RuntimeException("offline"))
        val mgr = LikeManager(repo, store, bus, detail, queue, refresh, this)

        mgr.toggleArtwork(1, like = true, likeCount = 11)
        advanceUntilIdle()

        assertEquals(true, store.artwork(1))           // override KEPT (still pending)
        coVerify { queue.enqueue("artwork", 1, true) } // queued for reconnect
        verify(exactly = 0) { detail.updateArtworkLike(1, false, any()) }
    }

    @Test
    fun `server error reverts the optimistic like`() = runTest {
        coEvery { repo.likeArtwork(1) } returns ApiResult.Error.Server(500)
        val mgr = LikeManager(repo, store, bus, detail, queue, refresh, this)

        mgr.toggleArtwork(1, like = true, likeCount = 11)
        advanceUntilIdle()

        assertNull(store.artwork(1))                                  // override cleared → server truth
        verify(exactly = 1) { detail.updateArtworkLike(1, true, 11) } // optimistic
        verify(exactly = 1) { detail.updateArtworkLike(1, false, 10) } // reverted to base
    }

    @Test
    fun `redundant unlike 4xx settles as unliked`() = runTest {
        coEvery { repo.unlikeArtwork(1) } returns ApiResult.Error.NotFound("You have not liked this artwork.")
        val mgr = LikeManager(repo, store, bus, detail, queue, refresh, this)

        mgr.toggleArtwork(1, like = false, likeCount = 4)
        advanceUntilIdle()

        assertNull(store.artwork(1))
        verify(exactly = 1) { detail.updateArtworkLike(1, false, 4) }
        verify(exactly = 0) { detail.updateArtworkLike(1, true, any()) }
    }
}
