package com.example.artrinx.feature.home.domain.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.example.artrinx.R

@Immutable
data class BannerItem(
    val id: String,
    @DrawableRes val imageRes: Int,
    val title: String,
    val artistName: String,
    val isSponsored: Boolean = false,
)

@Immutable
data class ArtworkItem(
    val id: String,
    @DrawableRes val imageRes: Int,
    val title: String,
    val artistName: String,
    @DrawableRes val artistAvatarRes: Int? = null,
)

@Immutable
data class CurationItem(
    val id: String,
    val title: String,
    val curatorHandle: String,
    @DrawableRes val curatorAvatarRes: Int? = null,
    val artworkRes: List<Int>,
)

@Immutable
data class FeedPost(
    val id: String,
    val artistName: String,
    val artistHandle: String,
    val artistRole: String = "Artist",
    @DrawableRes val artistAvatarRes: Int? = null,
    @DrawableRes val imageRes: Int,
    val title: String,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
)

object MockHomeData {

    val bannerItems = listOf(
        BannerItem("b1", R.drawable.art_sample_neon_corridor, "afrodroids", "by awo", true),
        BannerItem("b2", R.drawable.art_sample_cosmic_swirl, "Cosmic Swirl", "by lena", true),
        BannerItem("b3", R.drawable.art_sample_fluid_purple, "Fluid Motion", "by marco", true),
        BannerItem("b4", R.drawable.art_heaven, "Heaven's Gate", "by aria", true),
    )

    val newArtItems = listOf(
        ArtworkItem("a1", R.drawable.art_sample_street_poster, "Foyer de la Danse", "Edgar Degas"),
        ArtworkItem("a2", R.drawable.art_sample_artists_studio, "La Brioche", "Édouard Mo"),
        ArtworkItem("a3", R.drawable.art_sample_painted_hands, "Painted Hands", "Yuki Sato"),
        ArtworkItem("a4", R.drawable.art_sample_brush_red, "Red Brushstroke", "Carlos V"),
        ArtworkItem("a5", R.drawable.art_sample_typewriter, "Typewriter No.5", "Ana Lima"),
        ArtworkItem("a6", R.drawable.art_sample_street_artist, "Street Series", "Raj Patel"),
    )

    val popularCurations = listOf(
        CurationItem(
            id = "c1",
            title = "everything is blue",
            curatorHandle = "@evanicole",
            artworkRes = listOf(
                R.drawable.art_sample_chrysler_building,
                R.drawable.art_sample_cosmic_swirl,
                R.drawable.art_sample_fluid_purple,
            ),
        ),
        CurationItem(
            id = "c2",
            title = "European Greats",
            curatorHandle = "@wadahuston",
            artworkRes = listOf(
                R.drawable.art_sample_artist_outdoors,
                R.drawable.art_sample_street_poster,
                R.drawable.art_sample_painted_hands,
            ),
        ),
        CurationItem(
            id = "c3",
            title = "Abstract Visions",
            curatorHandle = "@mirastone",
            artworkRes = listOf(
                R.drawable.art_sample_pink_glitter,
                R.drawable.art_sample_fluid_purple,
                R.drawable.art_sample_paint_brushes,
            ),
        ),
        CurationItem(
            id = "c4",
            title = "Street & Soul",
            curatorHandle = "@thecollector",
            artworkRes = listOf(
                R.drawable.art_sample_street_artist,
                R.drawable.art_sample_neon_corridor,
                R.drawable.art_sample_brush_red,
            ),
        ),
    )

    val recentlyViewed = listOf(
        ArtworkItem("r1", R.drawable.sample_image_1, "Evening Sunset", "vijay"),
        ArtworkItem("r2", R.drawable.sample_image_2, "City Lights", "nadia"),
        ArtworkItem("r3", R.drawable.sample_image_3, "Morning Mist", "leon"),
        ArtworkItem("r4", R.drawable.art_sample_chrysler_building, "Chrysler", "aiden"),
        ArtworkItem("r5", R.drawable.art_and_artist, "The Artist", "priya"),
    )

    val feedItems = listOf(
        FeedPost(
            id = "f1", artistName = "Wade Huston", artistHandle = "@wadehuston",
            artistRole = "Artist",
            imageRes = R.drawable.art_sample_fluid_purple,
            title = "Coral Reef", likeCount = 26,
        ),
        FeedPost(
            id = "f2", artistName = "vijay", artistHandle = "@vijay",
            artistRole = "Gallery",
            imageRes = R.drawable.sample_image_1,
            title = "Mystical sunbeams, rays through branches in foggy morning field",
            likeCount = 2,
        ),
        FeedPost(
            id = "f3", artistName = "vamshikrishna", artistHandle = "@Gallery",
            artistRole = "Gallery",
            imageRes = R.drawable.sample_image_2,
            title = "evening sunset between city towers",
            likeCount = 0,
        ),
        FeedPost(
            id = "f4", artistName = "saketh", artistHandle = "@saketh",
            artistRole = "Artist",
            imageRes = R.drawable.art_sample_neon_corridor,
            title = "Neon corridor at midnight",
            likeCount = 14, commentCount = 2,
        ),
        FeedPost(
            id = "f5", artistName = "lena", artistHandle = "@lena.art",
            artistRole = "Artist",
            imageRes = R.drawable.art_sample_cosmic_swirl,
            title = "Cosmic Swirl — acrylic on canvas",
            likeCount = 23, commentCount = 4,
        ),
        FeedPost(
            id = "f6", artistName = "marco", artistHandle = "@marco_paints",
            artistRole = "Collector",
            imageRes = R.drawable.art_heaven,
            title = "Heaven's Gate — oil on board",
            likeCount = 31, commentCount = 7,
        ),
        FeedPost(
            id = "f7", artistName = "yuki", artistHandle = "@yuki.art",
            artistRole = "Artist",
            imageRes = R.drawable.art_sample_painted_hands,
            title = "Painted Hands series",
            likeCount = 5, commentCount = 2,
        ),
        FeedPost(
            id = "f8", artistName = "carlos", artistHandle = "@carlos_v",
            artistRole = "Artist",
            imageRes = R.drawable.art_sample_brush_red,
            title = "Red Brushstroke study",
            likeCount = 12, commentCount = 3,
        ),
        FeedPost(
            id = "f9", artistName = "ana", artistHandle = "@ana.lima",
            artistRole = "Gallery",
            imageRes = R.drawable.art_sample_typewriter,
            title = "Typewriter studies No. 5",
            likeCount = 7,
        ),
        FeedPost(
            id = "f10", artistName = "raj", artistHandle = "@rajpatel",
            artistRole = "Collector",
            imageRes = R.drawable.sample_image_3,
            title = "Morning light, Kolkata streets",
            likeCount = 19, commentCount = 5,
        ),
    )
}
