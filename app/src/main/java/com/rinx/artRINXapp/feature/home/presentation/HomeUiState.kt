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
    /** In-progress/just-finished PUBLIC upload, surfaced as a row at the top of the feed. */
    val uploadProgress: UploadProgress? = null,
    /** In-progress/just-finished PUBLIC curation create, surfaced at the top of the feed. */
    val curationProgress: CurationProgress? = null,
    val isLoading: Boolean = true,
    /** True while a pull-to-refresh is running (lightweight spinner, not the full-screen shimmer). */
    val isRefreshing: Boolean = false,
    val error: HomeError? = null,
)
