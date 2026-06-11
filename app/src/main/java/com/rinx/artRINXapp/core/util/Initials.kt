package com.rinx.artRINXapp.core.util

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
