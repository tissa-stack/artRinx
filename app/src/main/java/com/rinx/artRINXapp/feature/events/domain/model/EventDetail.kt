package com.rinx.artRINXapp.feature.events.domain.model

import androidx.compose.runtime.Immutable

/** Preformatted event popup content (formatting resolved in the repository for testability). */
@Immutable
data class EventDetail(
    val id: Long,
    val name: String,
    val imageUrl: String?,
    /** e.g. "September 3rd, 2025" — empty when the start stamp is missing. */
    val dateText: String,
    /** e.g. "6:00PM – 9:00PM" — empty when both stamps are missing. */
    val timeText: String,
    /** 1–3 lines (venue / street / "City, ST Postal" or "City, Country"); blanks dropped. */
    val locationLines: List<String>,
)
