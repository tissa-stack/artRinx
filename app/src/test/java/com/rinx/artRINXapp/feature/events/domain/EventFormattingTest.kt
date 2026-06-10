package com.rinx.artRINXapp.feature.events.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class EventFormattingTest {

    // ── Date ───────────────────────────────────────────────────────────────────

    @Test
    fun `date renders month name, ordinal day and year in event tz`() {
        // 2025-09-03T22:00:00Z in New York is still Sept 3rd (18:00 EDT).
        val date = EventFormatting.formatDate("2025-09-03T22:00:00Z", "America/New_York")
        assertEquals("September 3rd, 2025", date)
    }

    @Test
    fun `date crosses to next day when tz pushes past midnight`() {
        // 2026-05-20T01:16:00Z in Asia/Kolkata is 06:46 on May 20.
        val date = EventFormatting.formatDate("2026-05-20T01:16:00Z", "Asia/Kolkata")
        assertEquals("May 20th, 2026", date)
    }

    @Test
    fun `ordinal suffixes are correct for st nd rd th and teens`() {
        assertEquals("September 1st, 2025", EventFormatting.formatDate("2025-09-01T12:00:00Z", "UTC"))
        assertEquals("September 2nd, 2025", EventFormatting.formatDate("2025-09-02T12:00:00Z", "UTC"))
        assertEquals("September 3rd, 2025", EventFormatting.formatDate("2025-09-03T12:00:00Z", "UTC"))
        assertEquals("September 4th, 2025", EventFormatting.formatDate("2025-09-04T12:00:00Z", "UTC"))
        assertEquals("September 11th, 2025", EventFormatting.formatDate("2025-09-11T12:00:00Z", "UTC"))
        assertEquals("September 12th, 2025", EventFormatting.formatDate("2025-09-12T12:00:00Z", "UTC"))
        assertEquals("September 13th, 2025", EventFormatting.formatDate("2025-09-13T12:00:00Z", "UTC"))
        assertEquals("September 21st, 2025", EventFormatting.formatDate("2025-09-21T12:00:00Z", "UTC"))
        assertEquals("September 23rd, 2025", EventFormatting.formatDate("2025-09-23T12:00:00Z", "UTC"))
    }

    @Test
    fun `blank or unparseable date returns empty`() {
        assertEquals("", EventFormatting.formatDate(null, "UTC"))
        assertEquals("", EventFormatting.formatDate("", "UTC"))
        assertEquals("", EventFormatting.formatDate("not-a-date", "UTC"))
    }

    // ── Time ───────────────────────────────────────────────────────────────────

    @Test
    fun `time range uses en-dash and no space before AM PM`() {
        // 18:00–21:00 New York.
        val time = EventFormatting.formatTimeRange(
            "2025-09-03T22:00:00Z",
            "2025-09-04T01:00:00Z",
            "America/New_York",
        )
        assertEquals("6:00PM – 9:00PM", time)
    }

    @Test
    fun `unknown tz falls back to UTC`() {
        val time = EventFormatting.formatTimeRange(
            "2026-05-20T18:00:00Z",
            "2026-05-20T21:00:00Z",
            "Not/AZone",
        )
        assertEquals("6:00PM – 9:00PM", time)
    }

    @Test
    fun `missing tz falls back to UTC`() {
        val time = EventFormatting.formatTimeRange(
            "2026-05-20T06:00:00Z",
            "2026-05-20T09:00:00Z",
            null,
        )
        assertEquals("6:00AM – 9:00AM", time)
    }

    @Test
    fun `time range with only a start shows just the start`() {
        val time = EventFormatting.formatTimeRange("2026-05-20T06:00:00Z", null, "UTC")
        assertEquals("6:00AM", time)
    }

    // ── Location ─────────────────────────────────────────────────────────────────

    @Test
    fun `US location formats City, ST PostalCode`() {
        val lines = EventFormatting.locationLines(
            venueName = "Untitled Gallery",
            streetAddress = "459 Fifth Avenue",
            city = "New York",
            state = "NY",
            postalCode = "10007",
            country = "USA",
        )
        assertEquals(listOf("Untitled Gallery", "459 Fifth Avenue", "New York, NY 10007"), lines)
    }

    @Test
    fun `single-tier country formats City, Country`() {
        val lines = EventFormatting.locationLines(
            venueName = "Maharashtra",
            streetAddress = "5, 2nd Floor, Bharmal House",
            city = "Mumbai",
            state = "maharashtra",
            postalCode = "400003",
            country = "india",
        )
        assertEquals(listOf("Maharashtra", "5, 2nd Floor, Bharmal House", "Mumbai, India"), lines)
    }

    @Test
    fun `blank location fields are dropped`() {
        val lines = EventFormatting.locationLines(
            venueName = "  ",
            streetAddress = null,
            city = "Paris",
            state = null,
            postalCode = null,
            country = "France",
        )
        assertEquals(listOf("Paris, France"), lines)
    }
}
