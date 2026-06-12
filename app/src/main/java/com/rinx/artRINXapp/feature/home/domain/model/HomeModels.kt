package com.rinx.artRINXapp.feature.home.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class BannerItem(
    val id: String,
    val imageUrl: String,
    val title: String,
    val artistName: String,
    /** Secondary line under the title (art description / subtitle). */
    val subtitle: String = "",
    /** Destination opened when the banner is tapped, when present. */
    val url: String? = null,
    val isSponsored: Boolean = false,
)

@Immutable
data class ArtworkItem(
    val id: String,
    val imageUrl: String,
    val title: String,
    val artistName: String,
    val artistAvatarUrl: String? = null,
)

@Immutable
data class CurationItem(
    val id: String,
    val title: String,
    val curatorHandle: String,
    val curatorName: String = "Curator",
    val curatorAvatarUrl: String? = null,
    val artworkUrls: List<String>,
    /** Artwork ids index-aligned with [artworkUrls] (for tapping a card → that art's detail). */
    val artworkIds: List<String> = emptyList(),
    val styles: String = "Painting",
    val description: String = "A carefully curated collection of remarkable artworks.",
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    /** Curation owner's user id — used to detect "this is my own curation". */
    val authorId: Int? = null,
)

@Immutable
data class FeedPost(
    val id: String,
    val artistName: String,
    val artistHandle: String,
    val artistRole: String = "Artist",
    val artistAvatarUrl: String? = null,
    val imageUrl: String,
    val title: String,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false,
    /** Uploader's user id, for opening their profile. */
    val ownerId: Int? = null,
    /** Uploader's display name (the profile that owns this art — may differ from the credited artist). */
    val ownerName: String = "",
)

@Immutable
data class ShoppablePost(
    val id: String,
    val artistName: String,
    val artistHandle: String,
    val artistRole: String = "Artist",
    val artistAvatarUrl: String? = null,
    val imageUrl: String,
    val title: String,
    val medium: String,
    val description: String,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false,
    val shopUrl: String = "",
    /** Listed price (when set); rendered as a chip on the Shop Art button. */
    val price: Double? = null,
    /** Artwork owner's user id — used to detect "this is my own art". */
    val ownerId: Int? = null,
    /** Uploader's display name (the profile that owns this art — may differ from the credited artist). */
    val ownerName: String = "",
    /** Credited artist's RINX profile id (null when they have no profile). */
    val artistId: Int? = null,
)

/** Everything the single /api/feed/discover call returns, mapped to domain models. */
@Immutable
data class HomeFeed(
    val banners: List<BannerItem>,
    val newArt: List<ArtworkItem>,
    val curations: List<CurationItem>,
    val posts: List<FeedPost>,
    val recentlyViewed: List<ArtworkItem>,
)

/** Mixed "For You" feed item — either a regular post or an inline sponsored banner. */
sealed class ForYouItem {
    @Immutable data class Post(val post: FeedPost) : ForYouItem()
    @Immutable data class Sponsored(val banner: BannerItem) : ForYouItem()
}

/** Compact sample data used only by @Preview composables (image URLs intentionally blank). */
object MockHomeData {

    val bannerItems = listOf(
        BannerItem("b1", "", "afrodroids", "by awo", isSponsored = true),
        BannerItem("b2", "", "Cosmic Swirl", "by lena", isSponsored = true),
    )

    val newArtItems = listOf(
        ArtworkItem("a1", "", "Foyer de la Danse", "Edgar Degas"),
        ArtworkItem("a2", "", "La Brioche", "Édouard Mo"),
    )

    val popularCurations = listOf(
        CurationItem(
            id = "c1", title = "everything is blue", curatorHandle = "@evanicole",
            curatorName = "Eva Nicole", artworkUrls = listOf("", "", ""),
        ),
    )

    val recentlyViewed = listOf(
        ArtworkItem("r1", "", "Evening Sunset", "vijay"),
    )

    val feedItems = listOf(
        FeedPost("f1", "Wade Huston", "@wadehuston", "Artist", null, "", "Coral Reef", likeCount = 26),
        FeedPost("f2", "vijay", "@vijay", "Gallery", null, "", "Mystical sunbeams", likeCount = 2),
    )

    val shopItems = listOf(
        ShoppablePost(
            "s1", "Wade Huston", "@wadehuston", "Artist", null, "", "Lando", "Painting",
            "Beautiful landscape inspired by the land before the storm.", likeCount = 26,
        ),
    )

    val forYouItems: List<ForYouItem> = listOf(
        ForYouItem.Post(feedItems[0]),
        ForYouItem.Sponsored(bannerItems[0]),
        ForYouItem.Post(feedItems[1]),
    )
}