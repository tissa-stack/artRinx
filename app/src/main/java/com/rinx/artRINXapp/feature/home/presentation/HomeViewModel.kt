package com.rinx.artRINXapp.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.home.data.local.CurationPreviewStore
import com.rinx.artRINXapp.feature.home.domain.model.BannerItem
import com.rinx.artRINXapp.feature.home.domain.model.FeedPost
import com.rinx.artRINXapp.feature.home.domain.model.ForYouItem
import com.rinx.artRINXapp.feature.home.domain.repository.HomeRepository
import com.rinx.artRINXapp.feature.upload.domain.CurationManager
import com.rinx.artRINXapp.feature.upload.domain.UploadManager
import com.rinx.artRINXapp.feature.upload.domain.model.CurationProgress
import com.rinx.artRINXapp.feature.upload.domain.model.UploadProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val curationPreviewStore: CurationPreviewStore,
    private val uploadManager: UploadManager,
    private val curationManager: CurationManager,
) : ViewModel() {

    // Seed synchronously from cache so returning to the tab renders instantly with no shimmer (SWR).
    private val _uiState = MutableStateFlow(seedFromCache())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private fun seedFromCache(): HomeUiState {
        val feed = repository.cachedFeed() ?: return HomeUiState(isLoading = true)
        return HomeUiState(
            isLoading = false,
            bannerItems = feed.banners,
            newArtItems = feed.newArt,
            popularCurations = feed.curations,
            feedItems = feed.posts,
            shoppableItems = repository.cachedShop().orEmpty(),
            // Prefer cached recommendations; fall back to discover posts until they load.
            forYouItems = buildForYou(repository.cachedForYou() ?: feed.posts, feed.banners),
            recentlyViewed = feed.recentlyViewed,
        )
    }

    init {
        load()
        observeUploads()
        observeCurations()
    }

    // ── Upload progress (PUBLIC uploads only) ──────────────────────────────────

    private fun observeUploads() {
        viewModelScope.launch {
            uploadManager.progress.collect { progress ->
                // Private uploads belong to the Profile screen, not the public feed.
                val forHome = progress?.takeUnless { it.isPrivate }
                _uiState.update { it.copy(uploadProgress = forHome) }

                if (forHome is UploadProgress.Success) {
                    _uiState.update { state ->
                        val newPost = forHome.toFeedPost()
                        state.copy(
                            feedItems = listOf(newPost) + state.feedItems.filterNot { it.id == newPost.id },
                            uploadProgress = null,
                            toastMessage = "Your artwork is now live.",
                        )
                    }
                    uploadManager.dismiss()
                }
            }
        }
    }

    private fun UploadProgress.Success.toFeedPost(): FeedPost = FeedPost(
        id = artwork.id.toString(),
        artistName = artistName,
        artistHandle = artistHandle,
        // CDN url if the backend already returned it, else the local thumbnail for instant render.
        imageUrl = artwork.imageUrl.ifEmpty { localThumb.toString() },
        title = title,
        likeCount = 0,
        isLiked = false,
    )

    fun onRetryUpload() = uploadManager.retry()
    fun onDismissUpload() = uploadManager.dismiss()

    // ── Curation progress (PUBLIC curations only) ──────────────────────────────

    private fun observeCurations() {
        viewModelScope.launch {
            curationManager.progress.collect { progress ->
                val forHome = progress?.takeUnless { it.isPrivate }
                _uiState.update { it.copy(curationProgress = forHome) }

                // Per product: a public curation surfaces in "Popular Curations" only on the next
                // feed refresh — we don't optimistically prepend. Just clear the row on success.
                if (forHome is CurationProgress.Success) {
                    _uiState.update { it.copy(curationProgress = null, toastMessage = "Your curation is now live.") }
                    curationManager.dismiss()
                }
            }
        }
    }

    fun onRetryCuration() = curationManager.retry()
    fun onDismissCuration() = curationManager.dismiss()

    private fun load(isRefresh: Boolean = false) {
        // Cached data already on screen → revalidate silently (no shimmer, no refresh spinner).
        val hasCache = repository.cachedFeed() != null
        viewModelScope.launch {
            _uiState.update {
                when {
                    isRefresh -> it.copy(isRefreshing = true, error = null) // explicit pull-to-refresh
                    hasCache -> it                                          // silent background refresh
                    else -> it.copy(isLoading = true, error = null)         // first load → shimmer
                }
            }

            // Discover tab comes from one call; shop + recommended feeds from others. Run concurrently.
            val feedJob = async { repository.getDiscoverFeed() }
            val shopJob = async { repository.getShopArtworks(PAGE, SIZE) }
            val recommendedJob = async { repository.getRecommendedArtworks(PAGE, SIZE) }
            val feedRes = feedJob.await()
            val shopRes = shopJob.await()
            val recommendedRes = recommendedJob.await()

            // The discover feed is the primary content — fail the screen only if it errored.
            if (feedRes is ApiResult.Error) {
                _uiState.update {
                    when {
                        isRefresh -> it.copy(isRefreshing = false) // keep content; stop the spinner
                        hasCache -> it                             // silent refresh failed → keep cache
                        else -> it.copy(
                            isLoading = false,
                            error = if (feedRes is ApiResult.Error.Network) HomeError.NoInternet
                            else HomeError.Generic(),
                        )
                    }
                }
                return@launch
            }

            val feed = (feedRes as ApiResult.Success).data

            // Remember each curation's preview image order so its detail screen can open with the
            // same first images the user saw on the home deck.
            feed.curations.forEach { curationPreviewStore.put(it.id, it.artworkUrls) }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                    bannerItems = feed.banners,
                    newArtItems = feed.newArt,
                    popularCurations = feed.curations,
                    feedItems = feed.posts,
                    shoppableItems = (shopRes as? ApiResult.Success)?.data.orEmpty(),
                    // For You = personalized recommendations; fall back to discover posts if the
                    // recommendation call failed or returned nothing, so the tab is never empty.
                    forYouItems = buildForYou(
                        (recommendedRes as? ApiResult.Success)?.data?.takeIf { it.isNotEmpty() } ?: feed.posts,
                        feed.banners,
                    ),
                    // Recorded server-side when a detail screen calls the similar-artworks endpoint;
                    // returned here in the same discover-feed payload.
                    recentlyViewed = feed.recentlyViewed,
                )
            }
        }
    }

    /**
     * "For You" tab content. Only real API posts are shown — sponsored banners are NOT interleaved
     * (per product: Shop/For You must show only actual API data, no banners in between).
     */
    private fun buildForYou(posts: List<FeedPost>, banners: List<BannerItem>): List<ForYouItem> =
        posts.map { ForYouItem.Post(it) }

    fun onTabSelected(tab: HomeTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    /** Clear the one-shot success toast after the screen has shown it. */
    fun onToastShown() = _uiState.update { it.copy(toastMessage = null) }

    fun onRetry() {
        load()
    }

    /** Pull-to-refresh: re-run the load with the lightweight spinner. */
    fun refresh() {
        load(isRefresh = true)
    }

    // ── Like (Discover + For You feeds) ────────────────────────────────────────

    fun onLikeToggled(postId: String) {
        val post = _uiState.value.feedItems.find { it.id == postId } ?: return
        val nowLiked = !post.isLiked
        setFeedLiked(postId, nowLiked)
        viewModelScope.launch {
            val id = postId.toIntOrNull() ?: return@launch
            val result = if (nowLiked) repository.likeArtwork(id) else repository.unlikeArtwork(id)
            if (result is ApiResult.Error) setFeedLiked(postId, !nowLiked)   // revert on failure
        }
    }

    private fun setFeedLiked(postId: String, liked: Boolean) {
        _uiState.update { state ->
            state.copy(
                feedItems = state.feedItems.map { it.applyLike(postId, liked) },
                forYouItems = state.forYouItems.map { item ->
                    if (item is ForYouItem.Post && item.post.id == postId) {
                        ForYouItem.Post(item.post.applyLike(postId, liked))
                    } else {
                        item
                    }
                },
            )
        }
    }

    private fun FeedPost.applyLike(targetId: String, liked: Boolean): FeedPost =
        if (id == targetId) {
            copy(isLiked = liked, likeCount = (likeCount + if (liked) 1 else -1).coerceAtLeast(0))
        } else {
            this
        }

    // ── Like (Shop feed) ───────────────────────────────────────────────────────

    fun onShopLikeToggled(postId: String) {
        val post = _uiState.value.shoppableItems.find { it.id == postId } ?: return
        val nowLiked = !post.isLiked
        setShopLiked(postId, nowLiked)
        viewModelScope.launch {
            val id = postId.toIntOrNull() ?: return@launch
            val result = if (nowLiked) repository.likeArtwork(id) else repository.unlikeArtwork(id)
            if (result is ApiResult.Error) setShopLiked(postId, !nowLiked)
        }
    }

    private fun setShopLiked(postId: String, liked: Boolean) {
        _uiState.update { state ->
            state.copy(
                shoppableItems = state.shoppableItems.map {
                    if (it.id == postId) {
                        it.copy(isLiked = liked, likeCount = (it.likeCount + if (liked) 1 else -1).coerceAtLeast(0))
                    } else {
                        it
                    }
                },
            )
        }
    }

    private companion object {
        const val PAGE = 1
        const val SIZE = 10
    }
}