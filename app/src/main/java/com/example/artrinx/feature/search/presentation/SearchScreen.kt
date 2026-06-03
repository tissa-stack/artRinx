package com.example.artrinx.feature.search.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.artrinx.R
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.home.presentation.components.BottomNavBar
import com.example.artrinx.feature.search.presentation.components.CurationGridCard
import com.example.artrinx.feature.search.domain.model.ResultTab
import com.example.artrinx.feature.search.domain.model.SearchPhase
import com.example.artrinx.feature.search.domain.model.SearchResultItem
import com.example.artrinx.feature.search.domain.model.SortOption
import com.example.artrinx.feature.search.presentation.components.ArtMasonryResultsGrid
import com.example.artrinx.feature.search.presentation.components.FilterBottomSheet
import com.example.artrinx.feature.search.presentation.components.ManualMasonryGrid
import com.example.artrinx.feature.search.presentation.components.ResultTypeTabs
import com.example.artrinx.feature.search.presentation.components.SearchIdleShimmer
import com.example.artrinx.feature.search.presentation.components.SearchMessageView
import com.example.artrinx.feature.search.presentation.components.SearchResultsShimmer
import com.example.artrinx.feature.search.presentation.components.SearchTopBar
import com.example.artrinx.feature.search.presentation.components.TrendingTagsSection
import com.example.artrinx.feature.search.presentation.components.UserResultRow

@Composable
fun SearchScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToCurationDetail: (String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Intercept back when results are visible (return to the idle content) or the filter is open.
    BackHandler(enabled = uiState.phase == SearchPhase.RESULTS || uiState.isFilterSheetVisible) {
        if (uiState.isFilterSheetVisible) viewModel.onDismissFilter() else viewModel.onClearQuery()
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                BottomNavBar(
                    activeRoute = "search",
                    onNavigate = { route ->
                        when (route) {
                            "home" -> { focusManager.clearFocus(); onNavigateToHome() }
                            "create" -> { focusManager.clearFocus(); onNavigateToCreate() }
                            "notifications" -> { focusManager.clearFocus(); onNavigateToNotifications() }
                            "profile" -> { focusManager.clearFocus(); onNavigateToProfile() }
                        }
                    },
                )
            },
            contentWindowInsets = WindowInsets(0),
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
            ) {
                SearchTopBar(
                    query = uiState.query,
                    onQueryChange = viewModel::onQueryChange,
                    onClear = viewModel::onClearQuery,
                    onFocused = viewModel::onFocused,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                )

                AnimatedContent(
                    targetState = uiState.phase,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    label = "search-phase",
                ) { phase ->
                    when (phase) {
                        SearchPhase.IDLE -> Box(Modifier.fillMaxSize())

                        SearchPhase.ACTIVE_EMPTY -> SearchIdleContent(
                            uiState = uiState,
                            onTagClick = viewModel::onQueryChange,
                            onNavigateToDetail = onNavigateToDetail,
                        )

                        SearchPhase.RESULTS -> SearchResultsContent(
                            uiState = uiState,
                            onTabSelected = viewModel::onTabSelected,
                            onToggleSortMenu = viewModel::onToggleSortMenu,
                            onDismissSortMenu = viewModel::onDismissSortMenu,
                            onSortSelected = viewModel::onSortSelected,
                            onShowFilter = viewModel::onShowFilter,
                            onRetry = viewModel::onRetry,
                            onNavigateToDetail = onNavigateToDetail,
                            onNavigateToCurationDetail = onNavigateToCurationDetail,
                        )
                    }
                }
            }
        }

        // ── Full-screen filter overlay ─────────────────────────────────────
        if (uiState.isFilterSheetVisible) {
            FilterBottomSheet(
                filter = uiState.filter,
                mediums = uiState.mediums,
                isStyleExpanded = uiState.isStyleExpanded,
                onToggleStyle = viewModel::onToggleStyleExpanded,
                onToggleShopArt = viewModel::onToggleShopArt,
                onToggleMedium = viewModel::onToggleMedium,
                onReset = viewModel::onResetFilter,
                onViewResults = viewModel::onApplyFilter,
                onDismiss = viewModel::onDismissFilter,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// ── Idle / empty search content ───────────────────────────────────────────────

@Composable
private fun SearchIdleContent(
    uiState: SearchUiState,
    onTagClick: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
) {
    when {
        uiState.isIdleLoading -> SearchIdleShimmer(modifier = Modifier.fillMaxSize())

        uiState.trendingTags.isEmpty() && uiState.recommended.isEmpty() ->
            SearchMessageView(
                title = "Nothing to show yet",
                subtitle = "Search for art, artists, and curations.",
                modifier = Modifier.fillMaxSize(),
            )

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
        ) {
            if (uiState.trendingTags.isNotEmpty()) {
                item(key = "trending") {
                    TrendingTagsSection(tags = uiState.trendingTags, onTagClick = onTagClick)
                }
            }
            if (uiState.recommended.isNotEmpty()) {
                item(key = "rec-header") {
                    Spacer(Modifier.height(Spacing.xl))
                    Text(
                        text = "Recommended For You",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(Spacing.md))
                }
                item(key = "rec-grid") {
                    ManualMasonryGrid(
                        items = uiState.recommended,
                        onItemClick = { item -> if (item.artId.isNotEmpty()) onNavigateToDetail(item.artId) },
                    )
                    Spacer(Modifier.height(Spacing.xxl))
                }
            }
        }
    }
}

// ── Search results content ────────────────────────────────────────────────────

@Composable
private fun SearchResultsContent(
    uiState: SearchUiState,
    onTabSelected: (ResultTab) -> Unit,
    onToggleSortMenu: () -> Unit,
    onDismissSortMenu: () -> Unit,
    onSortSelected: (SortOption) -> Unit,
    onShowFilter: () -> Unit,
    onRetry: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCurationDetail: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // ── Tabs + Sort + Filter row ──────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ResultTypeTabs(
                selectedTab = uiState.selectedTab,
                onTabSelected = onTabSelected,
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(Spacing.xs))

            // Sort button (artworks/curations only — users have no sort)
            Box {
                IconButton(onClick = onToggleSortMenu) {
                    Icon(
                        painter = painterResource(R.drawable.ic_sort),
                        contentDescription = "Sort",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(Spacing.xl),
                    )
                }
                DropdownMenu(
                    expanded = uiState.isSortMenuVisible,
                    onDismissRequest = onDismissSortMenu,
                ) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (option == uiState.sortOption) BrandPrimary
                                    else MaterialTheme.colorScheme.onBackground,
                                    fontWeight = if (option == uiState.sortOption) FontWeight.SemiBold
                                    else FontWeight.Normal,
                                )
                            },
                            onClick = { onSortSelected(option) },
                        )
                    }
                }
            }

            // Filter button
            IconButton(onClick = onShowFilter) {
                Icon(
                    painter = painterResource(R.drawable.ic_filter),
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(Spacing.xl),
                )
            }
        }

        // ── Results body ──────────────────────────────────────────────
        when {
            uiState.isLoading -> SearchResultsShimmer(modifier = Modifier.weight(1f))

            uiState.isError -> SearchMessageView(
                title = "Couldn't load results",
                subtitle = "Check your connection and try again.",
                modifier = Modifier.weight(1f),
                actionLabel = "Retry",
                onAction = onRetry,
            )

            uiState.activeTabIsEmpty -> SearchMessageView(
                title = "No results found",
                subtitle = "Try a different search or adjust your filters.",
                modifier = Modifier.weight(1f),
            )

            else -> when (uiState.selectedTab) {
                ResultTab.ART -> ArtMasonryResultsGrid(
                    items = uiState.artResults,
                    modifier = Modifier.weight(1f),
                    onItemClick = { item -> if (item.artId.isNotEmpty()) onNavigateToDetail(item.artId) },
                )

                ResultTab.USERS -> LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                ) {
                    items(items = uiState.userResults, key = { it.id }) { user ->
                        UserResultRow(user = user)
                    }
                }

                ResultTab.CURATIONS -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    gridItems(items = uiState.curationResults, key = { it.id }) { curation ->
                        CurationGridCard(
                            item = curation,
                            onClick = { if (curation.id.isNotEmpty()) onNavigateToCurationDetail(curation.id) },
                        )
                    }
                }
            }
        }
    }
}

private val SortOption.label: String
    get() = when (this) {
        SortOption.NEWEST -> "Newest"
        SortOption.OLDEST -> "Oldest"
        SortOption.MOST_POPULAR -> "Most Popular"
    }