package com.example.artrinx.feature.upload.domain.model

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
