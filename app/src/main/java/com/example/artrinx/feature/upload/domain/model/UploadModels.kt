package com.example.artrinx.feature.upload.domain.model

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.example.artrinx.R

// ── Enums ─────────────────────────────────────────────────────────────────────

enum class PrivacyOption { PUBLIC, PRIVATE }
enum class ArtTab { UPLOADS, LIKED }

// ── Artist ────────────────────────────────────────────────────────────────────

@Immutable
data class ArtistResult(
    val handle: String,
    val displayName: String,
    val subtitle: String,               // "myself" / "Wade Huston · Following" etc.
    @DrawableRes val avatarRes: Int? = null,
)

// ── Upload Art form ───────────────────────────────────────────────────────────

data class ArtFormState(
    val imageUri: Uri? = null,
    val title: String = "",
    val description: String = "",
    val selectedArtist: ArtistResult? = null,
    val selectedMedium: String? = null,
    val tags: List<String> = emptyList(),
    val shopLink: String = "",
    val privacy: PrivacyOption = PrivacyOption.PUBLIC,
    val showMediumPicker: Boolean = false,
    val showPrivacyPicker: Boolean = false,
    val currentTagInput: String = "",
    val isTitleError: Boolean = false,
    val isDescriptionError: Boolean = false,
    val isUploading: Boolean = false,
    val artistSearchQuery: String = "",
) {
    val isValid: Boolean get() = title.isNotEmpty()
}

// ── New Curation form ─────────────────────────────────────────────────────────

@Immutable
data class UserArtItem(
    val id: String,
    @DrawableRes val imageRes: Int,
    val isSelected: Boolean = false,
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
    val uploadedArts: List<UserArtItem> = MockUploadData.uploadedArts,
    val likedArts: List<UserArtItem> = MockUploadData.likedArts,
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
