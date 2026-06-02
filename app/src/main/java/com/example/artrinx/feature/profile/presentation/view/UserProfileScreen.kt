package com.example.artrinx.feature.profile.presentation.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
    onNavigateToSettings: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    UserProfileContent(
        uiState = uiState,
        onTabSelected = viewModel::onTabSelected,
        onBioExpandToggle = viewModel::onBioExpandToggle,
        onNavigateToHome = onNavigateToHome,
        onNavigateToSearch = onNavigateToSearch,
        onNavigateToCreate = onNavigateToCreate,
        onNavigateToSettings = onNavigateToSettings,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserProfileContent(
    uiState: UserProfileUiState,
    onTabSelected: (ProfileTab) -> Unit,
    onBioExpandToggle: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            BottomNavBar(
                activeRoute = NavRoutes.PROFILE,
                onNavigate = { route ->
                    when (route) {
                        NavRoutes.HOME   -> onNavigateToHome()
                        NavRoutes.SEARCH -> onNavigateToSearch()
                        NavRoutes.CREATE -> onNavigateToCreate()
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
                        ProfileTab.ART -> ProfileArtMasonryGrid(
                            items = uiState.artItems,
                            modifier = Modifier.padding(top = Spacing.md),
                            onItemClick = {},
                        )
                        ProfileTab.CURATIONS -> ProfileCurationsGrid(
                            items = uiState.curations,
                            modifier = Modifier.padding(top = Spacing.md),
                            onItemClick = {},
                        )
                        ProfileTab.LIKED -> ProfileArtMasonryGrid(
                            items = uiState.likedItems,
                            modifier = Modifier.padding(top = Spacing.md),
                            onItemClick = {},
                        )
                    }
                }
            }
        }
    }
}