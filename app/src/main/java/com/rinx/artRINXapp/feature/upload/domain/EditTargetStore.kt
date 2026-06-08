package com.rinx.artRINXapp.feature.upload.domain

import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot hand-off of the id being edited when the create flow is reused as an edit flow.
 * The detail screen stages the id here, then navigates to the existing NEW_ART / NEW_CURATION
 * route; the create ViewModel consumes it on init. Avoids changing nav-arg signatures.
 * Mirrors [CurationSeedStore].
 */
@Singleton
class EditTargetStore @Inject constructor() {
    private var artworkId: Int? = null
    private var curationId: Int? = null

    fun setArtwork(id: Int) {
        artworkId = id
        curationId = null
    }

    fun setCuration(id: Int) {
        curationId = id
        artworkId = null
    }

    /** Returns the staged artwork id and clears it (single use). */
    fun consumeArtwork(): Int? {
        val out = artworkId
        artworkId = null
        return out
    }

    /** Returns the staged curation id and clears it (single use). */
    fun consumeCuration(): Int? {
        val out = curationId
        curationId = null
        return out
    }
}
