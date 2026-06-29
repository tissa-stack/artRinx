package com.rinx.artRINXapp.core.util

import java.util.Locale

/**
 * Capitalize the first letter of each whitespace-separated word, leaving the rest of each word exactly as
 * typed (so intentional inner caps like "McDonald" / "d'Angelo" survive) and collapsing runs of whitespace.
 * Blank input → "". Used to normalize the user's first name / full name before saving so it displays
 * capitalized throughout the app.
 */
fun String.capitalizeWords(): String =
    trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ") { word ->
        word.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString() }
    }

/**
 * Capitalize only the first character, leaving everything else (including length/spacing) untouched — safe
 * to apply on every keystroke. Used for the username, which is a single token validated live, so the
 * checked and saved value must stay identical.
 */
fun String.capitalizeFirst(): String =
    replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString() }
