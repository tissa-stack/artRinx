package com.example.artrinx.feature.search.domain.model

import androidx.compose.runtime.Immutable

enum class SearchPhase { IDLE, ACTIVE_EMPTY, RESULTS }

enum class SortOption { NEWEST, OLDEST, MOST_POPULAR }

enum class ResultTab { ART, USERS, CURATIONS }

enum class CardHeight { SHORT, MEDIUM, TALL }

/** Wire value for the `sort_by` query param (newest | oldest | most_popular). */
val SortOption.apiValue: String
    get() = when (this) {
        SortOption.NEWEST -> "newest"
        SortOption.OLDEST -> "oldest"
        SortOption.MOST_POPULAR -> "most_popular"
    }

/** Wire value for the required `category` query param of /api/search. */
val ResultTab.category: String
    get() = when (this) {
        ResultTab.ART -> "artwork"
        ResultTab.USERS -> "user"
        ResultTab.CURATIONS -> "curation"
    }

@Immutable
data class SearchFilter(
    val shopArtOnly: Boolean = false,
    val mediumIds: Set<Int> = emptySet(),
) {
    val hasAnySelection: Boolean
        get() = shopArtOnly || mediumIds.isNotEmpty()
}

@Immutable
data class SearchResultItem(
    val id: String,
    val imageUrl: String,
    val title: String,
    val artistName: String,
    val cardHeight: CardHeight,
    val artId: String = "",   // artwork id used for detail navigation
)

/**
 * Static medium labels. The search filter now uses the live `/api/mediums/` list, but the upload
 * flow's medium picker still references this hardcoded set — kept here for that consumer.
 */
val STYLE_OPTIONS = listOf(
    "Digital", "Drawing", "Mixed Media", "Painting", "Photography", "Prints", "Sculpture",
)

@Immutable
data class UserSearchItem(
    val id: String,
    val username: String,
    val displayName: String,
    val profileTypeName: String,
    val profilePictureUrl: String? = null,
)