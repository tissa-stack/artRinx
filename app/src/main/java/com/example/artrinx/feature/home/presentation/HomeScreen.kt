package com.example.artrinx.feature.home.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.artrinx.core.theme.ArtRinxTheme
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.home.domain.model.MockHomeData
import com.example.artrinx.feature.home.presentation.components.ArtworkCard
import com.example.artrinx.feature.home.presentation.components.BottomNavBar
import com.example.artrinx.feature.home.presentation.components.CollectionCard
import com.example.artrinx.feature.home.presentation.components.DiscoverFeedItem
import com.example.artrinx.feature.home.presentation.components.FeaturedCarousel
import com.example.artrinx.feature.home.presentation.components.RecentlyViewedCard
import com.example.artrinx.feature.home.presentation.components.SectionHeader
import com.example.artrinx.feature.home.presentation.components.ShoppableFeedItem
import com.example.artrinx.feature.home.presentation.components.TopTabs
import com.example.artrinx.feature.home.presentation.components.shimmer.BannerShimmer
import com.example.artrinx.feature.home.presentation.components.shimmer.CollectionShimmer
import com.example.artrinx.feature.home.presentation.components.shimmer.FeedShimmer
import com.example.artrinx.feature.home.presentation.components.shimmer.HorizontalListShimmer
import com.example.artrinx.feature.home.presentation.components.state.EmptyView
import com.example.artrinx.feature.home.presentation.components.state.ErrorView

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    HomeScreenContent(
        uiState = uiState,
        onTabSelected = viewModel::onTabSelected,
        onRetry = viewModel::onRetry,
        onLike = viewModel::onLikeToggled,
        onBookmark = viewModel::onBookmarkToggled,
        onShopLike = viewModel::onShopLikeToggled,
        onShopBookmark = viewModel::onShopBookmarkToggled,
    )
}

@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onTabSelected: (HomeTab) -> Unit,
    onRetry: () -> Unit,
    onLike: (String) -> Unit,
    onBookmark: (String) -> Unit,
    onShopLike: (String) -> Unit = {},
    onShopBookmark: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()

    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomNavBar(activeRoute = "home")
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        HomeContent(
            uiState = uiState,
            isDarkTheme = isDark,
            onTabSelected = onTabSelected,
            onRetry = onRetry,
            onLike = onLike,
            onBookmark = onBookmark,
            onShopLike = onShopLike,
            onShopBookmark = onShopBookmark,
            modifier = Modifier.fillMaxSize(),
            bottomPadding = innerPadding,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    isDarkTheme: Boolean,
    onTabSelected: (HomeTab) -> Unit,
    onRetry: () -> Unit,
    onLike: (String) -> Unit,
    onBookmark: (String) -> Unit,
    onShopLike: (String) -> Unit = {},
    onShopBookmark: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    bottomPadding: PaddingValues = PaddingValues(),
) {
    val d = LocalDimens.current
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = bottomPadding.calculateBottomPadding()),
    ) {
        stickyHeader(key = "top-tabs") {
            TopTabs(
                activeTab = uiState.activeTab,
                onTabSelected = onTabSelected,
                isDarkTheme = isDarkTheme,
            )
        }

        // ── Shop tab: shoppable feed only ─────────────────────────────
        if (uiState.activeTab == HomeTab.SHOP) {
            if (uiState.isLoading) {
                items(count = 3, key = { "shop-shimmer-$it" }) { FeedShimmer() }
            } else if (uiState.shoppableItems.isEmpty()) {
                item(key = "shop-empty") {
                    EmptyView(
                        icon = Icons.Outlined.Collections,
                        title = "No shoppable art yet",
                        subtitle = "Check back later for art you can buy.",
                    )
                }
            } else {
                items(uiState.shoppableItems, key = { it.id }) { post ->
                    ShoppableFeedItem(
                        post = post,
                        onLike = { onShopLike(post.id) },
                        onBookmark = { onShopBookmark(post.id) },
                    )
                }
            }
            return@LazyColumn
        }

        // ── Discover / For You tab ─────────────────────────────────────
        if (uiState.error != null) {
            item(key = "error-state") {
                ErrorView(
                    error = uiState.error,
                    onRetry = onRetry,
                    modifier = Modifier.fillParentMaxHeight(0.7f),
                )
            }
        } else {
            // ── Banner ─────────────────────────────────────────────────
            item(key = "banner") {
                when {
                    uiState.isLoading -> BannerShimmer()
                    uiState.bannerItems.isEmpty() -> EmptyView(
                        icon = Icons.Outlined.Image,
                        title = "No featured art",
                        subtitle = "Check back later for new artwork.",
                    )
                    else -> FeaturedCarousel(items = uiState.bannerItems)
                }
            }

            // ── New Art For You ────────────────────────────────────────
            item(key = "new-art-header") {
                SectionHeader(title = "New Art For You", onSeeAll = {})
            }
            item(key = "new-art-content") {
                when {
                    uiState.isLoading -> HorizontalListShimmer()
                    uiState.newArtItems.isEmpty() -> EmptyView(
                        icon = Icons.Outlined.Palette,
                        title = "No new art yet",
                        subtitle = "Explore art you might like.",
                    )
                    else -> LazyRow(
                        contentPadding = PaddingValues(
                            horizontal = Spacing.md,
                            vertical = Spacing.xs,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        items(uiState.newArtItems, key = { it.id }) { item ->
                            ArtworkCard(item = item)
                        }
                    }
                }
            }

            // ── Popular Curations ──────────────────────────────────────
            item(key = "curations-header") {
                SectionHeader(title = "Popular Curations", onSeeAll = {})
            }
            item(key = "curations-content") {
                when {
                    uiState.isLoading -> CollectionShimmer()
                    uiState.popularCurations.isEmpty() -> EmptyView(
                        icon = Icons.Outlined.Collections,
                        title = "No curations yet",
                        subtitle = "Follow curators to see their collections.",
                    )
                    else -> LazyRow(
                        contentPadding = PaddingValues(
                            horizontal = Spacing.md,
                            vertical = Spacing.sm,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        items(uiState.popularCurations, key = { it.id }) { item ->
                            CollectionCard(item = item)
                        }
                    }
                }
            }

            // ── Recently Viewed ────────────────────────────────────────
            item(key = "recent-header") {
                SectionHeader(title = "Recently Viewed", onSeeAll = {})
            }
            item(key = "recent-content") {
                when {
                    uiState.isLoading -> HorizontalListShimmer()
                    uiState.recentlyViewed.isEmpty() -> EmptyView(
                        icon = Icons.Outlined.History,
                        title = "Nothing viewed yet",
                        subtitle = "Start exploring to see artwork here.",
                    )
                    else -> LazyRow(
                        contentPadding = PaddingValues(
                            horizontal = Spacing.md,
                            vertical = Spacing.xs,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        items(uiState.recentlyViewed, key = { it.id }) { item ->
                            RecentlyViewedCard(item = item)
                        }
                    }
                }
            }

            // ── Discover Feed ──────────────────────────────────────────
            item(key = "feed-header") {
                SectionHeader(title = "Discover")
            }

            if (uiState.isLoading) {
                items(count = 3, key = { "feed-shimmer-$it" }) {
                    FeedShimmer()
                }
            } else if (uiState.feedItems.isEmpty()) {
                item(key = "feed-empty") {
                    EmptyView(
                        icon = Icons.Outlined.DynamicFeed,
                        title = "Nothing in your feed",
                        subtitle = "Follow artists to see their work here.",
                    )
                }
            } else {
                items(uiState.feedItems, key = { it.id }) { post ->
                    DiscoverFeedItem(
                        post = post,
                        onLike = { onLike(post.id) },
                        onBookmark = { onBookmark(post.id) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    ArtRinxTheme {
        HomeScreenContent(
            uiState = HomeUiState(
                isLoading = false,
                bannerItems = MockHomeData.bannerItems,
                newArtItems = MockHomeData.newArtItems,
                popularCurations = MockHomeData.popularCurations,
                recentlyViewed = MockHomeData.recentlyViewed,
                feedItems = MockHomeData.feedItems,
            ),
            onTabSelected = {},
            onRetry = {},
            onLike = {},
            onBookmark = {},
        )
    }
}
