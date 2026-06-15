package com.rinx.artRINXapp.feature.profile.presentation.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.util.ProfileRefreshBus
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileArtItem
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileCurationItem
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileTab
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import com.rinx.artRINXapp.feature.upload.domain.CurationManager
import com.rinx.artRINXapp.feature.upload.domain.UploadManager
import com.rinx.artRINXapp.feature.upload.domain.model.CurationProgress
import com.rinx.artRINXapp.feature.upload.domain.model.UploadProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val uploadManager: UploadManager,
    private val curationManager: CurationManager,
    private val profileRefreshBus: ProfileRefreshBus,
) : ViewModel() {

    // Seed synchronously from cache so returning to the Profile tab renders instantly (SWR).
    private val _uiState = MutableStateFlow(seedFromCache())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    // Next page to fetch per tab (page 1 is loaded by load()).
    private var artPage = PAGE
    private var curationPage = PAGE
    private var likedPage = PAGE

    private fun seedFromCache(): UserProfileUiState {
        val profile = profileRepository.cachedProfileData() ?: return UserProfileUiState(isLoading = true)
        val art = profileRepository.cachedMyArtworks().orEmpty()
        val curations = profileRepository.cachedMyCurations().orEmpty()
        val liked = profileRepository.cachedLikedArtworks().orEmpty()
        return UserProfileUiState(
            isLoading = false,
            profile = profile,
            artItems = art,
            curations = curations,
            likedItems = liked,
            artHasMore = art.size >= SIZE,
            curationHasMore = curations.size >= SIZE,
            likedHasMore = liked.size >= SIZE,
        )
    }

    init {
        load()
        observeUploads()
        observeCurations()
        observeRefreshes()
    }

    private fun load() {
        // Cache already on screen → this is a silent revalidate (isLoading stays false, no shimmer).
        viewModelScope.launch { fetchAll() }
    }

    /** Manual pull-to-refresh: revalidate everything and drive the refresh spinner until done. */
    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            fetchAll()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun fetchAll() {
        coroutineScope {
            // Fetch header, art, curations and liked concurrently.
            val profileDeferred = async { profileRepository.getProfileData() }
            val artworksDeferred = async { profileRepository.getMyArtworks(PAGE, SIZE) }
            val curationsDeferred = async { profileRepository.getMyCurations(PAGE, SIZE) }
            val likedDeferred = async { profileRepository.getLikedArtworks(PAGE, SIZE) }

            val profile = (profileDeferred.await() as? ApiResult.Success)?.data
            val artRes = artworksDeferred.await() as? ApiResult.Success
            val curationRes = curationsDeferred.await() as? ApiResult.Success
            val likedRes = likedDeferred.await() as? ApiResult.Success

            // Reset paging on a full (re)load. A full page implies there may be more.
            artPage = PAGE; curationPage = PAGE; likedPage = PAGE
            _uiState.update { state ->
                // Keep existing (cached) items if a sub-fetch failed — never blank good data.
                val artItems = artRes?.data ?: state.artItems
                val curations = curationRes?.data ?: state.curations
                val likedItems = likedRes?.data ?: state.likedItems
                state.copy(
                    isLoading = false,
                    profile = profile ?: state.profile,
                    artItems = artItems,
                    curations = curations,
                    likedItems = likedItems,
                    artHasMore = if (artRes != null) artItems.size >= SIZE else state.artHasMore,
                    curationHasMore = if (curationRes != null) curations.size >= SIZE else state.curationHasMore,
                    likedHasMore = if (likedRes != null) likedItems.size >= SIZE else state.likedHasMore,
                )
            }
        }
    }

    /** Infinite scroll: load the next page for the active tab, append (dedup by id), stop when short. */
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore) return
        val tab = state.activeTab
        val hasMore = when (tab) {
            ProfileTab.ART -> state.artHasMore
            ProfileTab.CURATIONS -> state.curationHasMore
            ProfileTab.LIKED -> state.likedHasMore
        }
        if (!hasMore) return
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            // Advance the page + recompute hasMore ONLY on success. On a transient error we keep the
            // page index and hasMore untouched and just clear the spinner, so the next scroll retries
            // the same page instead of skipping it and permanently ending pagination.
            when (tab) {
                ProfileTab.ART -> when (val res = profileRepository.getMyArtworks(artPage + 1, SIZE)) {
                    is ApiResult.Success -> {
                        artPage += 1
                        _uiState.update { st ->
                            val existing = st.artItems.associateBy { it.id }
                            st.copy(
                                artItems = st.artItems + res.data.filter { it.id !in existing },
                                artHasMore = res.data.size >= SIZE,
                                isLoadingMore = false,
                            )
                        }
                    }
                    is ApiResult.Error -> _uiState.update { it.copy(isLoadingMore = false) }
                }
                ProfileTab.CURATIONS -> when (val res = profileRepository.getMyCurations(curationPage + 1, SIZE)) {
                    is ApiResult.Success -> {
                        curationPage += 1
                        _uiState.update { st ->
                            val existing = st.curations.associateBy { it.id }
                            st.copy(
                                curations = st.curations + res.data.filter { it.id !in existing },
                                curationHasMore = res.data.size >= SIZE,
                                isLoadingMore = false,
                            )
                        }
                    }
                    is ApiResult.Error -> _uiState.update { it.copy(isLoadingMore = false) }
                }
                ProfileTab.LIKED -> when (val res = profileRepository.getLikedArtworks(likedPage + 1, SIZE)) {
                    is ApiResult.Success -> {
                        likedPage += 1
                        _uiState.update { st ->
                            val existing = st.likedItems.associateBy { it.id }
                            st.copy(
                                likedItems = st.likedItems + res.data.filter { it.id !in existing },
                                likedHasMore = res.data.size >= SIZE,
                                isLoadingMore = false,
                            )
                        }
                    }
                    is ApiResult.Error -> _uiState.update { it.copy(isLoadingMore = false) }
                }
            }
        }
    }

    /** Reload when an artwork/curation is edited or deleted elsewhere (detail/edit flows). */
    private fun observeRefreshes() {
        viewModelScope.launch {
            profileRefreshBus.events.collect { load() }
        }
    }

    // ── Upload progress (PRIVATE uploads only) ─────────────────────────────────

    private fun observeUploads() {
        viewModelScope.launch {
            uploadManager.progress.collect { progress ->
                val forProfile = progress?.takeIf { it.isPrivate }
                _uiState.update { it.copy(uploadProgress = forProfile) }

                if (forProfile is UploadProgress.Success) {
                    _uiState.update { state ->
                        val newItem = forProfile.toProfileArtItem()
                        val deduped = state.artItems.filterNot { it.id == newItem.id }
                        state.copy(
                            artItems = listOf(newItem) + deduped,
                            profile = state.profile?.let { it.copy(artCount = it.artCount + 1) },
                            uploadProgress = null,
                        )
                    }
                    uploadManager.dismiss()
                }
            }
        }
    }

    private fun UploadProgress.Success.toProfileArtItem(): ProfileArtItem = ProfileArtItem(
        id = artwork.id.toString(),
        imageRes = null,
        imageUrl = artwork.imageUrl.ifEmpty { localThumb.toString() },
        title = title,
        artistName = artistName,
        isPrivate = true,
    )

    fun onRetryUpload() = uploadManager.retry()
    fun onDismissUpload() = uploadManager.dismiss()

    // ── Curation progress (PRIVATE curations only) ─────────────────────────────

    private fun observeCurations() {
        viewModelScope.launch {
            curationManager.progress.collect { progress ->
                val forProfile = progress?.takeIf { it.isPrivate }
                _uiState.update { it.copy(curationProgress = forProfile) }

                if (forProfile is CurationProgress.Success) {
                    _uiState.update { state ->
                        val newItem = forProfile.toProfileCurationItem()
                        val deduped = state.curations.filterNot { it.id == newItem.id }
                        state.copy(
                            curations = listOf(newItem) + deduped,
                            profile = state.profile?.let { it.copy(curationCount = it.curationCount + 1) },
                            curationProgress = null,
                        )
                    }
                    curationManager.dismiss()
                }
            }
        }
    }

    private fun CurationProgress.Success.toProfileCurationItem(): ProfileCurationItem = ProfileCurationItem(
        id = curation.id.toString(),
        title = title,
        handle = "",
        artworkRes = emptyList(),
        isPrivate = true,
        artworkUrls = artworkUrls,
    )

    fun onRetryCuration() = curationManager.retry()
    fun onDismissCuration() = curationManager.dismiss()

    fun onTabSelected(tab: ProfileTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun onBioExpandToggle() {
        _uiState.update { it.copy(isBioExpanded = !it.isBioExpanded) }
    }

    private companion object {
        const val PAGE = 1
        const val SIZE = 30
    }
}
