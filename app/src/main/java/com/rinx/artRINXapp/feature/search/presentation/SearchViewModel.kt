package com.rinx.artRINXapp.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import com.rinx.artRINXapp.feature.search.domain.model.ResultTab
import com.rinx.artRINXapp.feature.search.domain.model.SearchFilter
import com.rinx.artRINXapp.feature.search.domain.model.SearchPhase
import com.rinx.artRINXapp.feature.search.domain.model.SortOption
import com.rinx.artRINXapp.feature.search.domain.repository.SearchRepository
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
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    // Seed idle content synchronously from cache so re-entering the tab shows it instantly (SWR).
    private val _uiState = MutableStateFlow(seedFromCache())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    // Separate job for cascading location lookups so a new country/state/city query cancels the prior one.
    private var locationJob: Job? = null

    private fun seedFromCache(): SearchUiState = SearchUiState(
        trendingTags = searchRepository.cachedTrendingTags().orEmpty(),
        recommended = searchRepository.cachedRecommended().orEmpty(),
    )

    init {
        loadIdleContent()
        loadMediums()
        loadCountries()
    }

    /** Load the country filter options (places with ≥1 searchable user, ranked by count). */
    private fun loadCountries() {
        viewModelScope.launch {
            val result = searchRepository.getCountries()
            if (result is ApiResult.Success) {
                _uiState.update { it.copy(countryOptions = result.data) }
            }
        }
    }

    // ── Idle / empty screen ────────────────────────────────────────────────

    private fun loadIdleContent() {
        // Cached idle content already shown → revalidate silently (no shimmer).
        val hasCache = searchRepository.cachedTrendingTags() != null || searchRepository.cachedRecommended() != null
        viewModelScope.launch {
            if (!hasCache) _uiState.update { it.copy(isIdleLoading = true) }
            val tags = searchRepository.getTrendingTags()
            val recommended = searchRepository.getRecommended()
            _uiState.update {
                it.copy(
                    isIdleLoading = false,
                    // Keep prior values if a call failed, so a flaky refresh never blanks the screen.
                    trendingTags = (tags as? ApiResult.Success)?.data ?: it.trendingTags,
                    recommended = (recommended as? ApiResult.Success)?.data ?: it.recommended,
                )
            }
        }
    }

    private fun loadMediums() {
        viewModelScope.launch {
            val result = profileRepository.getMediums()
            if (result is ApiResult.Success) {
                _uiState.update { it.copy(mediums = result.data) }
            }
        }
    }

    // ── Query / search ──────────────────────────────────────────────────────

    fun onFocused() {
        if (_uiState.value.phase == SearchPhase.IDLE) {
            _uiState.update { it.copy(phase = SearchPhase.ACTIVE_EMPTY) }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update {
            it.copy(
                query = query,
                phase = if (query.isBlank()) SearchPhase.ACTIVE_EMPTY else SearchPhase.RESULTS,
            )
        }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update {
                it.copy(artResults = emptyList(), curationResults = emptyList(), userResults = emptyList(), isLoading = false, isError = false)
            }
            return
        }
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            runSearch()
        }
    }

    private fun runSearch() {
        val state = _uiState.value
        val query = state.query.trim()
        if (query.isBlank()) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isError = false) }
            val mediumIds = state.filter.mediumIds.toList()
            val f = state.filter
            when (state.selectedTab) {
                ResultTab.ART -> {
                    when (val res = searchRepository.searchArtworks(query, mediumIds, f.shopArtOnly, state.sortOption, f.country, f.state, f.city)) {
                        is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, isError = false, artResults = res.data) }
                        is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, isError = true) }
                    }
                }
                ResultTab.CURATIONS -> {
                    when (val res = searchRepository.searchCurations(query, mediumIds, state.sortOption, f.country, f.state, f.city)) {
                        is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, isError = false, curationResults = res.data) }
                        is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, isError = true) }
                    }
                }
                ResultTab.USERS -> {
                    when (val res = searchRepository.searchUsers(query, f.country, f.state, f.city)) {
                        is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, isError = false, userResults = res.data) }
                        is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, isError = true) }
                    }
                }
            }
        }
    }

    fun onRetry() = runSearch()

    fun onClearQuery() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                query = "",
                phase = SearchPhase.ACTIVE_EMPTY,
                artResults = emptyList(),
                curationResults = emptyList(),
                userResults = emptyList(),
                isLoading = false,
                isError = false,
            )
        }
    }

    fun onDismissSearch() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                query = "",
                phase = SearchPhase.IDLE,
                artResults = emptyList(),
                curationResults = emptyList(),
                userResults = emptyList(),
                isLoading = false,
                isError = false,
            )
        }
    }

    fun onTabSelected(tab: ResultTab) {
        if (_uiState.value.selectedTab == tab) return
        _uiState.update { it.copy(selectedTab = tab) }
        if (_uiState.value.query.isNotBlank()) runSearch()
    }

    // ── Sort ──────────────────────────────────────────────────────────────

    fun onToggleSortMenu() = _uiState.update { it.copy(isSortMenuVisible = !it.isSortMenuVisible) }

    fun onDismissSortMenu() = _uiState.update { it.copy(isSortMenuVisible = false) }

    fun onSortSelected(option: SortOption) {
        _uiState.update { it.copy(sortOption = option, isSortMenuVisible = false) }
        if (_uiState.value.query.isNotBlank()) runSearch()
    }

    // ── Filter ──────────────────────────────────────────────────────────────

    fun onShowFilter() = _uiState.update { it.copy(isFilterSheetVisible = true) }

    fun onDismissFilter() = _uiState.update { it.copy(isFilterSheetVisible = false) }

    /** "View results": close the sheet and re-run the search with the chosen filters. */
    fun onApplyFilter() {
        _uiState.update { it.copy(isFilterSheetVisible = false) }
        if (_uiState.value.query.isNotBlank()) runSearch()
    }

    fun onToggleStyleExpanded() = _uiState.update { it.copy(isStyleExpanded = !it.isStyleExpanded) }

    fun onToggleShopArt() {
        _uiState.update { it.copy(filter = it.filter.copy(shopArtOnly = !it.filter.shopArtOnly)) }
    }

    fun onToggleMedium(id: Int) {
        _uiState.update { state ->
            val updated = state.filter.mediumIds.toMutableSet().apply {
                if (id in this) remove(id) else add(id)
            }
            state.copy(filter = state.filter.copy(mediumIds = updated))
        }
    }

    /** Country picked — clears state/city (they depend on the country) and loads its state options. */
    fun onCountrySelected(country: String?) {
        val c = country?.ifBlank { null }
        locationJob?.cancel()
        _uiState.update {
            it.copy(
                filter = it.filter.copy(country = c, state = null, city = null),
                stateOptions = emptyList(),
                cityOptions = emptyList(),
            )
        }
        if (c == null) return
        locationJob = viewModelScope.launch {
            val result = searchRepository.getStates(c)
            if (result is ApiResult.Success) {
                _uiState.update { it.copy(stateOptions = result.data) }
            }
        }
    }

    /** State picked — clears city and pre-loads the first page of cities for the country+state. */
    fun onStateSelected(state: String?) {
        val s = state?.ifBlank { null }
        val country = _uiState.value.filter.country
        locationJob?.cancel()
        _uiState.update {
            it.copy(filter = it.filter.copy(state = s, city = null), cityOptions = emptyList())
        }
        if (s == null || country.isNullOrBlank()) return
        locationJob = viewModelScope.launch {
            val result = searchRepository.getCities(country, s, query = null)
            if (result is ApiResult.Success) {
                _uiState.update { it.copy(cityOptions = result.data) }
            }
        }
    }

    /** City text changed — updates the committed value and debounced-queries matching cities (`q`). */
    fun onCityChanged(city: String?) {
        val text = city?.ifBlank { null }
        val f = _uiState.value.filter
        _uiState.update { it.copy(filter = it.filter.copy(city = text)) }
        // Cities require both country and state; never query without them.
        val country = f.country
        val state = f.state
        if (country.isNullOrBlank() || state.isNullOrBlank()) return
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            val result = searchRepository.getCities(country, state, query = text)
            if (result is ApiResult.Success) {
                _uiState.update { it.copy(cityOptions = result.data) }
            }
        }
    }

    /** A city suggestion was tapped — commit it exactly, no further query needed. */
    fun onCitySelected(city: String) {
        locationJob?.cancel()
        _uiState.update { it.copy(filter = it.filter.copy(city = city.ifBlank { null })) }
    }

    fun onResetFilter() {
        locationJob?.cancel()
        _uiState.update {
            it.copy(
                filter = SearchFilter(),
                isStyleExpanded = false,
                stateOptions = emptyList(),
                cityOptions = emptyList(),
            )
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 350L
    }
}