package com.rinx.artRINXapp.feature.upload.domain

import com.rinx.artRINXapp.feature.upload.domain.model.UserArtItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot hand-off of artworks to preselect when opening the New Curation screen from the
 * "Add to curation → Create Curation" flow. Avoids serializing image URLs through nav args.
 */
@Singleton
class CurationSeedStore @Inject constructor() {
    private var seed: List<UserArtItem> = emptyList()

    fun set(items: List<UserArtItem>) {
        seed = items
    }

    /** Returns the staged items and clears them (single use). */
    fun consume(): List<UserArtItem> {
        val out = seed
        seed = emptyList()
        return out
    }
}
