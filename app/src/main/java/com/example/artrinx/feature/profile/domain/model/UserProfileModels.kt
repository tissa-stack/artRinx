package com.example.artrinx.feature.profile.domain.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.example.artrinx.R
import com.example.artrinx.feature.search.domain.model.CardHeight

enum class ProfileTab(val displayName: String) {
    ART("Art"),
    CURATIONS("Curations"),
    LIKED("Liked"),
}

@Immutable
data class UserProfileData(
    val handle: String,
    val displayName: String,
    val role: String,
    val bio: String = "",
    val website: String = "",
    @param:DrawableRes val avatarRes: Int? = null,
    val artCount: Int = 0,
    val curationCount: Int = 0,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
)

@Immutable
data class ProfileArtItem(
    val id: String,
    @param:DrawableRes val imageRes: Int,
    val title: String,
    val artistName: String,
    val isPrivate: Boolean = false,
    val cardHeight: CardHeight = CardHeight.MEDIUM,
)

@Immutable
data class ProfileCurationItem(
    val id: String,
    val title: String,
    val handle: String,
    val artworkRes: List<Int>,
    val isPrivate: Boolean = false,
)

object MockUserProfileData {

    val profile = UserProfileData(
        handle = "evanicole",
        displayName = "Eva Nicole",
        role = "Collector",
        bio = "Hi my name is Eva Nicole and I love art. My favorite art is contemporary and conceptual art. My passion is collecting bold, expressive pieces.",
        website = "www.evanicole.com",
        artCount = 8,
        curationCount = 12,
        followerCount = 565,
        followingCount = 323,
    )

    val artItems = listOf(
        ProfileArtItem("pa1", R.drawable.art_sample_street_poster, "Foyer de la Danse", "Edgar Degas", isPrivate = true, cardHeight = CardHeight.TALL),
        ProfileArtItem("pa2", R.drawable.art_sample_cosmic_swirl, "Untitled", "Sophia Ahamed", cardHeight = CardHeight.MEDIUM),
        ProfileArtItem("pa3", R.drawable.art_sample_paint_brushes, "La Brioche", "Édouard Manet", cardHeight = CardHeight.SHORT),
        ProfileArtItem("pa4", R.drawable.art_sample_fluid_purple, "Fluid Dreams", "Marco Vanni", cardHeight = CardHeight.MEDIUM),
        ProfileArtItem("pa5", R.drawable.art_heaven, "Heaven's Gate", "Aria Chen", cardHeight = CardHeight.TALL),
        ProfileArtItem("pa6", R.drawable.art_sample_artist_outdoors, "Morning Walk", "Wade H.", cardHeight = CardHeight.MEDIUM),
        ProfileArtItem("pa7", R.drawable.art_sample_neon_corridor, "Neon Passage", "Saketh R.", isPrivate = true, cardHeight = CardHeight.SHORT),
        ProfileArtItem("pa8", R.drawable.art_sample_brush_red, "Red Study", "Carlos V.", cardHeight = CardHeight.MEDIUM),
    )

    val curations = listOf(
        ProfileCurationItem(
            "pc1", "mantle", "@evanicole",
            listOf(R.drawable.art_sample_chrysler_building, R.drawable.art_sample_cosmic_swirl, R.drawable.art_sample_fluid_purple),
        ),
        ProfileCurationItem(
            "pc2", "bedroom", "@evanicole",
            listOf(R.drawable.art_sample_artist_outdoors, R.drawable.art_sample_street_poster, R.drawable.art_sample_painted_hands),
            isPrivate = true,
        ),
        ProfileCurationItem(
            "pc3", "dinner party", "@evanicole",
            listOf(R.drawable.art_sample_chrysler_building, R.drawable.art_sample_neon_corridor, R.drawable.art_sample_brush_red),
        ),
        ProfileCurationItem(
            "pc4", "girly", "@evanicole",
            listOf(R.drawable.art_sample_pink_glitter, R.drawable.art_sample_fluid_purple, R.drawable.art_sample_paint_brushes),
        ),
    )

    val likedItems = listOf(
        ProfileArtItem("pl1", R.drawable.art_sample_street_poster, "Foyer de la Danse", "Edgar Degas", isPrivate = true, cardHeight = CardHeight.TALL),
        ProfileArtItem("pl2", R.drawable.art_sample_cosmic_swirl, "Untitled", "Sophia Ahamed", cardHeight = CardHeight.MEDIUM),
        ProfileArtItem("pl3", R.drawable.art_sample_paint_brushes, "La Brioche", "Édouard Manet", cardHeight = CardHeight.SHORT),
        ProfileArtItem("pl4", R.drawable.art_sample_fluid_purple, "Bauerngar", "Gustav Klimt", cardHeight = CardHeight.MEDIUM),
        ProfileArtItem("pl5", R.drawable.art_sample_pink_glitter, "Pink Glitter", "Mira Stone", cardHeight = CardHeight.TALL),
        ProfileArtItem("pl6", R.drawable.sample_image_1, "Evening Sunset", "Vijay", cardHeight = CardHeight.MEDIUM),
    )
}