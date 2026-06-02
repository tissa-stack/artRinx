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
import androidx.compose.material.icons.Icons
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.home.presentation.components.BottomNavBar
import com.example.artrinx.feature.search.domain.model.ResultTab
import com.example.artrinx.feature.search.domain.model.SearchPhase
import com.example.artrinx.feature.search.domain.model.SearchResultItem
import com.example.artrinx.feature.search.domain.model.SortOption
import com.example.artrinx.feature.search.domain.model.TRENDING_TAGS
import com.example.artrinx.feature.search.presentation.components.ArtMasonryResultsGrid
import com.example.artrinx.feature.search.presentation.components.FilterBottomSheet
import com.example.artrinx.feature.search.presentation.components.ManualMasonryGrid
import com.example.artrinx.feature.search.presentation.components.ResultTypeTabs
import com.example.artrinx.feature.search.presentation.components.SearchIdleShimmer
import com.example.artrinx.feature.search.presentation.components.SearchResultsShimmer
import com.example.artrinx.feature.search.presentation.components.SearchTopBar
import com.example.artrinx.feature.search.presentation.components.TrendingTagsSection
@Composable
fun SearchScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Only intercept back when results are showing — return to the content view.
    // ACTIVE_EMPTY falls through to the system so pressing back pops to HomeScreen.
    // Intercept back when results are visible. Also intercept when filter screen is open.
    BackHandler(enabled = uiState.phase == SearchPhase.RESULTS || uiState.isFilterSheetVisible) {
        if (uiState.isFilterSheetVisible) {
            viewModel.onDismissFilter()
        } else {
            viewModel.onClearQuery()
        }
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        bottomBar = {
            BottomNavBar(
                activeRoute = "search",
                onNavigate  = { route ->
                    when (route) {
                        "home"    -> { focusManager.clearFocus(); onNavigateToHome() }
                        "create"  -> { focusManager.clearFocus(); onNavigateToCreate() }
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
                query         = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                onClear       = viewModel::onClearQuery,
                onFocused     = viewModel::onFocused,
                modifier      = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )

            AnimatedContent(
                targetState  = uiState.phase,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                modifier     = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label        = "search-phase",
            ) { phase ->
                when (phase) {
                    SearchPhase.IDLE -> Box(Modifier.fillMaxSize())

                    SearchPhase.ACTIVE_EMPTY -> SearchIdleContent(
                        recommended      = uiState.recommended,
                        onTagClick       = viewModel::onQueryChange,
                        onNavigateToDetail = onNavigateToDetail,
                    )

                    SearchPhase.RESULTS -> SearchResultsContent(
                        uiState            = uiState,
                        onTabSelected      = viewModel::onTabSelected,
                        onToggleSortMenu   = viewModel::onToggleSortMenu,
                        onDismissSortMenu  = viewModel::onDismissSortMenu,
                        onSortSelected     = viewModel::onSortSelected,
                        onShowFilter       = viewModel::onShowFilter,
                        onNavigateToDetail = onNavigateToDetail,
                    )
                }
            }
        }
    }

    // ── Full-screen filter overlay — sits on top of Scaffold + BottomNavBar ──
    if (uiState.isFilterSheetVisible) {
        FilterBottomSheet(
            filter              = uiState.filter,
            isStyleExpanded     = uiState.isStyleExpanded,
            onToggleStyle       = viewModel::onToggleStyleExpanded,
            onToggleCountry     = viewModel::onToggleCountry,
            onToggleState       = viewModel::onToggleState,
            onToggleCity        = viewModel::onToggleCity,
            onToggleShopArt     = viewModel::onToggleShopArt,
            onToggleStyleOption = viewModel::onToggleStyleOption,
            onReset             = viewModel::onResetFilter,
            onViewResults       = viewModel::onDismissFilter,
            onDismiss           = viewModel::onDismissFilter,
            modifier            = Modifier.fillMaxSize(),
        )
    }
    } // end Box
}

// ── Idle / empty search content ───────────────────────────────────────────────

@Composable
private fun SearchIdleContent(
    recommended: List<SearchResultItem>,
    onTagClick: (String) -> Unit,
    onNavigateToDetail: (String) -> Unit,
) {
    LazyColumn(
        modifier        = Modifier.fillMaxSize(),
        contentPadding  = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        item(key = "trending") {
            TrendingTagsSection(
                tags       = TRENDING_TAGS,
                onTagClick = onTagClick,
            )
        }
        item(key = "rec-header") {
            Spacer(Modifier.height(Spacing.xl))
            Text(
                text       = "Recommended For You",
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.md))
        }
        item(key = "rec-grid") {
            ManualMasonryGrid(
                items       = recommended,
                onItemClick = { item -> if (item.artId.isNotEmpty()) onNavigateToDetail(item.artId) },
            )
            Spacer(Modifier.height(Spacing.xxl))
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
    onNavigateToDetail: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // ── Tabs + Sort + Filter row ──────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ResultTypeTabs(
                selectedTab   = uiState.selectedTab,
                onTabSelected = onTabSelected,
                modifier      = Modifier.weight(1f),
            )

            Spacer(Modifier.width(Spacing.xs))

            // Sort button + dropdown anchor
            Box {
                androidx.compose.material3.IconButton(onClick = onToggleSortMenu) {
                    Icon(
                        painter            = androidx.compose.ui.res.painterResource(com.example.artrinx.R.drawable.ic_sort),
                        contentDescription = "Sort",
                        tint               = MaterialTheme.colorScheme.onBackground,
                        modifier           = Modifier.size(Spacing.xl),
                    )
                }
                DropdownMenu(
                    expanded         = uiState.isSortMenuVisible,
                    onDismissRequest = onDismissSortMenu,
                ) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text    = {
                                Text(
                                    text       = option.label,
                                    style      = MaterialTheme.typography.bodyMedium,
                                    color      = if (option == uiState.sortOption) BrandPrimary
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
            androidx.compose.material3.IconButton(onClick = onShowFilter) {
                Icon(
                    painter            = androidx.compose.ui.res.painterResource(com.example.artrinx.R.drawable.ic_filter),
                    contentDescription = "Filter",
                    tint               = MaterialTheme.colorScheme.onBackground,
                    modifier           = Modifier.size(Spacing.xl),
                )
            }
        }

        // ── Results grid ──────────────────────────────────────────────
        if (uiState.isLoading) {
            SearchResultsShimmer(modifier = Modifier.weight(1f))
        } else {
            ArtMasonryResultsGrid(
                items       = uiState.results,
                modifier    = Modifier.weight(1f),
                onItemClick = { item -> if (item.artId.isNotEmpty()) onNavigateToDetail(item.artId) },
            )
        }
    }
}

private val SortOption.label: String
    get() = when (this) {
        SortOption.NEWEST      -> "Newest"
        SortOption.OLDEST      -> "Oldest"
        SortOption.MOST_POPULAR -> "Most Popular"
    }
