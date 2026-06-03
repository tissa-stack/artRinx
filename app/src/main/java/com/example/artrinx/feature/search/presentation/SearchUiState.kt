package com.example.artrinx.feature.search.presentation

import com.example.artrinx.feature.home.domain.model.CurationItem
import com.example.artrinx.feature.profile.domain.model.Medium
import com.example.artrinx.feature.search.domain.model.ResultTab
import com.example.artrinx.feature.search.domain.model.SearchFilter
import com.example.artrinx.feature.search.domain.model.SearchPhase
import com.example.artrinx.feature.search.domain.model.SearchResultItem
import com.example.artrinx.feature.search.domain.model.SortOption
import com.example.artrinx.feature.search.domain.model.UserSearchItem

data class SearchUiState(
    val query: String = "",
    val phase: SearchPhase = SearchPhase.ACTIVE_EMPTY,
    val selectedTab: ResultTab = ResultTab.ART,
    val sortOption: SortOption = SortOption.NEWEST,
    val filter: SearchFilter = SearchFilter(),

    // Results (per tab)
    val artResults: List<SearchResultItem> = emptyList(),
    val curationResults: List<CurationItem> = emptyList(),
    val userResults: List<UserSearchItem> = emptyList(),

    // Idle / empty screen content
    val recommended: List<SearchResultItem> = emptyList(),
    val trendingTags: List<String> = emptyList(),
    val isIdleLoading: Boolean = false,

    // Filter data
    val mediums: List<Medium> = emptyList(),

    // Active-tab search status
    val isLoading: Boolean = false,
    val isError: Boolean = false,

    // Sheet / menu visibility
    val isSortMenuVisible: Boolean = false,
    val isFilterSheetVisible: Boolean = false,
    val isStyleExpanded: Boolean = false,
) {
    /** Whether the currently selected tab has zero results (used for the "No results" state). */
    val activeTabIsEmpty: Boolean
        get() = when (selectedTab) {
            ResultTab.ART -> artResults.isEmpty()
            ResultTab.USERS -> userResults.isEmpty()
            ResultTab.CURATIONS -> curationResults.isEmpty()
        }
}