package com.rinx.artRINXapp.feature.search.presentation

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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.core.tour.TourTarget
import com.rinx.artRINXapp.core.tour.TourViewModel
import com.rinx.artRINXapp.feature.home.presentation.components.BottomNavBar
import com.rinx.artRINXapp.feature.search.presentation.components.CurationGridCard
import com.rinx.artRINXapp.feature.search.domain.model.ResultTab
import com.rinx.artRINXapp.feature.search.domain.model.SearchPhase
import com.rinx.artRINXapp.feature.search.domain.model.SearchResultItem
import com.rinx.artRINXapp.feature.search.domain.model.SortOption
import com.rinx.artRINXapp.feature.search.presentation.components.ArtMasonryResultsGrid
import com.rinx.artRINXapp.feature.search.presentation.components.FilterBottomSheet
import com.rinx.artRINXapp.feature.search.presentation.components.ManualMasonryGrid
import com.rinx.artRINXapp.feature.search.presentation.components.ResultTypeTabs
import com.rinx.artRINXapp.feature.search.presentation.components.SearchIdleShimmer
import com.rinx.artRINXapp.feature.search.presentation.components.SearchMessageView
import com.rinx.artRINXapp.feature.search.presentation.components.SearchResultsShimmer
import com.rinx.artRINXapp.feature.search.presentation.components.SearchTopBar
import com.rinx.artRINXapp.feature.search.presentation.components.TrendingTagsSection
import com.rinx.artRINXapp.feature.search.presentation.components.UserResultRow

@Composable
fun SearchScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToCurationDetail: (String) -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    // First-launch tour: report the search-bar bounds so the global overlay can spotlight it.
    val tour: TourViewModel = hiltViewModel()
    val tourState by tour.state.collectAsState()

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
                    modifier = Modifier
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                        .then(
                            if (tourState.active) {
                                Modifier.onGloballyPositioned {
                                    tour.report(TourTarget.SEARCH_BAR, it.boundsInWindow())
                                }
                            } else {
                                Modifier
                            },
                        ),
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
                            onOpenProfile = onOpenProfile,
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
                countryOptions = uiState.countryOptions,
                stateOptions = uiState.stateOptions,
                cityOptions = uiState.cityOptions,
                onCountrySelected = viewModel::onCountrySelected,
                onStateSelected = viewModel::onStateSelected,
                onCityChanged = viewModel::onCityChanged,
                onCitySelected = viewModel::onCitySelected,
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

        uiState.trendingTags.isEmpty() ->
            SearchMessageView(
                title = "Nothing to show yet",
                subtitle = "Search for art, artists, and curations.",
                modifier = Modifier.fillMaxSize(),
            )

        // Landing shows ONLY trending tags (no "Recommended For You" section per design).
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
        ) {
            item(key = "trending") {
                TrendingTagsSection(tags = uiState.trendingTags, onTagClick = onTagClick)
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
    onOpenProfile: (String) -> Unit,
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

            // Filter button — filled funnel tinted primary when any filter is active; outline otherwise.
            val filterActive = uiState.filter.hasAnySelection
            IconButton(onClick = onShowFilter) {
                Icon(
                    painter = painterResource(
                        if (filterActive) R.drawable.ic_filter_filled else R.drawable.ic_filter,
                    ),
                    contentDescription = "Filter",
                    tint = if (filterActive) BrandPrimary else MaterialTheme.colorScheme.onBackground,
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
                        UserResultRow(user = user, onClick = { onOpenProfile(user.id) })
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