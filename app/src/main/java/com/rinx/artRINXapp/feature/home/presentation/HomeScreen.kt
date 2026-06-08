package com.rinx.artRINXapp.feature.home.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.core.push.NotificationPermissionEffect
import com.rinx.artRINXapp.core.tour.TourTarget
import com.rinx.artRINXapp.core.tour.TourViewModel
import com.rinx.artRINXapp.core.tour.TourStep
import com.rinx.artRINXapp.core.theme.ArtRinxTheme
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.home.domain.model.MockHomeData
import com.rinx.artRINXapp.feature.home.presentation.components.ArtworkCard
import com.rinx.artRINXapp.feature.home.presentation.components.BottomNavBar
import com.rinx.artRINXapp.feature.home.presentation.components.CollectionCard
import com.rinx.artRINXapp.feature.home.presentation.components.DiscoverFeedItem
import com.rinx.artRINXapp.feature.home.presentation.components.FeaturedCarousel
import com.rinx.artRINXapp.feature.home.presentation.components.RecentlyViewedCard
import com.rinx.artRINXapp.feature.home.presentation.components.SectionHeader
import com.rinx.artRINXapp.feature.home.domain.model.ForYouItem
import com.rinx.artRINXapp.feature.home.presentation.components.FeaturedCarouselItem
import com.rinx.artRINXapp.feature.home.presentation.components.ShoppableFeedItem
import com.rinx.artRINXapp.feature.home.presentation.components.CurationProgressRow
import com.rinx.artRINXapp.feature.home.presentation.components.TopTabs
import com.rinx.artRINXapp.feature.home.presentation.components.UploadProgressRow
import com.rinx.artRINXapp.feature.home.presentation.components.AddToCurationSheet
import com.rinx.artRINXapp.feature.upload.domain.model.CurationSource
import com.rinx.artRINXapp.feature.home.presentation.components.shimmer.BannerShimmer
import com.rinx.artRINXapp.feature.home.presentation.components.shimmer.CollectionShimmer
import com.rinx.artRINXapp.feature.home.presentation.components.shimmer.FeedShimmer
import com.rinx.artRINXapp.feature.home.presentation.components.shimmer.HorizontalListShimmer
import com.rinx.artRINXapp.feature.home.presentation.components.state.EmptyView
import com.rinx.artRINXapp.feature.home.presentation.components.state.ErrorView

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    reselectTick: Int = 0,
    onReselect: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToCurationDetail: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNewCuration: () -> Unit = {},
    onOpenProfile: (Int) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    // First-launch tour (global overlay lives above the NavHost; here we just start it, mirror the
    // active flag for bounds-reporting, and keep the Home segment in sync with the tour step).
    val tour: TourViewModel = hiltViewModel()
    val tourState by tour.state.collectAsState()
    LaunchedEffect(Unit) { tour.startIfFirstTime() }
    LaunchedEffect(tourState.active, tourState.step) {
        if (tourState.active) segmentForTourStep(tourState.step)?.let(viewModel::onTabSelected)
    }

    // Home is the post-login / post-registration landing — ask for notification permission here,
    // but not while the first-launch tour is up (avoid the OS dialog covering the coach-marks).
    if (!tourState.active) NotificationPermissionEffect()

    val tourActive = tourState.active
    HomeScreenContent(
        uiState = uiState,
        reselectTick = reselectTick,
        onReselect = onReselect,
        onTabSelected = viewModel::onTabSelected,
        onTabBounds = if (tourActive) {
            { tab, rect -> tour.report(tab.toTourTarget(), rect) }
        } else {
            null
        },
        onItemBounds = if (tourActive) {
            { route, rect -> route.toTourTarget()?.let { tour.report(it, rect) } }
        } else {
            null
        },
        onRetry = viewModel::onRetry,
        onLike = viewModel::onLikeToggled,
        onShopLike = viewModel::onShopLikeToggled,
        onRefresh = viewModel::refresh,
        onRetryUpload = viewModel::onRetryUpload,
        onDismissUpload = viewModel::onDismissUpload,
        onRetryCuration = viewModel::onRetryCuration,
        onDismissCuration = viewModel::onDismissCuration,
        onNavigateToSearch        = onNavigateToSearch,
        onNavigateToCreate        = onNavigateToCreate,
        onNavigateToNotifications = onNavigateToNotifications,
        onNavigateToDetail        = onNavigateToDetail,
        onNavigateToCurationDetail = onNavigateToCurationDetail,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToNewCuration = onNavigateToNewCuration,
        onOpenProfile = onOpenProfile,
    )
}

@Composable
fun HomeScreenContent(
    uiState: HomeUiState,
    onTabSelected: (HomeTab) -> Unit,
    onRetry: () -> Unit,
    onLike: (String) -> Unit,
    reselectTick: Int = 0,
    onReselect: () -> Unit = {},
    onTabBounds: ((HomeTab, Rect) -> Unit)? = null,
    onItemBounds: ((String, Rect) -> Unit)? = null,
    onShopLike: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    onRetryUpload: () -> Unit = {},
    onDismissUpload: () -> Unit = {},
    onRetryCuration: () -> Unit = {},
    onDismissCuration: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToCurationDetail: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNewCuration: () -> Unit = {},
    onOpenProfile: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()

    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomNavBar(
                activeRoute = "home",
                onNavigate = { route ->
                    when (route) {
                        "home"          -> onReselect()
                        "search"        -> onNavigateToSearch()
                        "create"        -> onNavigateToCreate()
                        "notifications" -> onNavigateToNotifications()
                        "profile"       -> onNavigateToProfile()
                    }
                },
                onItemBounds = onItemBounds,
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        HomeContent(
            uiState = uiState,
            isDarkTheme = isDark,
            onTabSelected = onTabSelected,
            onRetry = onRetry,
            onLike = onLike,
            reselectTick = reselectTick,
            onShopLike = onShopLike,
            onRefresh = onRefresh,
            onRetryUpload = onRetryUpload,
            onDismissUpload = onDismissUpload,
            onRetryCuration = onRetryCuration,
            onDismissCuration = onDismissCuration,
            onNavigateToDetail = onNavigateToDetail,
            onNavigateToCurationDetail = onNavigateToCurationDetail,
            onNavigateToNewCuration = onNavigateToNewCuration,
            onOpenProfile = onOpenProfile,
            onTabBounds = onTabBounds,
            modifier = Modifier.fillMaxSize(),
            bottomPadding = innerPadding,
        )
    }
}

private fun String.toTourTarget(): TourTarget? = when (this) {
    "home" -> TourTarget.HOME_NAV
    "create" -> TourTarget.CREATE_NAV
    else -> null
}

private fun HomeTab.toTourTarget(): TourTarget = when (this) {
    HomeTab.DISCOVER -> TourTarget.DISCOVER_TAB
    HomeTab.SHOP -> TourTarget.SHOP_TAB
    HomeTab.FOR_YOU -> TourTarget.FORYOU_TAB
}

/** Home top-segment to show for each tour step (null = leave the segment unchanged). */
private fun segmentForTourStep(step: Int): HomeTab? = when (TourStep.ordered.getOrNull(step)) {
    TourStep.SHOP -> HomeTab.SHOP
    TourStep.FOR_YOU -> HomeTab.FOR_YOU
    TourStep.WELCOME, TourStep.DISCOVER, TourStep.CREATE -> HomeTab.DISCOVER
    else -> null
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    isDarkTheme: Boolean,
    onTabSelected: (HomeTab) -> Unit,
    onRetry: () -> Unit,
    onLike: (String) -> Unit,
    reselectTick: Int = 0,
    onShopLike: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    onRetryUpload: () -> Unit = {},
    onDismissUpload: () -> Unit = {},
    onRetryCuration: () -> Unit = {},
    onDismissCuration: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToCurationDetail: (String) -> Unit = {},
    onNavigateToNewCuration: () -> Unit = {},
    onOpenProfile: (Int) -> Unit = {},
    onTabBounds: ((HomeTab, Rect) -> Unit)? = null,
    modifier: Modifier = Modifier,
    bottomPadding: PaddingValues = PaddingValues(),
) {
    val d = LocalDimens.current
    var addToCurationSource by remember { mutableStateOf<CurationSource?>(null) }
    // Each tab keeps its own scroll position so switching tabs doesn't carry the scroll over.
    val discoverListState = rememberLazyListState()
    val shopListState = rememberLazyListState()
    val forYouListState = rememberLazyListState()
    val listState = when (uiState.activeTab) {
        HomeTab.DISCOVER -> discoverListState
        HomeTab.SHOP -> shopListState
        HomeTab.FOR_YOU -> forYouListState
    }

    // Re-tapping the Home tab while already on Home scrolls the active list back to the top.
    LaunchedEffect(reselectTick) {
        if (reselectTick > 0) listState.animateScrollToItem(0)
    }

    // When a public upload/curation progress row appears (e.g. the user just landed here after
    // creating one), scroll the feed to the very top so the progress section is actually visible
    // and not left off-screen above a remembered scroll position.
    val hasProgress = uiState.uploadProgress != null || uiState.curationProgress != null
    LaunchedEffect(hasProgress) {
        if (hasProgress) listState.animateScrollToItem(0)
    }

    Box(modifier = modifier) {
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding.calculateBottomPadding()),
    ) {
        stickyHeader(key = "top-tabs") {
            TopTabs(
                activeTab = uiState.activeTab,
                onTabSelected = onTabSelected,
                isDarkTheme = isDarkTheme,
                onTabBounds = onTabBounds,
            )
        }

        // ── Uploading row (public uploads) — pinned at the very top of the feed ──
        uiState.uploadProgress?.let { progress ->
            item(key = "upload-progress") {
                UploadProgressRow(
                    progress = progress,
                    onRetry = onRetryUpload,
                    onDismiss = onDismissUpload,
                )
            }
        }

        // ── Creating row (public curations) — directly below the upload row ──
        uiState.curationProgress?.let { progress ->
            item(key = "curation-progress") {
                CurationProgressRow(
                    progress = progress,
                    onRetry = onRetryCuration,
                    onDismiss = onDismissCuration,
                )
            }
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
                        onClick = { onNavigateToDetail(post.id) },
                        onAddToCuration = { post.id.toIntOrNull()?.let { addToCurationSource = CurationSource.Artwork(it, post.imageUrl) } },
                        onArtistClick = { post.ownerId?.let(onOpenProfile) },
                    )
                }
            }
            return@LazyColumn
        }

        // ── For You tab: finite mixed feed (posts + sponsored banners) ─
        if (uiState.activeTab == HomeTab.FOR_YOU) {
            if (uiState.isLoading) {
                items(count = 3, key = { "foryou-shimmer-$it" }) { FeedShimmer() }
            } else if (uiState.forYouItems.isEmpty()) {
                item(key = "foryou-empty") {
                    EmptyView(
                        icon = Icons.Outlined.Palette,
                        title = "Nothing here yet",
                        subtitle = "Curated art is on its way.",
                    )
                }
            } else {
                items(
                    count = uiState.forYouItems.size,
                    key = { i ->
                        when (val it = uiState.forYouItems[i]) {
                            is ForYouItem.Post -> "foryou-post-${it.post.id}"
                            is ForYouItem.Sponsored -> "foryou-sponsor-${it.banner.id}"
                        }
                    },
                ) { i ->
                    when (val forYouItem = uiState.forYouItems[i]) {
                        is ForYouItem.Post -> DiscoverFeedItem(
                            post = forYouItem.post,
                            onLike = { onLike(forYouItem.post.id) },
                            onClick = { onNavigateToDetail(forYouItem.post.id) },
                            onAddToCuration = {
                                forYouItem.post.id.toIntOrNull()?.let {
                                    addToCurationSource = CurationSource.Artwork(it, forYouItem.post.imageUrl)
                                }
                            },
                            onArtistClick = { forYouItem.post.ownerId?.let(onOpenProfile) },
                        )
                        is ForYouItem.Sponsored -> Column(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Sponsored",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    horizontal = Spacing.md,
                                    vertical = Spacing.xs,
                                ),
                            )
                            FeaturedCarouselItem(
                                item = forYouItem.banner,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(d.bannerHeight),
                            )
                        }
                    }
                }
            }
            return@LazyColumn
        }

        // ── Discover tab ───────────────────────────────────────────────
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
                    else -> FeaturedCarousel(
                        items = uiState.bannerItems,
                    )
                }
            }

            // ── New Art For You ────────────────────────────────────────
            item(key = "new-art-header") {
                SectionHeader(title = "New Art For You")
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
                            ArtworkCard(item = item, onClick = { onNavigateToDetail(item.id) })
                        }
                    }
                }
            }

            // ── Popular Curations ──────────────────────────────────────
            item(key = "curations-header") {
                SectionHeader(title = "Popular Curations")
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
                            CollectionCard(
                                item = item,
                                onClick = { onNavigateToCurationDetail(item.id) },
                            )
                        }
                    }
                }
            }

            // ── Recently Viewed ────────────────────────────────────────
            item(key = "recent-header") {
                SectionHeader(title = "Recently Viewed")
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
                            RecentlyViewedCard(item = item, onClick = { onNavigateToDetail(item.id) })
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
                        onClick = { onNavigateToDetail(post.id) },
                        onAddToCuration = { post.id.toIntOrNull()?.let { addToCurationSource = CurationSource.Artwork(it, post.imageUrl) } },
                        onArtistClick = { post.ownerId?.let(onOpenProfile) },
                    )
                }
            }
        }
    }
    }

    addToCurationSource?.let { src ->
        AddToCurationSheet(
            source = src,
            onDismiss = { addToCurationSource = null },
            onCreateNew = {
                addToCurationSource = null
                onNavigateToNewCuration()
            },
        )
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
        )
    }
}
