package com.rinx.artRINXapp.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.network.ConnectivityChecker
import com.rinx.artRINXapp.core.util.BlockedArtworkBus
import com.rinx.artRINXapp.core.util.BlockedUserBus
import com.rinx.artRINXapp.core.util.LikeBus
import com.rinx.artRINXapp.feature.home.data.local.CurationPreviewStore
import com.rinx.artRINXapp.feature.home.domain.LikeManager
import com.rinx.artRINXapp.feature.home.domain.model.BannerItem
import com.rinx.artRINXapp.feature.home.domain.model.CurationItem
import com.rinx.artRINXapp.feature.home.domain.model.FeedPost
import com.rinx.artRINXapp.feature.home.domain.model.ForYouItem
import com.rinx.artRINXapp.feature.home.domain.repository.HomeRepository
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import com.rinx.artRINXapp.feature.upload.domain.CurationManager
import com.rinx.artRINXapp.feature.upload.domain.UploadManager
import com.rinx.artRINXapp.feature.upload.domain.model.CurationProgress
import com.rinx.artRINXapp.feature.upload.domain.model.UploadProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
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
    private val likeBus: LikeBus,
    private val likeManager: LikeManager,
    private val blockedArtworkBus: BlockedArtworkBus,
    private val blockedUserBus: BlockedUserBus,
    private val profileRepository: ProfileRepository,
    private val connectivity: ConnectivityChecker,
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
            // Discover vertical feed = /artworks/all (cached page 1); fall back to discover posts.
            feedItems = repository.cachedDiscover() ?: feed.posts,
            shoppableItems = repository.cachedShop().orEmpty(),
            // Prefer cached recommendations; fall back to discover posts until they load.
            forYouItems = buildForYou(repository.cachedForYou() ?: feed.posts, feed.banners),
            recentlyViewed = feed.recentlyViewed,
        )
    }

    init {
        load()
        seedBlockedUsers()
        observeUploads()
        observeCurations()
        observeLikes()
        observeBlocks()
        observeUserBlocks()
    }

    /**
     * Record who I've blocked into [BlockedUsersStore] on HOME entry (cold-start AND in-session
     * re-login, since LocalDataCleaner wipes the store on sign-in and HomeViewModel is recreated on
     * each HOME navigation). This makes ProfileRepository's blockedProfileFallback fire for users I
     * blocked in a prior session, so their profile always shows the "Profile Blocked" panel instead
     * of a generic error / the neutral "not available" state. Fire-and-forget; failures are harmless.
     */
    private fun seedBlockedUsers() {
        viewModelScope.launch {
            runCatching { profileRepository.getBlockedUsers(1, 100) }
        }
    }

    /** Drop a blocked artwork from every list the moment it's blocked (no refresh wait). */
    private fun observeBlocks() {
        viewModelScope.launch {
            blockedArtworkBus.events.collect { blockedId ->
                val idStr = blockedId.toString()
                _uiState.update { state ->
                    state.copy(
                        feedItems = state.feedItems.filterNot { it.id == idStr },
                        forYouItems = state.forYouItems.filterNot {
                            it is ForYouItem.Post && it.post.id == idStr
                        },
                        shoppableItems = state.shoppableItems.filterNot { it.id == idStr },
                        newArtItems = state.newArtItems.filterNot { it.id == idStr },
                        recentlyViewed = state.recentlyViewed.filterNot { it.id == idStr },
                        // Also strip it from any curation preview deck on the feed.
                        popularCurations = state.popularCurations.map { it.removeArtwork(idStr) },
                    )
                }
            }
        }
        // Unblock → the store no longer filters it; silently revalidate so the art returns to the feed.
        viewModelScope.launch {
            blockedArtworkBus.unblocked.collect { load() }
        }
    }

    /** Drop every artwork/curation owned by a user the moment I block them (no refresh wait). */
    private fun observeUserBlocks() {
        viewModelScope.launch {
            blockedUserBus.events.collect { blockedUserId ->
                _uiState.update { state ->
                    state.copy(
                        feedItems = state.feedItems.filterNot { it.ownerId == blockedUserId },
                        forYouItems = state.forYouItems.filterNot {
                            it is ForYouItem.Post && it.post.ownerId == blockedUserId
                        },
                        shoppableItems = state.shoppableItems.filterNot {
                            it.ownerId == blockedUserId
                        },
                        newArtItems = state.newArtItems.filterNot {
                            it.ownerId == blockedUserId
                        },
                        recentlyViewed = state.recentlyViewed.filterNot {
                            it.ownerId == blockedUserId
                        },
                        // Drop whole curations authored by the blocked user (deck artworks lack a per-art
                        // owner, so a blocked user's single piece inside someone else's deck clears on refresh).
                        popularCurations = state.popularCurations.filterNot { it.authorId == blockedUserId },
                    )
                }
            }
        }
    }

    /** Remove a single artwork (by id) from a curation's index-aligned preview urls + ids. */
    private fun CurationItem.removeArtwork(artworkId: String): CurationItem {
        if (artworkId !in artworkIds) return this
        val keptUrls = ArrayList<String>(artworkUrls.size)
        val keptIds = ArrayList<String>(artworkIds.size)
        artworkUrls.indices.forEach { i ->
            if (artworkIds.getOrNull(i) != artworkId) {
                keptUrls += artworkUrls[i]
                keptIds += (artworkIds.getOrNull(i) ?: "")
            }
        }
        return copy(artworkUrls = keptUrls, artworkIds = keptIds)
    }

    /** Converge with likes made elsewhere (e.g. the detail screen) while this feed is live. */
    private fun observeLikes() {
        viewModelScope.launch {
            likeBus.events.collect { u -> applyLikeUpdate(u.id.toString(), u.isLiked, u.likeCount) }
        }
    }

    /** Set the absolute like state of an artwork wherever it appears (idempotent). */
    private fun applyLikeUpdate(id: String, isLiked: Boolean, likeCount: Int) {
        _uiState.update { state ->
            state.copy(
                feedItems = state.feedItems.map {
                    if (it.id == id) it.copy(isLiked = isLiked, likeCount = likeCount) else it
                },
                forYouItems = state.forYouItems.map { item ->
                    if (item is ForYouItem.Post && item.post.id == id) {
                        ForYouItem.Post(item.post.copy(isLiked = isLiked, likeCount = likeCount))
                    } else {
                        item
                    }
                },
                shoppableItems = state.shoppableItems.map {
                    if (it.id == id) it.copy(isLiked = isLiked, likeCount = likeCount) else it
                },
            )
        }
    }

    // ── Upload progress (PUBLIC uploads only) ──────────────────────────────────

    private fun observeUploads() {
        // Tracks whether a public upload is currently active, so we can detect the idle → active
        // edge (the moment an upload starts) and react only once per upload.
        var uploadActive = false
        viewModelScope.launch {
            uploadManager.progress.collect { progress ->
                // Private uploads belong to the Profile screen, not the public feed.
                val forHome = progress?.takeUnless { it.isPrivate }

                // The progress ROW now lives on the Create screen — CreateViewModel owns display AND
                // dismiss(). Home stays PASSIVE: it must not set a row and must NOT dismiss() (that
                // would starve the Create row of its terminal state via the shared conflated flow).
                // We still jump to Discover on the idle → active edge so the optimistic post below
                // lands on the tab the user sees when they next open Home.
                val justStarted = forHome != null && !uploadActive
                uploadActive = forHome != null

                if (justStarted) _uiState.update { it.copy(activeTab = HomeTab.DISCOVER) }

                if (forHome is UploadProgress.Success) {
                    _uiState.update { state ->
                        val newPost = forHome.toFeedPost()
                        state.copy(
                            feedItems = listOf(newPost) + state.feedItems.filterNot { it.id == newPost.id },
                            toastMessage = "Your artwork is now live.",
                        )
                    }
                }
            }
        }
    }

    private fun UploadProgress.Success.toFeedPost(): FeedPost {
        // The card header shows the UPLOADER (= current user), not the credited artist. The upload
        // result has no owner avatar/name, so seed them from the cached profile — otherwise the
        // optimistic card briefly shows a placeholder avatar + wrong name until the next refresh.
        val me = profileRepository.cachedProfileData()
        return FeedPost(
            id = artwork.id.toString(),
            artistName = artistName,
            artistHandle = artistHandle,
            artistRole = me?.role ?: "Artist",
            artistAvatarUrl = me?.avatarUrl,
            ownerName = me?.displayName.orEmpty(),
            // CDN url if the backend already returned it, else the local thumbnail for instant render.
            imageUrl = artwork.imageUrl.ifEmpty { localThumb.toString() },
            title = title,
            likeCount = 0,
            isLiked = false,
        )
    }

    fun onRetryUpload() = uploadManager.retry()
    fun onDismissUpload() = uploadManager.dismiss()

    // ── Curation progress (PUBLIC curations only) ──────────────────────────────

    private fun observeCurations() {
        viewModelScope.launch {
            curationManager.progress.collect { progress ->
                val forHome = progress?.takeUnless { it.isPrivate }

                // Row + dismiss are owned by the Create screen now (see observeUploads). Home stays
                // passive. Per product: a public curation surfaces in "Popular Curations" only on the
                // next feed refresh — we don't optimistically prepend. Just toast on success.
                if (forHome is CurationProgress.Success) {
                    _uiState.update { it.copy(toastMessage = "Your collection is now live.") }
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
            // On a first load with no cache (the only case that would show the offline screen), ride
            // out a transient network failure — e.g. DNS not yet resolvable just after the device
            // wakes — so we recover into content instead of flashing "offline".
            val retryTransient = !hasCache && !isRefresh
            val feedJob = async { withResumeRetry(retryTransient) { repository.getDiscoverFeed() } }
            val discoverJob = async { withResumeRetry(retryTransient) { repository.getDiscoverArtworks(PAGE, SIZE) } }
            val shopJob = async { withResumeRetry(retryTransient) { repository.getShopArtworks(PAGE, SIZE) } }
            val recommendedJob = async { withResumeRetry(retryTransient) { repository.getRecommendedArtworks(PAGE, SIZE) } }
            val feedRes = feedJob.await()
            val discoverRes = discoverJob.await()
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

            val discoverPage = (discoverRes as? ApiResult.Success)?.data
            val shopPage = (shopRes as? ApiResult.Success)?.data
            val forYouPage = (recommendedRes as? ApiResult.Success)?.data

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = null,
                    bannerItems = feed.banners,
                    newArtItems = feed.newArt,
                    popularCurations = feed.curations,
                    // Discover vertical feed = /artworks/all; fall back to discover posts only if it
                    // failed/empty so the tab is never blank.
                    feedItems = discoverPage?.items?.takeIf { l -> l.isNotEmpty() } ?: feed.posts,
                    shoppableItems = shopPage?.items.orEmpty(),
                    // For You = personalized recommendations; fall back to discover posts if the
                    // recommendation call failed or returned nothing, so the tab is never empty.
                    forYouItems = buildForYou(
                        forYouPage?.items?.takeIf { l -> l.isNotEmpty() } ?: feed.posts,
                        feed.banners,
                    ),
                    // Recorded server-side when a detail screen calls the similar-artworks endpoint;
                    // returned here in the same discover-feed payload.
                    recentlyViewed = feed.recentlyViewed,
                    // Reset paging — page 1 just loaded; next is page 2. If a feed failed to load
                    // (null page), don't end it: allow a later loadMore to retry from page 2.
                    discoverPaging = PageState(nextPage = 2, endReached = discoverPage?.endReached ?: false),
                    shopPaging = PageState(nextPage = 2, endReached = shopPage?.endReached ?: false),
                    forYouPaging = PageState(nextPage = 2, endReached = forYouPage?.endReached ?: false),
                )
            }
        }
    }

    /**
     * Infinite scroll: load the next page for [tab] and append (de-duped by id). No-op while the
     * first load is running, while a page is already in flight, or once the end is reached.
     */
    fun loadMore(tab: HomeTab) {
        val state = _uiState.value
        if (state.isLoading) return
        val paging = when (tab) {
            HomeTab.DISCOVER -> state.discoverPaging
            HomeTab.SHOP -> state.shopPaging
            HomeTab.FOR_YOU -> state.forYouPaging
        }
        // Don't auto-load while a page is in flight, at the end, or sitting on a failed page — a
        // failed page waits for an explicit [retryLoadMore] so scrolling can't spam retries.
        if (paging.isLoadingMore || paging.endReached || paging.loadMoreError != null) return

        setLoadingMore(tab, true)
        viewModelScope.launch {
            val page = paging.nextPage
            when (tab) {
                HomeTab.DISCOVER -> {
                    val res = repository.getDiscoverArtworks(page, SIZE)
                    _uiState.update { s ->
                        if (res is ApiResult.Success) {
                            val merged = appendDistinct(s.feedItems, res.data.items) { it.id }
                            s.copy(feedItems = merged, discoverPaging = s.discoverPaging.copy(
                                nextPage = page + 1, isLoadingMore = false,
                                endReached = res.data.endReached, loadMoreError = null,
                            ))
                        } else {
                            s.copy(discoverPaging = s.discoverPaging.copy(
                                isLoadingMore = false, loadMoreError = (res as ApiResult.Error).toHomeError(),
                            ))
                        }
                    }
                }
                HomeTab.SHOP -> {
                    val res = repository.getShopArtworks(page, SIZE)
                    _uiState.update { s ->
                        if (res is ApiResult.Success) {
                            val merged = appendDistinct(s.shoppableItems, res.data.items) { it.id }
                            s.copy(shoppableItems = merged, shopPaging = s.shopPaging.copy(
                                nextPage = page + 1, isLoadingMore = false,
                                endReached = res.data.endReached, loadMoreError = null,
                            ))
                        } else {
                            s.copy(shopPaging = s.shopPaging.copy(
                                isLoadingMore = false, loadMoreError = (res as ApiResult.Error).toHomeError(),
                            ))
                        }
                    }
                }
                HomeTab.FOR_YOU -> {
                    val res = repository.getRecommendedArtworks(page, SIZE)
                    _uiState.update { s ->
                        if (res is ApiResult.Success) {
                            val existing = s.forYouItems.mapNotNull { (it as? ForYouItem.Post)?.post }
                            val merged = appendDistinct(existing, res.data.items) { it.id }
                            s.copy(forYouItems = buildForYou(merged, s.bannerItems), forYouPaging = s.forYouPaging.copy(
                                nextPage = page + 1, isLoadingMore = false,
                                endReached = res.data.endReached, loadMoreError = null,
                            ))
                        } else {
                            s.copy(forYouPaging = s.forYouPaging.copy(
                                isLoadingMore = false, loadMoreError = (res as ApiResult.Error).toHomeError(),
                            ))
                        }
                    }
                }
            }
        }
    }

    /** Clear a failed page's error and try the same page again (the footer Retry button). */
    fun retryLoadMore(tab: HomeTab) {
        _uiState.update { s ->
            when (tab) {
                HomeTab.DISCOVER -> s.copy(discoverPaging = s.discoverPaging.copy(loadMoreError = null))
                HomeTab.SHOP -> s.copy(shopPaging = s.shopPaging.copy(loadMoreError = null))
                HomeTab.FOR_YOU -> s.copy(forYouPaging = s.forYouPaging.copy(loadMoreError = null))
            }
        }
        loadMore(tab)
    }

    private fun ApiResult.Error.toHomeError(): HomeError =
        if (this is ApiResult.Error.Network) HomeError.NoInternet else HomeError.Generic()

    private fun setLoadingMore(tab: HomeTab, loading: Boolean) {
        _uiState.update { s ->
            when (tab) {
                HomeTab.DISCOVER -> s.copy(discoverPaging = s.discoverPaging.copy(isLoadingMore = loading))
                HomeTab.SHOP -> s.copy(shopPaging = s.shopPaging.copy(isLoadingMore = loading))
                HomeTab.FOR_YOU -> s.copy(forYouPaging = s.forYouPaging.copy(isLoadingMore = loading))
            }
        }
    }

    /** Append [next] onto [current], skipping any whose [key] already appears (overlap/refresh-safe). */
    private fun <T> appendDistinct(current: List<T>, next: List<T>, key: (T) -> String): List<T> {
        if (next.isEmpty()) return current
        val seen = current.mapTo(HashSet()) { key(it) }
        return current + next.filter { seen.add(key(it)) }
    }

    /**
     * Retry a *transient* network failure (e.g. DNS not yet resolvable in the moments right after the
     * device wakes) with short backoff — but only while the device actually has a network, so a real
     * offline returns immediately. Only invoked on the first, cache-less load (the sole path that
     * would otherwise show the offline screen); success/refresh/cached loads pass [retry] = false and
     * behave exactly as before. The shimmer stays up during retries (no error is set until they're
     * exhausted), so the user never sees a false "offline" flash.
     */
    private suspend fun <T> withResumeRetry(
        retry: Boolean,
        call: suspend () -> ApiResult<T>,
    ): ApiResult<T> {
        var res = call()
        if (!retry) return res
        var i = 0
        while (res is ApiResult.Error.Network && connectivity.isOnline() && i < RESUME_RETRY_DELAYS_MS.size) {
            delay(RESUME_RETRY_DELAYS_MS[i])
            i++
            res = call()
        }
        return res
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
        val id = postId.toIntOrNull() ?: return
        // The tap can originate from Discover OR For You, which are SEPARATE lists — resolve from
        // whichever holds the post. Keying off feedItems alone silently dropped For-You-only likes
        // (recommendations that aren't also in the Discover feed), so those taps did nothing.
        val post = _uiState.value.feedItems.find { it.id == postId }
            ?: _uiState.value.forYouItems.firstNotNullOfOrNull {
                (it as? ForYouItem.Post)?.post?.takeIf { p -> p.id == postId }
            }
            ?: return
        val nowLiked = !post.isLiked
        setFeedLiked(postId, nowLiked) // instant local feedback (updates both feed + For You)
        // Durable state + network run on LikeManager's app scope so leaving the tab can't cancel them;
        // a hard-failure revert comes back through LikeBus → applyLikeUpdate.
        val newCount = (post.likeCount + if (nowLiked) 1 else -1).coerceAtLeast(0)
        likeManager.toggleArtwork(id, nowLiked, newCount)
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
        val id = postId.toIntOrNull() ?: return
        val nowLiked = !post.isLiked
        setShopLiked(postId, nowLiked) // instant local feedback
        val newCount = _uiState.value.shoppableItems.find { it.id == postId }?.likeCount ?: post.likeCount
        likeManager.toggleArtwork(id, nowLiked, newCount)
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

        // Backoff for riding out a transient network failure on a cold/resume load (~5.5s total).
        // Covers the brief window where the just-woken device can't resolve DNS yet.
        val RESUME_RETRY_DELAYS_MS = longArrayOf(300, 700, 1500, 3000)
    }
}