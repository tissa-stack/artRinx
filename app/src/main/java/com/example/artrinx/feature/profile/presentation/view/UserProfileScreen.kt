package com.example.artrinx.feature.profile.presentation.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.artrinx.core.navigation.NavRoutes
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.home.presentation.components.BottomNavBar
import com.example.artrinx.feature.home.presentation.components.CurationProgressRow
import com.example.artrinx.feature.home.presentation.components.UploadProgressRow
import com.example.artrinx.feature.home.presentation.components.state.EmptyView
import com.example.artrinx.feature.profile.domain.model.ProfileTab
import com.example.artrinx.feature.profile.presentation.view.components.ProfileArtMasonryGrid
import com.example.artrinx.feature.profile.presentation.view.components.ProfileCurationsGrid
import com.example.artrinx.feature.profile.presentation.view.components.ProfileHeaderSection
import com.example.artrinx.feature.profile.presentation.view.components.ProfileTabBar
import com.example.artrinx.feature.profile.presentation.view.components.shimmer.ProfileShimmer

@Composable
fun UserProfileScreen(
    viewModel: UserProfileViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToCurationDetail: (String) -> Unit = {},
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
        onNavigateToDetail        = onNavigateToDetail,
        onNavigateToCurationDetail = onNavigateToCurationDetail,
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
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCurationDetail: (String) -> Unit,
) {
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .statusBarsPadding(),
                contentPadding = PaddingValues(bottom = Spacing.xxl),
            ) {
                item(key = "header") {
                    ProfileHeaderSection(
                        profile = uiState.profile,
                        isBioExpanded = uiState.isBioExpanded,
                        onExpandBio = onBioExpandToggle,
                        onSettingsClick = onNavigateToSettings,
                    )
                }

                stickyHeader(key = "tabs") {
                    ProfileTabBar(
                        activeTab = uiState.activeTab,
                        onTabSelected = onTabSelected,
                    )
                }

                item(key = "content_${uiState.activeTab.name}") {
                    when (uiState.activeTab) {
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