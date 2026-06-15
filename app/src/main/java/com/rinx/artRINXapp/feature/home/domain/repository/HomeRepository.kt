package com.rinx.artRINXapp.feature.home.domain.repository

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.home.domain.model.ArtworkItem
import com.rinx.artRINXapp.feature.home.domain.model.CurationItem
import com.rinx.artRINXapp.feature.home.domain.model.FeedPost
import com.rinx.artRINXapp.feature.home.domain.model.HomeFeed
import com.rinx.artRINXapp.feature.home.domain.model.ShoppablePost

interface HomeRepository {
    /** Discover tab — banners, new art, and popular curations in a single request. */
    suspend fun getDiscoverFeed(): ApiResult<HomeFeed>
    suspend fun getShopArtworks(page: Int, size: Int): ApiResult<List<ShoppablePost>>

    /** "For You" tab — personalized recommendations (GET /api/artworks/recommended). */
    suspend fun getRecommendedArtworks(page: Int, size: Int): ApiResult<List<FeedPost>>

    // ── In-memory SWR cache (survives navigation; cleared on logout/delete) ───
    /** Last successful discover feed, or null if never loaded this session. */
    fun cachedFeed(): HomeFeed?
    /** Last successful shop page, or null. */
    fun cachedShop(): List<ShoppablePost>?
    /** Last successful "For You" recommendations page, or null. */
    fun cachedForYou(): List<FeedPost>?
    /** Drop the cached feed/shop/for-you. */
    fun clearCache()
    /**
     * Patch the like state of a single artwork across the in-memory feed/shop/for-you caches so a
     * later SWR re-seed reflects a like made elsewhere (e.g. on the detail screen). No-op if the
     * artwork isn't cached. Carries absolute values (idempotent).
     */
    fun updateCachedLike(artworkId: Int, isLiked: Boolean, likeCount: Int)

    suspend fun likeArtwork(artworkId: Int): ApiResult<Unit>
    suspend fun unlikeArtwork(artworkId: Int): ApiResult<Unit>

    // ── Detail screens ──────────────────────────────────────────────────────
    suspend fun getArtworkDetail(id: Int): ApiResult<ShoppablePost>
    suspend fun getSimilarArtworks(id: Int): ApiResult<List<ArtworkItem>>
    suspend fun getCurationDetail(id: Int): ApiResult<CurationItem>
    suspend fun getMoreCurations(): ApiResult<List<CurationItem>>
    suspend fun likeCuration(curationId: Int): ApiResult<Unit>
    suspend fun unlikeCuration(curationId: Int): ApiResult<Unit>
}