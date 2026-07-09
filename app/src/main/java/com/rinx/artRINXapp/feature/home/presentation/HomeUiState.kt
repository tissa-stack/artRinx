package com.rinx.artRINXapp.feature.home.presentation

import androidx.compose.runtime.Immutable
import com.rinx.artRINXapp.feature.home.domain.model.ArtworkItem
import com.rinx.artRINXapp.feature.home.domain.model.BannerItem
import com.rinx.artRINXapp.feature.home.domain.model.CurationItem
import com.rinx.artRINXapp.feature.home.domain.model.FeedPost
import com.rinx.artRINXapp.feature.home.domain.model.ForYouItem
import com.rinx.artRINXapp.feature.home.domain.model.ShoppablePost
import com.rinx.artRINXapp.feature.upload.domain.model.CurationProgress
import com.rinx.artRINXapp.feature.upload.domain.model.UploadProgress

enum class HomeTab(val displayName: String) {
    DISCOVER("Discover"),
    SHOP("Shop"),
    FOR_YOU("For You"),
}

sealed class HomeError {
    object NoInternet : HomeError()
    data class Generic(val message: String = "") : HomeError()
}

/**
 * Infinite-scroll bookkeeping for one paginated feed. [nextPage] is the next page to request (starts
 * at 2 because the initial load fetches page 1); [endReached] stops further requests; [isLoadingMore]
 * gates concurrent loads and drives the footer spinner. [loadMoreError] is set when the next-page
 * fetch fails — it shows an inline error + Retry footer and pauses auto-loading until the user retries.
 */
@Immutable
data class PageState(
    val nextPage: Int = 2,
    val endReached: Boolean = false,
    val isLoadingMore: Boolean = false,
    val loadMoreError: HomeError? = null,
)

@Immutable
data class HomeUiState(
    val activeTab: HomeTab = HomeTab.DISCOVER,
    val bannerItems: List<BannerItem> = emptyList(),
    val newArtItems: List<ArtworkItem> = emptyList(),
    val popularCurations: List<CurationItem> = emptyList(),
    val recentlyViewed: List<ArtworkItem> = emptyList(),
    val feedItems: List<FeedPost> = emptyList(),
    val shoppableItems: List<ShoppablePost> = emptyList(),
    val forYouItems: List<ForYouItem> = emptyList(),
    /** Infinite-scroll state for the Discover vertical feed (/artworks/all). */
    val discoverPaging: PageState = PageState(),
    /** Infinite-scroll state for the Shop feed (/artworks/shop). */
    val shopPaging: PageState = PageState(),
    /** Infinite-scroll state for the For You feed (/artworks/recommended). */
    val forYouPaging: PageState = PageState(),
    // NOTE: the public upload/curation progress ROW now lives on the Create screen (CreateViewModel
    // owns display + dismiss). HomeViewModel no longer populates these — they stay null so the
    // Home-feed row plumbing (HomeScreen) is dormant. Kept only to avoid churning that plumbing.
    val uploadProgress: UploadProgress? = null,
    val curationProgress: CurationProgress? = null,
    val isLoading: Boolean = true,
    /** True while a pull-to-refresh is running (lightweight spinner, not the full-screen shimmer). */
    val isRefreshing: Boolean = false,
    val error: HomeError? = null,
    /** One-shot success message (e.g. after a public upload/curation), shown as a toast then cleared. */
    val toastMessage: String? = null,
)
