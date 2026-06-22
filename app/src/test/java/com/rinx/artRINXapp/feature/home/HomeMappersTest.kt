package com.rinx.artRINXapp.feature.home

import com.rinx.artRINXapp.feature.home.data.remote.dto.ArtistDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.ArtworkDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.ArtworkMediumDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.ArtworkSizeDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.AuthorDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.CurationDto
import com.rinx.artRINXapp.feature.home.data.repository.HomeRepositoryImpl
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure DTO → domain mappers in [HomeRepositoryImpl]. The mappers are member extension functions, so
 * they're exercised inside `with(repo) { ... }`. The API service is unused by the mappers (relaxed
 * mock). Covers image fallbacks, artist-credit precedence, dimension formatting, and null handling.
 */
class HomeMappersTest {

    private val repo = HomeRepositoryImpl(
        mockk(relaxed = true),
        com.rinx.artRINXapp.core.util.BlockedArtworkStore(),
        com.rinx.artRINXapp.core.util.BlockedUsersStore(),
    )

    // ── toArtworkItem ─────────────────────────────────────────────────────────

    @Test
    fun `toArtworkItem prefers image_url then webp then thumbnail`() = with(repo) {
        assertEquals("img", ArtworkDto(id = 1, imageUrl = "img", webpUrl = "webp", thumbnailUrl = "thumb").toArtworkItem().imageUrl)
        assertEquals("webp", ArtworkDto(id = 1, imageUrl = null, webpUrl = "webp", thumbnailUrl = "thumb").toArtworkItem().imageUrl)
        assertEquals("thumb", ArtworkDto(id = 1, imageUrl = null, webpUrl = null, thumbnailUrl = "thumb").toArtworkItem().imageUrl)
        assertEquals("", ArtworkDto(id = 1).toArtworkItem().imageUrl)
    }

    @Test
    fun `toArtworkItem credits the named artist over the uploader display name`() = with(repo) {
        val dto = ArtworkDto(id = 1, displayName = "Uploader", artist = ArtistDto(artistName = "Real Artist"))
        assertEquals("Real Artist", dto.toArtworkItem().artistName)
    }

    @Test
    fun `toArtworkItem falls back to uploader display name when no artist is credited`() = with(repo) {
        val dto = ArtworkDto(id = 1, displayName = "Uploader", artist = null)
        assertEquals("Uploader", dto.toArtworkItem().artistName)
    }

    @Test
    fun `toArtworkItem renders a null id as an empty string`() = with(repo) {
        assertEquals("", ArtworkDto(id = null).toArtworkItem().id)
    }

    @Test
    fun `toArtworkItem carries owner and credited-artist ids for block filtering`() = with(repo) {
        val dto = ArtworkDto(id = 1, userId = 42, artist = ArtistDto(artistId = 7, artistName = "Picasso"))
        val item = dto.toArtworkItem()
        assertEquals(42, item.ownerId)
        assertEquals(7, item.artistId)
    }

    @Test
    fun `toArtworkItem leaves owner ids null when the dto has none`() = with(repo) {
        val item = ArtworkDto(id = 1).toArtworkItem()
        assertNull(item.ownerId)
        assertNull(item.artistId)
    }

    // ── toFeedPost ────────────────────────────────────────────────────────────

    @Test
    fun `toFeedPost maps likes, handle, role and owner with defaults`() = with(repo) {
        val dto = ArtworkDto(
            id = 9, title = "Title", likesCount = 5, isLiked = true,
            userId = 42, artist = ArtistDto(artistId = 7, artistName = "Picasso"),
        )
        val post = dto.toFeedPost()
        assertEquals("9", post.id)
        assertEquals(5, post.likeCount)
        assertTrue(post.isLiked)
        assertEquals("@Picasso", post.artistHandle)
        assertEquals("Artist", post.artistRole) // default when profile_type_name is null
        assertEquals(42, post.ownerId)
    }

    @Test
    fun `toFeedPost defaults likes to zero and isLiked to false when absent`() = with(repo) {
        val post = ArtworkDto(id = 1).toFeedPost()
        assertEquals(0, post.likeCount)
        assertFalse(post.isLiked)
        assertEquals("", post.artistHandle)
    }

    // ── toShoppablePost ───────────────────────────────────────────────────────

    @Test
    fun `toShoppablePost maps medium, price, shop link and dimensions`() = with(repo) {
        val dto = ArtworkDto(
            id = 3, title = "Sea", price = 250.0, shopLink = "https://shop/x",
            medium = ArtworkMediumDto(title = "Oil"),
            size = ArtworkSizeDto(heightCm = "60", widthCm = "90", unit = "cm"),
        )
        val sp = dto.toShoppablePost()
        assertEquals("Oil", sp.medium)
        assertEquals(250.0, sp.price!!, 0.0)
        assertEquals("https://shop/x", sp.shopUrl)
        assertEquals("60 × 90 cm", sp.dimensions)
    }

    @Test
    fun `toShoppablePost leaves shop url blank and dimensions null when absent`() = with(repo) {
        val sp = ArtworkDto(id = 3).toShoppablePost()
        assertEquals("", sp.shopUrl)
        assertNull(sp.price)
        assertNull(sp.dimensions)
    }

    // ── toDimensionsDisplay ───────────────────────────────────────────────────

    @Test
    fun `dimensions format whole numbers without a decimal and keep fractions`() = with(repo) {
        assertEquals("60 × 90 cm", ArtworkSizeDto(heightCm = "60.0", widthCm = "90.0", unit = "cm").toDimensionsDisplay())
        assertEquals("60.5 × 90 cm", ArtworkSizeDto(heightCm = "60.5", widthCm = "90", unit = "cm").toDimensionsDisplay())
    }

    @Test
    fun `dimensions default to cm when unit is blank`() = with(repo) {
        assertEquals("10 × 20 cm", ArtworkSizeDto(heightCm = "10", widthCm = "20", unit = "  ").toDimensionsDisplay())
    }

    @Test
    fun `dimensions handle a single provided edge`() = with(repo) {
        assertEquals("Height: 30 cm", ArtworkSizeDto(heightCm = "30", widthCm = null, unit = "cm").toDimensionsDisplay())
        assertEquals("Width: 40 cm", ArtworkSizeDto(heightCm = null, widthCm = "40", unit = "cm").toDimensionsDisplay())
    }

    @Test
    fun `dimensions are null when neither edge is a valid number`() = with(repo) {
        assertNull(ArtworkSizeDto(heightCm = null, widthCm = null).toDimensionsDisplay())
        assertNull(ArtworkSizeDto(heightCm = "abc", widthCm = "xyz").toDimensionsDisplay())
    }

    // ── toCurationItem ────────────────────────────────────────────────────────

    @Test
    fun `toCurationItem keeps natural artwork order and index-aligns ids with urls`() = with(repo) {
        val dto = CurationDto(
            id = 11, title = "Blues", isLiked = true, likesCount = 4,
            author = AuthorDto(id = 2, username = "eva", displayName = "Eva", profilePicture = "pp"),
            artworks = listOf(
                ArtworkDto(id = 100, imageUrl = "u100", medium = ArtworkMediumDto(title = "Oil")),
                ArtworkDto(id = 101, imageUrl = null, thumbnailUrl = null), // dropped: no image
                ArtworkDto(id = 102, thumbnailUrl = "t102", medium = ArtworkMediumDto(title = "Ink")),
            ),
        )
        val c = dto.toCurationItem()
        assertEquals(listOf("u100", "t102"), c.artworkUrls)
        assertEquals(listOf("100", "102"), c.artworkIds) // index-aligned, image-less one removed from both
        assertEquals("@eva", c.curatorHandle)
        assertEquals("Eva", c.curatorName)
        assertEquals("Oil, Ink", c.styles)
        assertTrue(c.isLiked)
        assertEquals(4, c.likeCount)
    }

    @Test
    fun `toCurationItem leaves styles and description blank when not provided`() = with(repo) {
        val c = CurationDto(id = 12).toCurationItem()
        assertEquals("Curator", c.curatorName) // friendly default kept
        assertEquals("", c.styles) // no fabricated "Painting" — UI hides the Styles section
        assertEquals("", c.description) // no fabricated placeholder sentence
        assertTrue(c.artworkUrls.isEmpty())
    }
}
