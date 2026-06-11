package com.rinx.artRINXapp.feature.profile.presentation.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import com.rinx.artRINXapp.core.ui.CollapsingHeaderTabsPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.navigation.NavRoutes
import com.rinx.artRINXapp.core.theme.BrandPrimary
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
    onNavigateToSettings: () -> Unit = {},
    onNavigateToInviteFriends: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToCurationDetail: (String) -> Unit = {},
    onOpenFollowers: () -> Unit = {},
    onOpenFollowing: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    UserProfileContent(
        uiState = uiState,
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
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserProfileContent(
    uiState: UserProfileUiState,
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
    onNavigateToSettings: () -> Unit,
    onNavigateToInviteFriends: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCurationDetail: (String) -> Unit,
    onOpenFollowers: () -> Unit = {},
    onOpenFollowing: () -> Unit = {},
    onLoadMore: () -> Unit = {},
) {
    var showFeedback by remember { mutableStateOf(false) }
    if (showFeedback) {
        FeedbackDialog(onDismiss = { showFeedback = false })
    }
    Scaffold(
        bottomBar = {
            BottomNavBar(
                activeRoute = NavRoutes.PROFILE,
                onNavigate = { route ->
                    when (route) {
                        NavRoutes.HOME          -> onNavigateToHome()
                        NavRoutes.SEARCH        -> onNavigateToSearch()
                        NavRoutes.CREATE        -> onNavigateToCreate()
                        NavRoutes.NOTIFICATIONS -> onNavigateToNotifications()
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        if (uiState.isLoading) {
            ProfileShimmer(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .statusBarsPadding(),
            )
        } else if (uiState.profile != null) {
            val tabs = ProfileTab.entries
            val pagerState = rememberPagerState(
                initialPage = tabs.indexOf(uiState.activeTab).coerceAtLeast(0),
            ) { tabs.size }
            // Swipe ↔ tab two-way sync (mirrors Home tabs).
            LaunchedEffect(pagerState.currentPage) {
                val swiped = tabs[pagerState.currentPage]
                if (swiped != uiState.activeTab) onTabSelected(swiped)
            }
            LaunchedEffect(uiState.activeTab) {
                val idx = tabs.indexOf(uiState.activeTab).coerceAtLeast(0)
                if (pagerState.currentPage != idx) pagerState.animateScrollToPage(idx)
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
            CollapsingHeaderTabsPager(
                pagerState = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .statusBarsPadding(),
                header = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ProfileHeaderSection(
                            profile = uiState.profile,
                            isBioExpanded = uiState.isBioExpanded,
                            onExpandBio = onBioExpandToggle,
                            onSettingsClick = onNavigateToSettings,
                            onInviteFriendsClick = onNavigateToInviteFriends,
                            onFollowersClick = onOpenFollowers,
                            onFollowingClick = onOpenFollowing,
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
                        onTabSelected = onTabSelected,
                        pagerState = pagerState,
                        tabs = tabs,
                    )
                },
            ) { page ->
                  val tab = tabs[page]
                  LazyColumn(
                    state = listStateFor(tab),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Spacing.xxl),
                  ) {
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
                                    title = "No curations yet",
                                    subtitle = "Curations you create will appear here.",
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