package com.rinx.artRINXapp.feature.home

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.util.BlockedArtworkStore
import com.rinx.artRINXapp.core.util.BlockedUsersStore
import com.rinx.artRINXapp.core.util.LikeStore
import com.rinx.artRINXapp.feature.home.data.remote.HomeApiService
import com.rinx.artRINXapp.feature.home.data.remote.dto.ArtworkDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.EnvelopeDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.PageDto
import com.rinx.artRINXapp.feature.home.data.repository.HomeRepositoryImpl
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import retrofit2.Response
import org.junit.Test

/**
 * Data-layer guard for the [LikeStore] overlay: a like/unlike the user made must survive a stale
 * server read (eventual consistency) applied on any fresh fetch, without double-counting, and the
 * override must self-clear once the server agrees. Mirrors the blocked-filter tests' setup.
 */
class LikeOverlayTest {

    private val api = mockk<HomeApiService>()
    private val likeStore = LikeStore()
    private val repo = HomeRepositoryImpl(api, BlockedArtworkStore(), BlockedUsersStore(), likeStore)

    private fun allPage(vararg dtos: ArtworkDto) =
        Response.success(EnvelopeDto(success = true, code = 200, data = PageDto(items = dtos.toList(), total = dtos.size)))

    @Test
    fun `stale server unliked is overridden to liked with +1 count`() = runTest {
        likeStore.setArtwork(1, true) // user just liked it
        coEvery { api.getAllArtworks(1, 10) } returns allPage(
            ArtworkDto(id = 1, userId = 99, isLiked = false, likesCount = 10), // server hasn't caught up
        )

        val item = (repo.getDiscoverArtworks(1, 10) as ApiResult.Success).data.items.single()
        assertEquals(true, item.isLiked)
        assertEquals(11, item.likeCount) // base 10 + the user's own like
    }

    @Test
    fun `server agreement leaves item untouched and self-clears the override`() = runTest {
        likeStore.setArtwork(1, true)
        coEvery { api.getAllArtworks(1, 10) } returns allPage(
            ArtworkDto(id = 1, userId = 99, isLiked = true, likesCount = 11), // server now reflects the like
        )

        val item = (repo.getDiscoverArtworks(1, 10) as ApiResult.Success).data.items.single()
        assertEquals(true, item.isLiked)
        assertEquals(11, item.likeCount)     // no extra delta on agreement
        assertNull(likeStore.artwork(1))     // confirmed → override dropped, server authoritative again
    }

    @Test
    fun `stale server liked is overridden to unliked with -1 count (never below zero)`() = runTest {
        likeStore.setArtwork(2, false) // user just unliked it
        coEvery { api.getShopArtworks(1, 10) } returns allPage(
            ArtworkDto(id = 2, userId = 99, isLiked = true, likesCount = 0), // inconsistent/stale server
        )

        val item = (repo.getShopArtworks(1, 10) as ApiResult.Success).data.items.single()
        assertEquals(false, item.isLiked)
        assertEquals(0, item.likeCount) // coerced at zero, not -1
    }

    @Test
    fun `items without an override pass through and order and size are preserved`() = runTest {
        likeStore.setArtwork(2, true)
        coEvery { api.getAllArtworks(1, 10) } returns allPage(
            ArtworkDto(id = 1, userId = 99, isLiked = false, likesCount = 3),
            ArtworkDto(id = 2, userId = 99, isLiked = false, likesCount = 3),
            ArtworkDto(id = 3, userId = 99, isLiked = false, likesCount = 3),
        )

        val items = (repo.getDiscoverArtworks(1, 10) as ApiResult.Success).data.items
        assertEquals(listOf("1", "2", "3"), items.map { it.id }) // order + size intact
        assertEquals(false, items[0].isLiked)
        assertEquals(true, items[1].isLiked)  // only the overridden one changes
        assertEquals(4, items[1].likeCount)
        assertEquals(false, items[2].isLiked)
    }
}
