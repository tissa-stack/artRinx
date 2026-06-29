package com.rinx.artRINXapp.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NameFormattingTest {

    @Test
    fun `single lowercase word is capitalized`() {
        assertEquals("John", "john".capitalizeWords())
    }

    @Test
    fun `each word is capitalized`() {
        assertEquals("John Doe", "john doe".capitalizeWords())
    }

    @Test
    fun `surrounding and repeated whitespace is trimmed and collapsed`() {
        assertEquals("Jane Mary", "  jane   mary  ".capitalizeWords())
    }

    @Test
    fun `intentional inner caps are preserved`() {
        assertEquals("McDonald", "mcDonald".capitalizeWords())
        assertEquals("John McDonald", "john mcDonald".capitalizeWords())
    }

    @Test
    fun `already-capitalized input is unchanged`() {
        assertEquals("John Doe", "John Doe".capitalizeWords())
    }

    @Test
    fun `blank input returns empty`() {
        assertEquals("", "".capitalizeWords())
        assertEquals("", "   ".capitalizeWords())
    }

    // ── capitalizeFirst (username) ────────────────────────────────────────────

    @Test
    fun `capitalizeFirst uppercases only the first character and preserves the rest`() {
        assertEquals("Johndoe", "johndoe".capitalizeFirst())
        assertEquals("John_doe", "john_doe".capitalizeFirst())
        assertEquals("McDonald", "mcDonald".capitalizeFirst()) // rest untouched
    }

    @Test
    fun `capitalizeFirst preserves length and does not trim`() {
        assertEquals("Ab c ", "ab c ".capitalizeFirst()) // length/spacing untouched (safe live typing)
        assertEquals("", "".capitalizeFirst())
    }
}
