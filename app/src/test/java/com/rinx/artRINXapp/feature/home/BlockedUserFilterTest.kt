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
 * Data-layer guard for blocking a *user*: the repository must drop any artwork whose uploader
 * (`userId`) or credited artist (`artist.artistId`) is in [BlockedUsersStore], so a blocked
 * person's art never reaches the UI — even before the next network refresh. Mirrors the existing
 * blocked-*artwork* filter, now keyed on owner ids. Null-owner items are always kept (no false drops).
 */
class BlockedUserFilterTest {

    private val api = mockk<HomeApiService>()
    private val blockedArtwork = BlockedArtworkStore()
    private val blockedUsers = BlockedUsersStore()
    private val repo = HomeRepositoryImpl(api, blockedArtwork, blockedUsers)

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
        assertEquals(listOf("2"), result.data.map { it.id })
    }

    @Test
    fun `getShopArtworks drops art credited to a blocked artist`() = runTest {
        blockedUsers.markBlocked(7)
        coEvery { api.getShopArtworks(any(), any()) } returns shopPage(
            ArtworkDto(id = 1, userId = 99, artist = ArtistDto(artistId = 7)), // blocked artist → dropped
            ArtworkDto(id = 2, userId = 99),
        )

        val result = repo.getShopArtworks(1, 20) as ApiResult.Success
        assertEquals(listOf("2"), result.data.map { it.id })
    }

    @Test
    fun `getShopArtworks keeps everything when nobody is blocked`() = runTest {
        coEvery { api.getShopArtworks(any(), any()) } returns shopPage(
            ArtworkDto(id = 1, userId = 42),
            ArtworkDto(id = 2, userId = null), // null owner must never be dropped
        )

        val result = repo.getShopArtworks(1, 20) as ApiResult.Success
        assertEquals(listOf("1", "2"), result.data.map { it.id })
    }
}
