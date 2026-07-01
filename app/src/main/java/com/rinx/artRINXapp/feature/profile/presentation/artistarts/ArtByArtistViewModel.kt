package com.rinx.artRINXapp.feature.profile.presentation.artistarts

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.paging.ListPage
import com.rinx.artRINXapp.core.paging.toLoadMoreMessage
import com.rinx.artRINXapp.feature.home.presentation.HomeError
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileArtItem
import com.rinx.artRINXapp.feature.profile.domain.model.PublicProfile
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ArtByArtistUiState(
    val artistName: String = "",
    /** True when the credited artist has a RINX profile (→ avatar + follow + tap-to-profile). */
    val hasProfile: Boolean = false,
    val profile: PublicProfile? = null,
    val arts: List<ProfileArtItem> = emptyList(),
    val artsPaging: ListPage = ListPage(),
    val isFollowing: Boolean = false,
    val isLoading: Boolean = true,
    /** Full-screen first-load error (no cached data to show). null once arts are on screen. */
    val error: HomeError? = null,
)

@HiltViewModel
class ArtByArtistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val artistName: String = savedStateHandle.get<String>("artistName").orEmpty()
    private val artistId: Int? = savedStateHandle.get<String>("artistId")?.toIntOrNull()

    private val _state = MutableStateFlow(
        ArtByArtistUiState(artistName = artistName, hasProfile = artistId != null),
    )
    val state: StateFlow<ArtByArtistUiState> = _state.asStateFlow()

    /** The profile id to open when the header is tapped (null when the artist has no RINX profile). */
    val profileId: Int? = artistId

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val artsJob = async { profileRepository.getArtworksByName(artistName, PAGE, SIZE) }
            val profJob = artistId?.let { id -> async { profileRepository.getPublicProfile(id) } }
            val artsRes = artsJob.await()
            val prof = (profJob?.await() as? ApiResult.Success)?.data
            val arts = (artsRes as? ApiResult.Success)?.data.orEmpty()
            _state.update {
                it.copy(
                    isLoading = false,
                    arts = arts,
                    artsPaging = ListPage(page = PAGE, hasMore = arts.size >= SIZE),
                    profile = prof,
                    isFollowing = prof?.isFollowing ?: false,
                    // Full-screen error only on a first-load failure with nothing to show.
                    // A 404 from by-name means "no (public) works credited to this artist" — an EMPTY
                    // result, not a failure (e.g. the only work is private). Show the empty state, not
                    // a Retry error (retrying would just re-404).
                    error = (artsRes as? ApiResult.Error)
                        ?.takeIf { it !is ApiResult.Error.NotFound && arts.isEmpty() }
                        ?.toHomeError(),
                )
            }
        }
    }

    /** Infinite scroll: load the next page, append (dedup by id), stop when a short page comes back. */
    fun loadMore() {
        val s = _state.value
        // blocked = isLoadingMore || !hasMore || loadMoreError != null
        if (s.isLoading || s.artsPaging.blocked) return
        _state.update { it.copy(artsPaging = it.artsPaging.copy(isLoadingMore = true)) }
        viewModelScope.launch {
            // Advance the page + clear the error ONLY on success. On a transient failure we keep the
            // page index so the next attempt retries the same page instead of skipping it, and surface
            // a footer message + Retry (auto-loading pauses via `blocked` until the user retries).
            val next = _state.value.artsPaging.page + 1
            when (val res = profileRepository.getArtworksByName(artistName, next, SIZE)) {
                is ApiResult.Success -> _state.update { st ->
                    val existing = st.arts.associateBy { it.id }
                    st.copy(
                        arts = st.arts + res.data.filter { it.id !in existing },
                        artsPaging = st.artsPaging.copy(
                            page = next,
                            hasMore = res.data.size >= SIZE,
                            isLoadingMore = false,
                            loadMoreError = null,
                        ),
                    )
                }
                // A 404 on a later page = no more results (end of list), not a footer error.
                is ApiResult.Error.NotFound -> _state.update { st ->
                    st.copy(
                        artsPaging = st.artsPaging.copy(
                            hasMore = false,
                            isLoadingMore = false,
                            loadMoreError = null,
                        ),
                    )
                }
                is ApiResult.Error -> _state.update { st ->
                    st.copy(
                        artsPaging = st.artsPaging.copy(
                            isLoadingMore = false,
                            loadMoreError = res.toLoadMoreMessage(),
                        ),
                    )
                }
            }
        }
    }

    /** Footer Retry: clear the failed-page error and try the same page again. */
    fun retryLoadMore() {
        _state.update { it.copy(artsPaging = it.artsPaging.copy(loadMoreError = null)) }
        loadMore()
    }

    fun onRetry() = load()

    private fun ApiResult.Error.toHomeError(): HomeError =
        if (this is ApiResult.Error.Network) HomeError.NoInternet else HomeError.Generic()

    private companion object {
        const val PAGE = 1
        const val SIZE = 30
    }
}
