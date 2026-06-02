package com.example.artrinx.feature.search.presentation

import com.example.artrinx.feature.search.domain.model.MockSearchData
import com.example.artrinx.feature.search.domain.model.ResultTab
import com.example.artrinx.feature.search.domain.model.SearchFilter
import com.example.artrinx.feature.search.domain.model.SearchPhase
import com.example.artrinx.feature.search.domain.model.SearchResultItem
import com.example.artrinx.feature.search.domain.model.SortOption

data class SearchUiState(
    val query: String = "",
    val phase: SearchPhase = SearchPhase.ACTIVE_EMPTY,
    val selectedTab: ResultTab = ResultTab.ART,
    val sortOption: SortOption = SortOption.NEWEST,
    val filter: SearchFilter = SearchFilter(),
    val results: List<SearchResultItem> = emptyList(),
    val recommended: List<SearchResultItem> = MockSearchData.recommended,
    val isLoading: Boolean = false,
    val isSortMenuVisible: Boolean = false,
    val isFilterSheetVisible: Boolean = false,
    val isStyleExpanded: Boolean = false,
)
