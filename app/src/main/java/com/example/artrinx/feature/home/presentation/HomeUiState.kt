package com.example.artrinx.feature.home.presentation

import androidx.compose.runtime.Immutable
import com.example.artrinx.feature.home.domain.model.ArtworkItem
import com.example.artrinx.feature.home.domain.model.BannerItem
import com.example.artrinx.feature.home.domain.model.CurationItem
import com.example.artrinx.feature.home.domain.model.FeedPost
import com.example.artrinx.feature.home.domain.model.ShoppablePost

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
    val isLoading: Boolean = true,
    val error: HomeError? = null,
)
