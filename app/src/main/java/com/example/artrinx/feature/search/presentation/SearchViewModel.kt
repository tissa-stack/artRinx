package com.example.artrinx.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.feature.search.domain.model.MockSearchData
import com.example.artrinx.feature.search.domain.model.ResultTab
import com.example.artrinx.feature.search.domain.model.SearchFilter
import com.example.artrinx.feature.search.domain.model.SearchPhase
import com.example.artrinx.feature.search.domain.model.SortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onFocused() {
        if (_uiState.value.phase == SearchPhase.IDLE) {
            _uiState.update { it.copy(phase = SearchPhase.ACTIVE_EMPTY) }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update {
            it.copy(
                query = query,
                phase = if (query.isEmpty()) SearchPhase.ACTIVE_EMPTY else SearchPhase.RESULTS,
            )
        }
        searchJob?.cancel()
        if (query.isNotEmpty()) {
            searchJob = viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, results = emptyList()) }
                delay(400)
                _uiState.update { it.copy(isLoading = false, results = MockSearchData.artResults) }
            }
        }
    }

    fun onClearQuery() {
        searchJob?.cancel()
        _uiState.update { it.copy(query = "", phase = SearchPhase.ACTIVE_EMPTY, results = emptyList(), isLoading = false) }
    }

    fun onDismissSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(query = "", phase = SearchPhase.IDLE, results = emptyList(), isLoading = false) }
    }

    fun onTabSelected(tab: ResultTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onToggleSortMenu() {
        _uiState.update { it.copy(isSortMenuVisible = !it.isSortMenuVisible) }
    }

    fun onDismissSortMenu() {
        _uiState.update { it.copy(isSortMenuVisible = false) }
    }

    fun onSortSelected(option: SortOption) {
        _uiState.update { it.copy(sortOption = option, isSortMenuVisible = false) }
    }

    fun onShowFilter() {
        _uiState.update { it.copy(isFilterSheetVisible = true) }
    }

    fun onDismissFilter() {
        _uiState.update { it.copy(isFilterSheetVisible = false) }
    }

    fun onToggleStyleExpanded() {
        _uiState.update { it.copy(isStyleExpanded = !it.isStyleExpanded) }
    }

    fun onToggleCountry() {
        _uiState.update { it.copy(filter = it.filter.copy(country = !it.filter.country)) }
    }

    fun onToggleState() {
        _uiState.update { it.copy(filter = it.filter.copy(state = !it.filter.state)) }
    }

    fun onToggleCity() {
        _uiState.update { it.copy(filter = it.filter.copy(city = !it.filter.city)) }
    }

    fun onToggleShopArt() {
        _uiState.update { it.copy(filter = it.filter.copy(shopArtOnly = !it.filter.shopArtOnly)) }
    }

    fun onToggleStyleOption(style: String) {
        _uiState.update { state ->
            val updated = state.filter.styles.toMutableSet().apply {
                if (style in this) remove(style) else add(style)
            }
            state.copy(filter = state.filter.copy(styles = updated))
        }
    }

    fun onResetFilter() {
        _uiState.update { it.copy(filter = SearchFilter(), isStyleExpanded = false) }
    }
}
