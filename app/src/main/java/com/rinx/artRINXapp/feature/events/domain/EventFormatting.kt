package com.rinx.artRINXapp.feature.events.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Pure formatting helpers for the event popup. Kept free of Android/Compose deps so they're
 * unit-testable. Times are parsed as ISO-8601 UTC and rendered in the event's IANA timezone;
 * an unknown/blank zone falls back to UTC (java.util.TimeZone.getTimeZone returns GMT for
 * unrecognised ids, which is the desired fallback — we also normalise blank → "UTC").
 */
object EventFormatting {

    /** "September 3rd, 2025" — full month name, ordinal day, year — rendered in [tz]. */
    fun formatDate(startIsoUtc: String?, tz: String?): String {
        val instant = parseUtc(startIsoUtc) ?: return ""
        val zone = zoneOf(tz)
        val month = SimpleDateFormat("MMMM", Locale.US).apply { timeZone = zone }.format(instant)
        val day = SimpleDateFormat("d", Locale.US).apply { timeZone = zone }.format(instant).toInt()
        val year = SimpleDateFormat("yyyy", Locale.US).apply { timeZone = zone }.format(instant)
        return "$month ${day}${ordinalSuffix(day)}, $year"
    }

    /** "6:00PM – 9:00PM" — en-dash separator, no space before AM/PM — rendered in [tz]. */
    fun formatTimeRange(startIsoUtc: String?, endIsoUtc: String?, tz: String?): String {
        val start = parseUtc(startIsoUtc)?.let { time(it, tz) }
        val end = parseUtc(endIsoUtc)?.let { time(it, tz) }
        return when {
            start != null && end != null -> "$start – $end"
            start != null -> start
            end != null -> end
            else -> ""
        }
    }

    /**
     * Location block lines (blanks dropped):
     *  - line 1: venue name
     *  - line 2: street address
     *  - line 3: "City, ST PostalCode" for US, else "City, Country"
     */
    fun locationLines(
        venueName: String?,
        streetAddress: String?,
        city: String?,
        state: String?,
        postalCode: String?,
        country: String?,
    ): List<String> {
        val lines = mutableListOf<String>()
        venueName?.trim()?.takeIf { it.isNotEmpty() }?.let { lines.add(it) }
        streetAddress?.trim()?.takeIf { it.isNotEmpty() }?.let { lines.add(it) }

        val cityClean = city?.trim().orEmpty()
        val third = if (isUnitedStates(country)) {
            // "City, ST PostalCode"
            val st = state?.trim().orEmpty().uppercase(Locale.US)
            val zip = postalCode?.trim().orEmpty()
            listOf(cityClean.let { if (st.isNotEmpty()) "$it, $st" else it }, zip)
                .filter { it.isNotEmpty() }
                .joinToString(" ")
                .trim()
        } else {
            // "City, Country"
            val countryClean = country?.trim()?.let { titleCase(it) }.orEmpty()
            listOf(cityClean, countryClean).filter { it.isNotEmpty() }.joinToString(", ")
        }
        if (third.isNotEmpty()) lines.add(third)
        return lines
    }

    // ── internals ──────────────────────────────────────────────────────────────

    private fun time(date: Date, tz: String?): String =
        SimpleDateFormat("h:mma", Locale.US).apply { timeZone = zoneOf(tz) }.format(date)

    private fun zoneOf(tz: String?): TimeZone {
        val id = tz?.trim().takeUnless { it.isNullOrEmpty() } ?: "UTC"
        // getTimeZone returns GMT for unknown ids — that's our UTC fallback.
        return TimeZone.getTimeZone(id)
    }

    /**
     * Server stamps may carry 6-digit microseconds and/or a trailing 'Z' or offset
     * (e.g. "2026-05-20T01:16:00Z" or "2026-05-20T01:16:00.489176"). SimpleDateFormat.SSS would
     * misread microseconds, so take the seconds-precision prefix and treat it as UTC.
     */
    private fun parseUtc(iso: String?): Date? {
        if (iso.isNullOrBlank()) return null
        val base = iso.trim()
        if (base.length < 19) return null
        val truncated = base.substring(0, 19)
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(truncated)
        } catch (_: Exception) {
            null
        }
    }

    private fun ordinalSuffix(day: Int): String {
        if (day in 11..13) return "th"
        return when (day % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }

    private fun isUnitedStates(country: String?): Boolean {
        val c = country?.trim()?.lowercase(Locale.US) ?: return false
        return c == "us" || c == "usa" || c == "u.s." || c == "u.s.a." ||
            c == "united states" || c == "united states of america"
    }

    private fun titleCase(s: String): String =
        s.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }
}
