package com.rinx.artRINXapp.feature.home

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.util.BlockedArtworkStore
import com.rinx.artRINXapp.core.util.BlockedUsersStore
import com.rinx.artRINXapp.feature.home.data.remote.HomeApiService
import com.rinx.artRINXapp.feature.home.data.remote.dto.ArtistDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.ArtworkDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.EnvelopeDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.PageDto
import com.rinx.artRINXapp.feature.home.data.repository.HomeRepositoryImpl
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import retrofit2.Response
import org.junit.Test

/**
 * Data-layer guard for blocking a *user*: the repository must drop any artwork whose UPLOADER
 * (`userId`) is in [BlockedUsersStore], so a blocked person's own posts never reach the UI — even
 * before the next network refresh. Art merely CREDITING a blocked user as artist but uploaded by
 * someone else is KEPT (blocking hides the person's presence, not others' legitimate posts).
 * Null-owner items are always kept (no false drops).
 */
class BlockedUserFilterTest {

    private val api = mockk<HomeApiService>()
    private val blockedArtwork = BlockedArtworkStore()
    private val blockedUsers = BlockedUsersStore()
    private val repo = HomeRepositoryImpl(api, blockedArtwork, blockedUsers, com.rinx.artRINXapp.core.util.LikeStore())

    private fun shopPage(vararg dtos: ArtworkDto) =
        Response.success(EnvelopeDto(success = true, code = 200, data = PageDto(items = dtos.toList())))

    @Test
    fun `getShopArtworks drops art uploaded by a blocked user`() = runTest {
        blockedUsers.markBlocked(42)
        coEvery { api.getShopArtworks(any(), any()) } returns shopPage(
            ArtworkDto(id = 1, userId = 42),   // blocked uploader → dropped
            ArtworkDto(id = 2, userId = 99),   // unrelated → kept
        )

        val result = repo.getShopArtworks(1, 20) as ApiResult.Success
        assertEquals(listOf("2"), result.data.items.map { it.id })
    }

    @Test
    fun `getShopArtworks keeps art merely credited to a blocked artist when someone else uploaded it`() = runTest {
        blockedUsers.markBlocked(7)
        coEvery { api.getShopArtworks(any(), any()) } returns shopPage(
            ArtworkDto(id = 1, userId = 99, artist = ArtistDto(artistId = 7)), // uploaded by 99, credits blocked 7 → kept
            ArtworkDto(id = 2, userId = 7),                                    // uploaded by blocked 7 → dropped
        )

        val result = repo.getShopArtworks(1, 20) as ApiResult.Success
        assertEquals(listOf("1"), result.data.items.map { it.id })
    }

    @Test
    fun `getShopArtworks keeps everything when nobody is blocked`() = runTest {
        coEvery { api.getShopArtworks(any(), any()) } returns shopPage(
            ArtworkDto(id = 1, userId = 42),
            ArtworkDto(id = 2, userId = null), // null owner must never be dropped
        )

        val result = repo.getShopArtworks(1, 20) as ApiResult.Success
        assertEquals(listOf("1", "2"), result.data.items.map { it.id })
    }

    // ── Discover pagination (GET /artworks/all) ────────────────────────────────

    private fun allPage(total: Int, vararg dtos: ArtworkDto) =
        Response.success(EnvelopeDto(success = true, code = 200, data = PageDto(items = dtos.toList(), total = total)))

    @Test
    fun `getDiscoverArtworks maps items and reports not-ended when more pages remain`() = runTest {
        coEvery { api.getAllArtworks(1, 10) } returns allPage(
            total = 25,
            ArtworkDto(id = 1, userId = 99),
            ArtworkDto(id = 2, userId = 99),
        )

        val result = repo.getDiscoverArtworks(1, 10) as ApiResult.Success
        assertEquals(listOf("1", "2"), result.data.items.map { it.id })
        assertEquals(false, result.data.endReached) // 1*10 < 25
    }

    @Test
    fun `getDiscoverArtworks reports ended on the last page per total`() = runTest {
        coEvery { api.getAllArtworks(3, 10) } returns allPage(
            total = 25,
            ArtworkDto(id = 21, userId = 99),
        )

        val result = repo.getDiscoverArtworks(3, 10) as ApiResult.Success
        assertEquals(true, result.data.endReached) // 3*10 >= 25
    }

    @Test
    fun `getDiscoverArtworks does not falsely end when total is zero but items returned`() = runTest {
        coEvery { api.getAllArtworks(1, 10) } returns allPage(
            total = 0, // endpoint omitted/zeroed total → must not stop while a full page came back
            ArtworkDto(id = 1, userId = 99),
        )

        val result = repo.getDiscoverArtworks(1, 10) as ApiResult.Success
        assertEquals(false, result.data.endReached)
    }
}
