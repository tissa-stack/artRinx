package com.example.artrinx.feature.settings.presentation.titleplan

import com.example.artrinx.feature.settings.domain.model.MockSettingsData
import com.example.artrinx.feature.settings.domain.model.ProfileTitleOption

/**
 * Title ↔ profile-type-id mapping. The settings title catalog ids (1–4) intentionally match the
 * backend `profile_type_id`s (see feature/auth/domain/model/ProfileType.kt):
 * 1 = Artist, 2 = Collector, 3 = Art Curious (role "curious"), 4 = Gallery.
 */
internal fun titleIdForRole(role: String?): Int? = when (role?.lowercase()) {
    "artist" -> 1
    "collector" -> 2
    "curious" -> 3
    "gallery" -> 4
    else -> null
}

internal fun roleForTitleId(id: Int?): String? = when (id) {
    1 -> "artist"
    2 -> "collector"
    3 -> "curious"
    4 -> "gallery"
    else -> null
}

/**
 * The user's current title card. Prefers the live `profile_title` from the API (reflects any prior
 * edit); falls back to the onboarding [role]; finally shows the raw title with no catalog copy.
 */
internal fun currentTitleOption(role: String?, profileTitle: String): ProfileTitleOption {
    val t = profileTitle.trim()
    MockSettingsData.profileTitles.firstOrNull {
        it.name.equals(t, ignoreCase = true) || it.name.substringBefore(" -").equals(t, ignoreCase = true)
    }?.let { return it }

    titleIdForRole(role)?.let { id -> MockSettingsData.profileTitles.firstOrNull { it.id == id } }?.let { return it }

    return ProfileTitleOption(id = 0, name = t.ifBlank { "—" }, description = "")
}
