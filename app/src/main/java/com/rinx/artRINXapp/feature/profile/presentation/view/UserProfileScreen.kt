package com.rinx.artRINXapp.feature.profile.presentation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import com.rinx.artRINXapp.core.ui.ProfileHeaderTabsPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.navigation.NavRoutes
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.tour.TourTarget
import com.rinx.artRINXapp.core.tour.TourViewModel
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.profile.presentation.feedback.FeedbackDialog
import com.rinx.artRINXapp.feature.home.presentation.components.BottomNavBar
import com.rinx.artRINXapp.feature.home.presentation.components.CurationProgressRow
import com.rinx.artRINXapp.feature.home.presentation.components.UploadProgressRow
import com.rinx.artRINXapp.feature.home.presentation.components.state.EmptyView
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileTab
import com.rinx.artRINXapp.feature.profile.presentation.view.components.ProfileArtMasonryGrid
import com.rinx.artRINXapp.feature.profile.presentation.view.components.ProfileCurationsGrid
import com.rinx.artRINXapp.feature.profile.presentation.view.components.ProfileHeaderSection
import com.rinx.artRINXapp.feature.profile.presentation.view.components.ProfileTabBar
import com.rinx.artRINXapp.feature.profile.presentation.view.components.shimmer.ProfileShimmer

@Composable
fun UserProfileScreen(
    viewModel: UserProfileViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToInviteFriends: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToCurationDetail: (String) -> Unit = {},
    onOpenFollowers: () -> Unit = {},
    onOpenFollowing: () -> Unit = {},
    /** Non-null when shown as a pushed screen (e.g. tapping your own avatar elsewhere) — renders a
     *  Back affordance and keeps [activeRoute] highlighted in the bottom nav. Null = the Profile tab. */
    onBack: (() -> Unit)? = null,
    activeRoute: String = NavRoutes.PROFILE,
) {
    val uiState by viewModel.uiState.collectAsState()

    // First-launch tour: report the invite button's bounds so the global overlay can spotlight it
    // (the final tour step navigates to the Profile tab and highlights "Invite friends").
    val tour: TourViewModel = hiltViewModel()
    val tourState by tour.state.collectAsState()

    UserProfileContent(
        uiState = uiState,
        onBack = onBack,
        activeRoute = activeRoute,
        onNavigateToProfile = onNavigateToProfile,
        onInviteBounds = if (tourState.active) {
            { rect -> tour.report(TourTarget.PROFILE_INVITE, rect) }
        } else {
            null
        },
        onTabSelected = viewModel::onTabSelected,
        onBioExpandToggle = viewModel::onBioExpandToggle,
        onRetryUpload = viewModel::onRetryUpload,
        onDismissUpload = viewModel::onDismissUpload,
        onRetryCuration = viewModel::onRetryCuration,
        onDismissCuration = viewModel::onDismissCuration,
        onNavigateToHome          = onNavigateToHome,
        onNavigateToSearch        = onNavigateToSearch,
        onNavigateToCreate        = onNavigateToCreate,
        onNavigateToNotifications = onNavigateToNotifications,
        onNavigateToSettings      = onNavigateToSettings,
        onNavigateToInviteFriends = onNavigateToInviteFriends,
        onNavigateToDetail        = onNavigateToDetail,
        onNavigateToCurationDetail = onNavigateToCurationDetail,
        onOpenFollowers = onOpenFollowers,
        onOpenFollowing = onOpenFollowing,
        onLoadMore = viewModel::loadMore,
        onRefresh = viewModel::refresh,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileContent(
    uiState: UserProfileUiState,
    onBack: (() -> Unit)? = null,
    activeRoute: String = NavRoutes.PROFILE,
    onTabSelected: (ProfileTab) -> Unit,
    onBioExpandToggle: () -> Unit,
    onRetryUpload: () -> Unit,
    onDismissUpload: () -> Unit,
    onRetryCuration: () -> Unit,
    onDismissCuration: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onNavigateToInviteFriends: () -> Unit = {},
    onInviteBounds: ((Rect) -> Unit)? = null,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCurationDetail: (String) -> Unit,
    onOpenFollowers: () -> Unit = {},
    onOpenFollowing: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    var showFeedback by remember { mutableStateOf(false) }
    if (showFeedback) {
        FeedbackDialog(onDismiss = { showFeedback = false })
    }
    Scaffold(
        bottomBar = {
            BottomNavBar(
                activeRoute = activeRoute,
                onNavigate = { route ->
                    when (route) {
                        NavRoutes.HOME          -> onNavigateToHome()
                        NavRoutes.SEARCH        -> onNavigateToSearch()
                        NavRoutes.CREATE        -> onNavigateToCreate()
                        NavRoutes.NOTIFICATIONS -> onNavigateToNotifications()
                        NavRoutes.PROFILE       -> onNavigateToProfile()
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
      Column(
          modifier = Modifier
              .fillMaxSize()
              .padding(bottom = innerPadding.calculateBottomPadding())
              .statusBarsPadding(),
      ) {
        if (uiState.isLoading) {
            ProfileShimmer(
                modifier = Modifier.fillMaxSize(),
            )
        } else if (uiState.profile != null) {
            val tabs = ProfileTab.entries
            // Fixed header + tab bar above a swipeable pager (mirrors Home): swipe or drag the bar
            // to change tabs; the header/tabs stay put while pages slide.
            val pagerState = rememberPagerState(
                initialPage = tabs.indexOf(uiState.activeTab).coerceAtLeast(0),
            ) { tabs.size }
            // Pager is the single source of truth: swipe + tab tap drive it; the settled page mirrors
            // into activeTab. No activeTab→pager binding (that two-way loop, plus the old tab-bar drag,
            // is what left the header stuck between tabs).
            LaunchedEffect(pagerState.settledPage) {
                val swiped = tabs[pagerState.settledPage]
                if (swiped != uiState.activeTab) onTabSelected(swiped)
            }
            // Each tab keeps its own scroll position; infinite-scroll pages the active tab.
            val artListState = rememberLazyListState()
            val curationListState = rememberLazyListState()
            val likedListState = rememberLazyListState()
            fun listStateFor(tab: ProfileTab) = when (tab) {
                ProfileTab.ART -> artListState
                ProfileTab.CURATIONS -> curationListState
                ProfileTab.LIKED -> likedListState
            }
            val activeListState = listStateFor(uiState.activeTab)
            LaunchedEffect(activeListState, uiState.activeTab) {
                snapshotFlow { activeListState.canScrollForward }
                    .collect { canScroll -> if (!canScroll) onLoadMore() }
            }
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
            ProfileHeaderTabsPager(
                pagerState = pagerState,
                listStateFor = { listStateFor(tabs[it]) },
                modifier = Modifier.fillMaxSize(),
                header = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ProfileHeaderSection(
                            profile = uiState.profile,
                            isBioExpanded = uiState.isBioExpanded,
                            onExpandBio = onBioExpandToggle,
                            onSettingsClick = onNavigateToSettings,
                            onInviteFriendsClick = onNavigateToInviteFriends,
                            onInviteBounds = onInviteBounds,
                            onFollowersClick = onOpenFollowers,
                            onFollowingClick = onOpenFollowing,
                            onBack = onBack,
                        )
                        // Feedback button — mid/bottom-right of the header, just above the tabs.
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = Spacing.lg, bottom = Spacing.md)
                                .size(Spacing.giant)
                                .clip(CircleShape)
                                .background(BrandPrimary)
                                .clickable { showFeedback = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_feedback),
                                contentDescription = "Leave feedback",
                                tint = Color.White,
                                modifier = Modifier.size(Spacing.xl),
                            )
                        }
                    }
                },
                tabBar = {
                    ProfileTabBar(
                        activeTab = uiState.activeTab,
                        pagerState = pagerState,
                        tabs = tabs,
                    )
                },
            ) { page ->
                val tab = tabs[page]
                item(key = "content_${tab.name}") {
                    when (tab) {
                        ProfileTab.ART -> Column {
                            uiState.uploadProgress?.let { progress ->
                                UploadProgressRow(
                                    progress = progress,
                                    onRetry = onRetryUpload,
                                    onDismiss = onDismissUpload,
                                    modifier = Modifier.padding(top = Spacing.sm),
                                )
                            }
                            if (uiState.artItems.isEmpty() && uiState.uploadProgress == null) {
                                EmptyView(
                                    icon = Icons.Outlined.Image,
                                    title = "No art yet",
                                    subtitle = "Artworks you upload will appear here.",
                                    modifier = Modifier.padding(top = Spacing.md),
                                )
                            } else {
                                ProfileArtMasonryGrid(
                                    items = uiState.artItems,
                                    modifier = Modifier.padding(top = Spacing.md),
                                    onItemClick = { onNavigateToDetail(it.id) },
                                )
                            }
                        }
                        ProfileTab.CURATIONS -> Column {
                            uiState.curationProgress?.let { progress ->
                                CurationProgressRow(
                                    progress = progress,
                                    onRetry = onRetryCuration,
                                    onDismiss = onDismissCuration,
                                    modifier = Modifier.padding(top = Spacing.sm),
                                )
                            }
                            if (uiState.curations.isEmpty() && uiState.curationProgress == null) {
                                EmptyView(
                                    icon = Icons.Outlined.Collections,
                                    title = "No collections yet",
                                    subtitle = "Collections you create will appear here.",
                                    modifier = Modifier.padding(top = Spacing.md),
                                )
                            } else {
                                ProfileCurationsGrid(
                                    items = uiState.curations,
                                    modifier = Modifier.padding(top = Spacing.md),
                                    onItemClick = { onNavigateToCurationDetail(it.id) },
                                )
                            }
                        }
                        ProfileTab.LIKED -> {
                            if (uiState.likedItems.isEmpty()) {
                                EmptyView(
                                    icon = Icons.Outlined.FavoriteBorder,
                                    title = "No liked art yet",
                                    subtitle = "Art you like will appear here.",
                                    modifier = Modifier.padding(top = Spacing.md),
                                )
                            } else {
                                ProfileArtMasonryGrid(
                                    items = uiState.likedItems,
                                    modifier = Modifier.padding(top = Spacing.md),
                                    onItemClick = { onNavigateToDetail(it.id) },
                                )
                            }
                        }
                    }
                }
            }
            }
        }
      }
    }
}