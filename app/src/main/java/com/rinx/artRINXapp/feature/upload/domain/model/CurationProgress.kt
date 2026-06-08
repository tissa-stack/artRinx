package com.rinx.artRINXapp.feature.upload.domain.model

/**
 * Cross-screen state of the currently-running (or just-finished) curation create.
 * [thumbnail] is the first selected artwork's remote image URL (a String, since curations are
 * built from existing artworks — no local file). [isPrivate] routes the row/result to Home
 * (public) or Profile › Curations (private).
 */
sealed interface CurationProgress {
    val thumbnail: String?
    val isPrivate: Boolean

    data class Creating(
        override val thumbnail: String?,
        override val isPrivate: Boolean,
    ) : CurationProgress

    data class Success(
        override val thumbnail: String?,
        override val isPrivate: Boolean,
        val curation: CreatedCuration,
        val title: String,
        val artworkUrls: List<String>,
    ) : CurationProgress

    data class Failed(
        override val thumbnail: String?,
        override val isPrivate: Boolean,
        val retryable: Boolean,
        val message: String,
    ) : CurationProgress
}
