package com.example.artrinx.feature.upload.domain.model

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.example.artrinx.R

// ── Enums ─────────────────────────────────────────────────────────────────────

enum class PrivacyOption { PUBLIC, PRIVATE }
enum class ArtTab { UPLOADS, LIKED }

/** Overlay state shown on the create screen while a PRIVATE artwork/curation is being created. */
enum class CreationStatus { LOADING, CREATED, FAILED }

// ── Artist ────────────────────────────────────────────────────────────────────

@Immutable
data class ArtistResult(
    val handle: String,
    val displayName: String,
    val subtitle: String,               // "myself" / "Wade Huston · Following" etc.
    @param:DrawableRes val avatarRes: Int? = null,
    /** Registered RINX user id; null for a non-RINX / free entry. */
    val userId: Int? = null,
    /** Remote avatar (real users). Preferred over [avatarRes] when present. */
    val avatarUrl: String? = null,
)

// ── Upload Art form ───────────────────────────────────────────────────────────

data class ArtFormState(
    val imageUri: Uri? = null,
    val title: String = "",
    val description: String = "",
    val selectedArtist: ArtistResult? = null,
    val selfArtist: ArtistResult? = null,
    val artistResults: List<ArtistResult> = emptyList(),
    val selectedMedium: String? = null,
    val selectedMediumId: Int? = null,
    val tags: List<String> = emptyList(),
    val tagSuggestions: List<String> = emptyList(),
    val mediums: List<MediumOption> = emptyList(),
    val shopLink: String = "",
    val privacy: PrivacyOption = PrivacyOption.PUBLIC,
    val showMediumPicker: Boolean = false,
    val showPrivacyPicker: Boolean = false,
    val currentTagInput: String = "",
    val isTitleError: Boolean = false,
    val isDescriptionError: Boolean = false,
    val isUploading: Boolean = false,
    val artistSearchQuery: String = "",
    /** Non-null while a PRIVATE upload is in flight / just finished (drives the overlay). */
    val creationStatus: CreationStatus? = null,
    val creationError: String? = null,
) {
    val isValid: Boolean get() = title.isNotEmpty()
}

/** A selectable medium for the upload form (id needed for the create-artwork call). */
@Immutable
data class MediumOption(
    val id: Int,
    val title: String,
)

// ── Upload domain models ────────────────────────────────────────────────────

/** Everything the upload flow needs from the form, decoupled from UI state. */
data class UploadRequest(
    val imageUri: Uri,
    val title: String,
    val description: String?,
    val tags: List<String>,
    val mediumId: Int?,
    val shopLink: String?,
    val price: Double?,
    /** privacy=true → PRIVATE (profile only); privacy=false → PUBLIC (home feed). */
    val isPrivate: Boolean,
    /** Artist attribution (both null = no artist → defaults to the uploader). */
    val artistId: Int? = null,
    val artistName: String? = null,
)

/** Step-1 result: the signed CDN URL + the file path to finalize with. */
data class PreparedUpload(
    val filePath: String,
    val uploadUrl: String,
    val rekognitionTags: String? = null,
)

/** Step-3 result. */
data class CreatedArtwork(
    val id: Int,
    val imageUrl: String,
)

// ── New Curation form ─────────────────────────────────────────────────────────

@Immutable
data class UserArtItem(
    val id: String,
    @param:DrawableRes val imageRes: Int? = null,
    val isSelected: Boolean = false,
    /** Remote thumbnail (real artworks). Preferred over [imageRes] when present. */
    val imageUrl: String? = null,
    /** Numeric artwork id used to build the curation's artwork_ids. */
    val artworkId: Int? = null,
)

data class NewCurationState(
    val selectedArts: List<UserArtItem> = emptyList(),
    val previewIndex: Int = 0,
    val title: String = "",
    val description: String = "",
    val privacy: PrivacyOption = PrivacyOption.PUBLIC,
    val showPrivacyPicker: Boolean = false,
    val isCreating: Boolean = false,
    val activeArtTab: ArtTab = ArtTab.UPLOADS,
    val uploadedArts: List<UserArtItem> = emptyList(),
    val likedArts: List<UserArtItem> = emptyList(),
    /** Non-null while a PRIVATE curation is being created / just finished (drives the overlay). */
    val creationStatus: CreationStatus? = null,
    val creationError: String? = null,
) {
    val isValid: Boolean get() = title.isNotEmpty() && selectedArts.isNotEmpty()
    val displayedArts: List<UserArtItem> get() = if (activeArtTab == ArtTab.UPLOADS) uploadedArts else likedArts
}

// ── Mock data ─────────────────────────────────────────────────────────────────

object MockUploadData {

    val artists = listOf(
        ArtistResult("hayleyag",    "hayleyag",    "myself",                    null),
        ArtistResult("workbywade",  "workbywade",  "Wade Huston · Following",   null),
        ArtistResult("sophahem",    "sophahem",    "Sophia Ahamed · 1.2K followers", null),
        ArtistResult("lena.art",    "lena.art",    "lena · 890 followers",      null),
        ArtistResult("marco_paints","marco_paints","marco · Collector",         null),
    )

    val uploadedArts = listOf(
        UserArtItem("u1",  R.drawable.art_sample_street_poster),
        UserArtItem("u2",  R.drawable.art_sample_cosmic_swirl),
        UserArtItem("u3",  R.drawable.art_sample_fluid_purple),
        UserArtItem("u4",  R.drawable.art_sample_painted_hands),
        UserArtItem("u5",  R.drawable.art_sample_paint_brushes),
        UserArtItem("u6",  R.drawable.art_heaven),
        UserArtItem("u7",  R.drawable.art_sample_artist_outdoors),
        UserArtItem("u8",  R.drawable.art_sample_neon_corridor),
        UserArtItem("u9",  R.drawable.art_sample_brush_red),
        UserArtItem("u10", R.drawable.art_sample_typewriter),
        UserArtItem("u11", R.drawable.art_sample_pink_glitter),
        UserArtItem("u12", R.drawable.art_and_artist),
    )

    val likedArts = listOf(
        UserArtItem("l1",  R.drawable.art_sample_chrysler_building),
        UserArtItem("l2",  R.drawable.art_sample_artists_studio),
        UserArtItem("l3",  R.drawable.art_sample_street_artist),
        UserArtItem("l4",  R.drawable.art_sample_cosmic_swirl),
        UserArtItem("l5",  R.drawable.art_sample_fluid_purple),
        UserArtItem("l6",  R.drawable.art_heaven),
        UserArtItem("l7",  R.drawable.art_sample_painted_hands),
        UserArtItem("l8",  R.drawable.art_sample_paint_brushes),
        UserArtItem("l9",  R.drawable.art_sample_neon_corridor),
        UserArtItem("l10", R.drawable.art_and_artist),
        UserArtItem("l11", R.drawable.art_sample_brush_red),
        UserArtItem("l12", R.drawable.art_sample_pink_glitter),
    )
}
