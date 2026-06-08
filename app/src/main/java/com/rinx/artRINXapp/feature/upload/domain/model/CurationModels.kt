package com.rinx.artRINXapp.feature.upload.domain.model

/** Everything the curation-create flow needs, decoupled from UI state. */
data class CreateCurationRequest(
    val title: String,
    val description: String?,
    /** privacy=true → PRIVATE (Profile curations); privacy=false → PUBLIC (Home). */
    val isPrivate: Boolean,
    val artworkIds: List<Int>,
    /** Selected artworks' remote image URLs — for the progress thumbnail + optimistic display. */
    val artworkUrls: List<String>,
)

/** Result of POST /api/curations/. */
data class CreatedCuration(
    val id: Int,
)

/** Existing curation fields used to prefill the edit (New Curation) flow. */
data class EditableCuration(
    val title: String,
    val description: String?,
    val isPrivate: Boolean,
    val arts: List<UserArtItem>,
)

/** What the "Add to curation" sheet is acting on. */
sealed interface CurationSource {
    /** A single artwork post. */
    data class Artwork(val artworkId: Int, val imageUrl: String?) : CurationSource

    /** A whole curation — all of its artworks are the source. */
    data class Curation(val curationId: Int) : CurationSource
}
