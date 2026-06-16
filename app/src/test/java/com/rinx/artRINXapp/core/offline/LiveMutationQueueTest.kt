package com.rinx.artRINXapp.core.offline

import android.content.Context
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.home.domain.repository.HomeRepository
import com.rinx.artRINXapp.testutil.FakePreferencesDataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

/**
 * Offline like/unlike queue. Verifies net-of-action dedup (latest action per target wins), that
 * [LiveMutationQueue.drain] drops on success but KEEPS items on a network failure, and that
 * [LiveMutationQueue.clear] empties the queue. Uses an in-memory DataStore + a mocked repository —
 * no real likes are ever sent, and a mocked Context yields a null ConnectivityManager (handled).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LiveMutationQueueTest {

    private fun newQueue(scope: kotlinx.coroutines.CoroutineScope, repo: HomeRepository): LiveMutationQueue =
        LiveMutationQueue(repo, FakePreferencesDataStore(), mockk<Context>(relaxed = true), scope)

    @Test
    fun `net-of-action - only the latest action per target is replayed`() = runTest {
        val repo = mockk<HomeRepository>(relaxed = true)
        coEvery { repo.likeArtwork(any()) } returns ApiResult.Success(Unit)
        coEvery { repo.unlikeArtwork(any()) } returns ApiResult.Success(Unit)

        val queue = newQueue(backgroundScope, repo)
        advanceUntilIdle() // let the init-time drain (empty queue) settle

        queue.enqueue("artwork", 5, like = true)
        queue.enqueue("artwork", 5, like = false)
        queue.enqueue("artwork", 5, like = true) // final desired state: liked

        queue.drain()

        coVerify(exactly = 1) { repo.likeArtwork(5) }
        coVerify(exactly = 0) { repo.unlikeArtwork(any()) }
    }

    @Test
    fun `drain keeps a mutation that fails on a network error and retries it later`() = runTest {
        val repo = mockk<HomeRepository>(relaxed = true)
        coEvery { repo.likeArtwork(5) } returns ApiResult.Error.Network(IOException("offline"))

        val queue = newQueue(backgroundScope, repo)
        advanceUntilIdle()

        queue.enqueue("artwork", 5, like = true)
        queue.drain()
        coVerify(exactly = 1) { repo.likeArtwork(5) } // attempted, still offline → kept

        // Connectivity returns: the kept mutation replays and now succeeds.
        coEvery { repo.likeArtwork(5) } returns ApiResult.Success(Unit)
        queue.drain()
        coVerify(exactly = 2) { repo.likeArtwork(5) }

        // Once drained successfully it is gone — a further drain is a no-op.
        queue.drain()
        coVerify(exactly = 2) { repo.likeArtwork(5) }
    }

    @Test
    fun `drain drops a mutation on success`() = runTest {
        val repo = mockk<HomeRepository>(relaxed = true)
        coEvery { repo.unlikeCuration(7) } returns ApiResult.Success(Unit)

        val queue = newQueue(backgroundScope, repo)
        advanceUntilIdle()

        queue.enqueue("curation", 7, like = false)
        queue.drain()
        queue.drain() // second drain must not replay

        coVerify(exactly = 1) { repo.unlikeCuration(7) }
    }

    @Test
    fun `clear empties the queue so nothing is replayed`() = runTest {
        val repo = mockk<HomeRepository>(relaxed = true)
        coEvery { repo.likeArtwork(any()) } returns ApiResult.Success(Unit)

        val queue = newQueue(backgroundScope, repo)
        advanceUntilIdle()

        queue.enqueue("artwork", 5, like = true)
        queue.clear()
        queue.drain()

        coVerify(exactly = 0) { repo.likeArtwork(any()) }
    }
}
