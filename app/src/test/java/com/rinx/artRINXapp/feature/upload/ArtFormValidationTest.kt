package com.rinx.artRINXapp.feature.upload

import com.rinx.artRINXapp.feature.upload.domain.model.ArtFormState
import com.rinx.artRINXapp.feature.upload.domain.model.ArtistResult
import com.rinx.artRINXapp.feature.upload.domain.model.ShopLinkVisibility
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtFormValidationTest {

    private val artist = ArtistResult(handle = "", displayName = "Jane Doe", subtitle = "")

    /** A form with all required fields satisfied and no shop link. */
    private fun validBase() = ArtFormState(
        title = "My Art",
        description = "A description",
        selectedArtist = artist,
        selectedMediumId = 3,
        tags = listOf("abstract"),
    )

    @Test
    fun `missing tags is invalid`() {
        assertFalse(validBase().copy(tags = emptyList()).isValid)
    }

    @Test
    fun `empty form is invalid`() {
        assertFalse(ArtFormState().isValid)
    }

    @Test
    fun `title alone is not enough`() {
        assertFalse(ArtFormState(title = "T").isValid)
    }

    @Test
    fun `missing description is invalid`() {
        assertFalse(validBase().copy(description = "   ").isValid)
    }

    @Test
    fun `missing artist name is invalid`() {
        assertFalse(validBase().copy(selectedArtist = null).isValid)
        assertFalse(validBase().copy(selectedArtist = artist.copy(displayName = " ")).isValid)
    }

    @Test
    fun `missing medium is invalid`() {
        assertFalse(validBase().copy(selectedMediumId = null).isValid)
    }

    @Test
    fun `all required fields and no shop link is valid`() {
        assertTrue(validBase().isValid)
    }

    @Test
    fun `artist id is optional - name only is valid`() {
        val nameOnly = artist.copy(userId = null)
        assertTrue(validBase().copy(selectedArtist = nameOnly).isValid)
    }

    @Test
    fun `shop link entered without price is invalid`() {
        val s = validBase().copy(
            shopLinkVisibility = ShopLinkVisibility.VISIBLE,
            shopLink = "https://shop.example/art",
            price = "",
        )
        assertFalse(s.isValid)
    }

    @Test
    fun `shop link with zero or non-numeric price is invalid`() {
        val base = validBase().copy(
            shopLinkVisibility = ShopLinkVisibility.VISIBLE,
            shopLink = "https://shop.example/art",
        )
        assertFalse(base.copy(price = "0").isValid)
        assertFalse(base.copy(price = "abc").isValid)
    }

    @Test
    fun `shop link with valid price is valid`() {
        val s = validBase().copy(
            shopLinkVisibility = ShopLinkVisibility.VISIBLE,
            shopLink = "https://shop.example/art",
            price = "250",
        )
        assertTrue(s.isValid)
    }

    @Test
    fun `price not required when no shop link is entered`() {
        // Shop link is now a normal field for everyone; with a blank link, price isn't required.
        val s = validBase().copy(shopLink = "", price = "")
        assertTrue(s.isValid)
    }

    @Test
    fun `price required whenever a shop link is entered regardless of visibility`() {
        // The premium gating was removed — any non-blank shop link requires a valid price.
        val s = validBase().copy(
            shopLinkVisibility = ShopLinkVisibility.HIDDEN,
            shopLink = "https://shop.example/art",
            price = "",
        )
        assertFalse(s.isValid)
    }
}
