package com.rinx.artRINXapp.core.util

import androidx.compose.ui.graphics.Color

/**
 * Initials for an avatar fallback when a user has no profile picture.
 * "Shiva Krishna" → "SK"; single word "shivakrishna" → "SH"; blank/null → null.
 */
fun initialsOf(name: String?): String? {
    val words = name?.trim()?.split(Regex("\\s+"))?.filter { it.isNotBlank() }.orEmpty()
    return when {
        words.isEmpty() -> null
        words.size == 1 -> words[0].take(2).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}

/**
 * A stable, soft pastel background for an initials avatar, derived from [name] so the same user
 * always gets the same colour. Returns a light pastel (high lightness, low saturation) that reads
 * well behind dark initials text in both themes.
 */
fun pastelColorFor(name: String?): Color {
    val key = name?.trim()?.lowercase().orEmpty().ifEmpty { "?" }
    var hash = 0
    for (c in key) hash = c.code + ((hash shl 5) - hash) // simple deterministic string hash
    val hue = ((hash % 360) + 360) % 360
    return Color.hsv(hue.toFloat(), 0.40f, 0.85f)
}
