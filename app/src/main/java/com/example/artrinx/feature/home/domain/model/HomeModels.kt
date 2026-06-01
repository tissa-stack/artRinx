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
    val curatorName: String = "Curator",
    @DrawableRes val curatorAvatarRes: Int? = null,
    val artworkRes: List<Int>,
    val styles: String = "Painting",
    val description: String = "A carefully curated collection of remarkable artworks.",
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

@Immutable
data class ShoppablePost(
    val id: String,
    val artistName: String,
    val artistHandle: String,
    val artistRole: String = "Artist",
    @DrawableRes val artistAvatarRes: Int? = null,
    @DrawableRes val imageRes: Int,
    val title: String,
    val medium: String,
    val description: String,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
)

/** Mixed "For You" feed item — either a regular post or an inline sponsored banner. */
sealed class ForYouItem {
    @Immutable data class Post(val post: FeedPost) : ForYouItem()
    @Immutable data class Sponsored(val banner: BannerItem) : ForYouItem()
}

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
            curatorName = "Eva Nicole",
            artworkRes = listOf(
                R.drawable.art_sample_chrysler_building,
                R.drawable.art_sample_cosmic_swirl,
                R.drawable.art_sample_fluid_purple,
            ),
            styles = "Photography, Painting, Conceptual",
            description = "A journey through shades of blue across mediums and movements — from midnight cityscapes to oceanic abstractions.",
        ),
        CurationItem(
            id = "c2",
            title = "European Greats",
            curatorHandle = "@wadahuston",
            curatorName = "Wade Huston",
            artworkRes = listOf(
                R.drawable.art_sample_artist_outdoors,
                R.drawable.art_sample_street_poster,
                R.drawable.art_sample_painted_hands,
            ),
            styles = "Painting, Sculpture, Conceptual, Digital Art",
            description = "Swirling fish schools, bright corals and sparkling brimming life. A portion of the sale of this piece goes to saving coral reefs.",
        ),
        CurationItem(
            id = "c3",
            title = "Abstract Visions",
            curatorHandle = "@mirastone",
            curatorName = "Mira Stone",
            artworkRes = listOf(
                R.drawable.art_sample_pink_glitter,
                R.drawable.art_sample_fluid_purple,
                R.drawable.art_sample_paint_brushes,
            ),
            styles = "Abstract, Mixed Media, Digital",
            description = "A celebration of colour and form without boundaries — art that asks you to feel before you think.",
        ),
        CurationItem(
            id = "c4",
            title = "Street & Soul",
            curatorHandle = "@thecollector",
            curatorName = "The Collector",
            artworkRes = listOf(
                R.drawable.art_sample_street_artist,
                R.drawable.art_sample_neon_corridor,
                R.drawable.art_sample_brush_red,
            ),
            styles = "Street Art, Photography, Graffiti",
            description = "Raw energy captured from the streets — art born in alleys, underpasses, and forgotten walls.",
        ),
    )

    val recentlyViewed = listOf(
        ArtworkItem("r1", R.drawable.sample_image_1, "Evening Sunset", "vijay"),
        ArtworkItem("r2", R.drawable.sample_image_2, "City Lights", "nadia"),
        ArtworkItem("r3", R.drawable.sample_image_3, "Morning Mist", "leon"),
        ArtworkItem("r4", R.drawable.art_sample_chrysler_building, "Chrysler", "aiden"),
        ArtworkItem("r5", R.drawable.art_and_artist, "The Artist", "priya"),
    )

    val shopItems = listOf(
        ShoppablePost(
            "s1", "Wade Huston", "@wadehuston", "Artist", null,
            R.drawable.art_sample_artist_outdoors, "Lando", "Painting",
            "Beautiful landscape inspired by the land before the storm came through the valley", 26,
        ),
        ShoppablePost(
            "s2", "Wade Huston", "@wadehuston", "Artist", null,
            R.drawable.art_and_artist, "Plumes", "Painting",
            "Swirling fish schools, bright corals and sparkling underwater light", 26,
        ),
        ShoppablePost(
            "s3", "lena", "@lena.art", "Artist", null,
            R.drawable.art_sample_cosmic_swirl, "Cosmic Drift", "Acrylic",
            "Abstract cosmic series exploring the vastness of space and colour", 14,
        ),
        ShoppablePost(
            "s4", "marco", "@marco_paints", "Collector", null,
            R.drawable.art_sample_fluid_purple, "Fluid No. III", "Oil on Canvas",
            "Third instalment in the fluid motion series — deep purples and oceanic blues", 8,
        ),
        ShoppablePost(
            "s5", "aria", "@aria.creates", "Artist", null,
            R.drawable.art_heaven, "Heaven's Gate", "Oil on Board",
            "Inspired by classical mythology, painted over three months in natural light", 31,
        ),
        ShoppablePost(
            "s6", "saketh", "@saketh", "Gallery", null,
            R.drawable.art_sample_neon_corridor, "Neon Passage", "Digital Print",
            "Limited edition digital artwork capturing urban night life through a neon lens", 19,
        ),
        ShoppablePost(
            "s7", "yuki", "@yuki.art", "Artist", null,
            R.drawable.art_sample_painted_hands, "Hands I", "Watercolour",
            "Study of human gesture — the first in a series of twelve hand studies", 5,
        ),
        ShoppablePost(
            "s8", "carlos", "@carlos_v", "Artist", null,
            R.drawable.art_sample_brush_red, "Red Study", "Mixed Media",
            "Exploration of the colour red across materials — oil, pigment ink, and wax", 12,
        ),
    )

    val forYouItems: List<ForYouItem> by lazy {
        // Interleave regular posts with sponsored banners
        val posts = feedItems
        val banners = bannerItems
        buildList {
            add(ForYouItem.Post(posts[0]))
            add(ForYouItem.Sponsored(banners[0]))
            add(ForYouItem.Post(posts[1]))
            add(ForYouItem.Post(posts[2]))
            add(ForYouItem.Sponsored(banners[1]))
            add(ForYouItem.Post(posts[3]))
            add(ForYouItem.Post(posts[4]))
            add(ForYouItem.Sponsored(banners[2]))
            add(ForYouItem.Post(posts[5]))
            add(ForYouItem.Post(posts[6]))
        }
    }

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
