package com.rinx.artRINXapp.feature.home.data.repository

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.network.toApiError
import com.rinx.artRINXapp.core.util.BlockedArtworkStore
import com.rinx.artRINXapp.core.util.BlockedUsersStore
import com.rinx.artRINXapp.core.util.LikeStore
import com.rinx.artRINXapp.feature.home.data.remote.HomeApiService
import com.rinx.artRINXapp.feature.home.data.remote.dto.ArtworkDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.ArtworkSizeDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.BannerDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.CurationDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.LikeArtworkRequest
import com.rinx.artRINXapp.feature.home.data.remote.dto.LikeCurationRequest
import com.rinx.artRINXapp.feature.home.domain.model.ArtworkItem
import com.rinx.artRINXapp.feature.home.domain.model.BannerItem
import com.rinx.artRINXapp.feature.home.domain.model.CurationItem
import com.rinx.artRINXapp.feature.home.domain.model.FeedPost
import com.rinx.artRINXapp.feature.home.domain.model.HomeFeed
import com.rinx.artRINXapp.feature.home.domain.model.Paged
import com.rinx.artRINXapp.feature.home.domain.model.ShoppablePost
import com.rinx.artRINXapp.feature.home.domain.repository.HomeRepository
import androidx.annotation.VisibleForTesting
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val apiService: HomeApiService,
    private val blockedStore: BlockedArtworkStore,
    private val blockedUsersStore: BlockedUsersStore,
    private val likeStore: LikeStore,
) : HomeRepository {

    // SWR cache — survives navigation (this is @Singleton); cleared on logout/delete.
    @Volatile private var feedCache: HomeFeed? = null
    @Volatile private var discoverCache: List<FeedPost>? = null
    @Volatile private var shopCache: List<ShoppablePost>? = null
    @Volatile private var forYouCache: List<FeedPost>? = null

    // Strip any blocked artwork (by id) AND any blocked user's content (by owner/author) from cached
    // reads so a re-seed / back-navigation never resurfaces it before the next network refresh.
    override fun cachedFeed(): HomeFeed? = feedCache?.let { f ->
        f.copy(
            newArt = f.newArt.filterNot { blockedStore.isBlocked(it.id) || isUserBlocked(it.ownerId, it.artistId) },
            posts = f.posts.filterNot { blockedStore.isBlocked(it.id) || isUserBlocked(it.ownerId) }
                .map { it.withLike(confirm = false) },
            recentlyViewed = f.recentlyViewed.filterNot { blockedStore.isBlocked(it.id) || isUserBlocked(it.ownerId, it.artistId) },
            curations = f.curations.filterNot { isUserBlocked(it.authorId) }
                .map { it.stripBlocked().withLike(confirm = false) },
        )
    }
    override fun cachedDiscover(): List<FeedPost>? =
        discoverCache?.filterNot { blockedStore.isBlocked(it.id) || isUserBlocked(it.ownerId) }
            ?.map { it.withLike(confirm = false) }
    override fun cachedShop(): List<ShoppablePost>? =
        shopCache?.filterNot { blockedStore.isBlocked(it.id) || isUserBlocked(it.ownerId, it.artistId) }
            ?.map { it.withLike(confirm = false) }
    override fun cachedForYou(): List<FeedPost>? =
        forYouCache?.filterNot { blockedStore.isBlocked(it.id) || isUserBlocked(it.ownerId) }
            ?.map { it.withLike(confirm = false) }
    override fun clearCache() {
        feedCache = null
        discoverCache = null
        shopCache = null
        forYouCache = null
    }

    override fun updateCachedLike(artworkId: Int, isLiked: Boolean, likeCount: Int) {
        val idStr = artworkId.toString()
        feedCache = feedCache?.let { f ->
            f.copy(posts = f.posts.map {
                if (it.id == idStr) it.copy(isLiked = isLiked, likeCount = likeCount) else it
            })
        }
        discoverCache = discoverCache?.map {
            if (it.id == idStr) it.copy(isLiked = isLiked, likeCount = likeCount) else it
        }
        shopCache = shopCache?.map {
            if (it.id == idStr) it.copy(isLiked = isLiked, likeCount = likeCount) else it
        }
        forYouCache = forYouCache?.map {
            if (it.id == idStr) it.copy(isLiked = isLiked, likeCount = likeCount) else it
        }
    }

    override suspend fun getDiscoverFeed(): ApiResult<HomeFeed> = safeCall {
        val response = apiService.getDiscoverFeed()
        if (response.isSuccessful) {
            val data = response.body()?.data
            val newArt = data?.newArt.orEmpty().notBlocked()
            val feed = HomeFeed(
                banners = data?.sponsored.orEmpty().map { it.toBannerItem() },
                newArt = newArt.map { it.toArtworkItem() },
                curations = data?.popularCurations.orEmpty().map { it.toCurationItem() }
                    .filterNot { isUserBlocked(it.authorId) }
                    .map { it.withLike(confirm = true) },
                posts = newArt.map { it.toFeedPost().withLike(confirm = true) },
                // Render in the exact order the backend returns recently_viewed.
                recentlyViewed = data?.recentlyViewed.orEmpty().notBlocked().map { it.toArtworkItem() },
            )
            feedCache = feed
            ApiResult.Success(feed)
        } else {
            errorFor(response)
        }
    }

    override suspend fun getDiscoverArtworks(page: Int, size: Int): ApiResult<Paged<FeedPost>> = safeCall {
        val response = apiService.getAllArtworks(page, size)
        if (response.isSuccessful) {
            val body = response.body()?.data
            val items = body?.items.orEmpty().notBlocked().map { it.toFeedPost().withLike(confirm = true) }
            if (page == PAGE) discoverCache = items // cache only the first page (what the tab seeds from)
            ApiResult.Success(Paged(items, page, size, body?.total ?: 0))
        } else {
            errorFor(response)
        }
    }

    override suspend fun getShopArtworks(page: Int, size: Int): ApiResult<Paged<ShoppablePost>> = safeCall {
        val response = apiService.getShopArtworks(page, size)
        if (response.isSuccessful) {
            val body = response.body()?.data
            val items = body?.items.orEmpty().notBlocked().map { it.toShoppablePost().withLike(confirm = true) }
            if (page == PAGE) shopCache = items // cache only the first page (what the tab seeds from)
            ApiResult.Success(Paged(items, page, size, body?.total ?: 0))
        } else {
            errorFor(response)
        }
    }

    override suspend fun getRecommendedArtworks(page: Int, size: Int): ApiResult<Paged<FeedPost>> = safeCall {
        val response = apiService.getRecommendedArtworks(page, size)
        if (response.isSuccessful) {
            val body = response.body()?.data
            val items = body?.items.orEmpty().notBlocked().map { it.toFeedPost().withLike(confirm = true) }
            if (page == PAGE) forYouCache = items // cache only the first page (what the tab seeds from)
            ApiResult.Success(Paged(items, page, size, body?.total ?: 0))
        } else {
            errorFor(response)
        }
    }

    override suspend fun likeArtwork(artworkId: Int): ApiResult<Unit> = safeCall {
        val response = apiService.likeArtwork(LikeArtworkRequest(artworkId))
        if (response.isSuccessful) ApiResult.Success(Unit) else errorFor(response)
    }

    override suspend fun unlikeArtwork(artworkId: Int): ApiResult<Unit> = safeCall {
        val response = apiService.unlikeArtwork(artworkId)
        if (response.isSuccessful) ApiResult.Success(Unit) else errorFor(response)
    }

    // ── Detail screens ──────────────────────────────────────────────────────

    override suspend fun getArtworkDetail(id: Int): ApiResult<ShoppablePost> = safeCall {
        val response = apiService.getArtwork(id)
        val dto = response.body()?.data
        if (response.isSuccessful && dto != null) {
            // A blocked artwork — or one by a blocked owner/artist — is treated as GONE, so it can't
            // be opened from cache, a deep link, or a stale list item (parity with the list filters).
            if (blockedStore.isBlocked(id) || isUserBlocked(dto.userId, dto.artist?.artistId)) {
                ApiResult.Error.NotFound("This artwork isn't available.")
            } else {
                ApiResult.Success(dto.toShoppablePost().withLike(confirm = true))
            }
        } else {
            errorFor(response)
        }
    }

    override suspend fun getSimilarArtworks(id: Int, page: Int, size: Int): ApiResult<Paged<ArtworkItem>> = safeCall {
        val response = apiService.getSimilarArtworks(id, page, size)
        if (response.isSuccessful) {
            val body = response.body()?.data
            val items = body?.items.orEmpty().notBlocked().map { it.toArtworkItem() }
            ApiResult.Success(Paged(items, page, size, body?.total ?: 0))
        } else {
            errorFor(response)
        }
    }

    override suspend fun getCurationDetail(id: Int): ApiResult<CurationItem> = safeCall {
        val response = apiService.getCuration(id)
        val dto = response.body()?.data
        if (response.isSuccessful && dto != null) {
            ApiResult.Success(dto.toCurationItem().withLike(confirm = true))
        } else {
            errorFor(response)
        }
    }

    override suspend fun getMoreCurations(): ApiResult<List<CurationItem>> = safeCall {
        val response = apiService.getAllCurations(PAGE, SIZE)
        if (response.isSuccessful) {
            ApiResult.Success(
                response.body()?.data?.items.orEmpty().map { it.toCurationItem().withLike(confirm = false) },
            )
        } else {
            errorFor(response)
        }
    }

    override suspend fun likeCuration(curationId: Int): ApiResult<Unit> = safeCall {
        val response = apiService.likeCuration(LikeCurationRequest(curationId))
        if (response.isSuccessful) ApiResult.Success(Unit) else errorFor(response)
    }

    override suspend fun unlikeCuration(curationId: Int): ApiResult<Unit> = safeCall {
        val response = apiService.unlikeCuration(curationId)
        if (response.isSuccessful) ApiResult.Success(Unit) else errorFor(response)
    }

    // ── Like overlay (LikeStore) ────────────────────────────────────────────────
    // Reconcile an item's like state with the user's own recorded intent. The ±1 count delta is
    // applied ONLY when the server/cached isLiked DISAGREES with the intent — on agreement the
    // server count is trusted as-is (no delta), so this composes with updateCachedLike and never
    // double-counts. [confirm] is set only on fresh network reads: when the server already agrees
    // with the stored intent we drop the entry, so the override is transient and the server becomes
    // authoritative again once it has caught up (a later change on another device is then respected).

    /** @return reconciled (isLiked, likeCount) for an artwork id, or the inputs unchanged. */
    private fun reconcileArtwork(id: String?, isLiked: Boolean, likeCount: Int, confirm: Boolean): Pair<Boolean, Int> {
        val key = id?.toIntOrNull() ?: return isLiked to likeCount
        val want = likeStore.artwork(key) ?: return isLiked to likeCount
        if (isLiked == want) {
            if (confirm) likeStore.clearArtwork(key)
            return isLiked to likeCount
        }
        return want to (likeCount + if (want) 1 else -1).coerceAtLeast(0)
    }

    private fun FeedPost.withLike(confirm: Boolean): FeedPost {
        val (liked, count) = reconcileArtwork(id, isLiked, likeCount, confirm)
        return if (liked == isLiked && count == likeCount) this else copy(isLiked = liked, likeCount = count)
    }

    private fun ShoppablePost.withLike(confirm: Boolean): ShoppablePost {
        val (liked, count) = reconcileArtwork(id, isLiked, likeCount, confirm)
        return if (liked == isLiked && count == likeCount) this else copy(isLiked = liked, likeCount = count)
    }

    private fun CurationItem.withLike(confirm: Boolean): CurationItem {
        val key = id.toIntOrNull() ?: return this
        val want = likeStore.curation(key) ?: return this
        if (isLiked == want) {
            if (confirm) likeStore.clearCuration(key)
            return this
        }
        return copy(isLiked = want, likeCount = (likeCount + if (want) 1 else -1).coerceAtLeast(0))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Drop blocked artworks from a DTO list (by artwork id, or by a blocked owner/artist) before mapping. */
    private fun List<ArtworkDto>.notBlocked(): List<ArtworkDto> =
        filterNot { (it.id != null && blockedStore.isBlocked(it.id)) || isUserBlocked(it.userId, it.artist?.artistId) }

    /** True if any of the given user ids belongs to a user I've blocked (nulls ignored). */
    private fun isUserBlocked(vararg ids: Int?): Boolean =
        ids.any { it != null && blockedUsersStore.isBlocked(it) }

    /** Remove any blocked artwork from a cached curation's (index-aligned) urls + ids. */
    private fun CurationItem.stripBlocked(): CurationItem {
        if (artworkUrls.isEmpty()) return this
        val keptUrls = ArrayList<String>(artworkUrls.size)
        val keptIds = ArrayList<String>(artworkIds.size)
        artworkUrls.indices.forEach { i ->
            val id = artworkIds.getOrNull(i)
            if (!blockedStore.isBlocked(id)) {
                keptUrls += artworkUrls[i]
                keptIds += (id ?: "")
            }
        }
        return copy(artworkUrls = keptUrls, artworkIds = keptIds)
    }

    private inline fun <T> safeCall(block: () -> ApiResult<T>): ApiResult<T> = try {
        block()
    } catch (e: IOException) {
        ApiResult.Error.Network(e)
    } catch (e: Exception) {
        ApiResult.Error.Unknown(e)
    }

    // Prefer the server's message (e.g. block/report reasons) over a generic fallback.
    private fun errorFor(response: Response<*>): ApiResult.Error = response.toApiError()

    // ── DTO → domain mappers ────────────────────────────────────────────────────

    private fun BannerDto.toBannerItem() = BannerItem(
        id = id?.toString() ?: "",
        imageUrl = imageUrl.orEmpty(),
        title = title.orEmpty(),
        artistName = artist?.let { "by $it" } ?: subtitle.orEmpty(),
        subtitle = subtitle.orEmpty(),
        url = link?.takeIf { it.isNotBlank() },
        isSponsored = true,
    )

    private fun ArtworkDto.bestImage(): String = imageUrl ?: webpUrl ?: thumbnailUrl ?: ""
    // Credit the artwork's named artist first (who the piece is BY), falling back to the uploader's
    // display name only when no credited artist is present.
    private fun ArtworkDto.artistDisplay(): String =
        artist?.artistName?.takeIf { it.isNotBlank() } ?: displayName ?: ""

    @VisibleForTesting
    internal fun ArtworkDto.toArtworkItem() = ArtworkItem(
        id = id?.toString() ?: "",
        imageUrl = bestImage(),
        title = title.orEmpty(),
        artistName = artistDisplay(),
        artistAvatarUrl = profilePictureUrl,
        ownerId = userId,
        artistId = artist?.artistId,
        aspectRatio = aspectRatio?.toFloat(),
    )

    @VisibleForTesting
    internal fun ArtworkDto.toFeedPost() = FeedPost(
        id = id?.toString() ?: "",
        artistName = artistDisplay(),
        artistHandle = artist?.artistName?.let { "@$it" } ?: "",
        artistRole = profileTypeName ?: "Artist",
        artistAvatarUrl = profilePictureUrl,
        imageUrl = bestImage(),
        title = title.orEmpty(),
        likeCount = likesCount ?: 0,
        isLiked = isLiked ?: false,
        ownerId = userId ?: artist?.artistId,
        ownerName = displayName?.takeIf { it.isNotBlank() } ?: artistDisplay(),
        aspectRatio = aspectRatio?.toFloat(),
    )

    @VisibleForTesting
    internal fun ArtworkDto.toShoppablePost() = ShoppablePost(
        id = id?.toString() ?: "",
        artistName = artistDisplay(),
        artistHandle = artist?.artistName?.let { "@$it" } ?: "",
        artistRole = profileTypeName ?: "Artist",
        artistAvatarUrl = profilePictureUrl,
        imageUrl = bestImage(),
        title = title.orEmpty(),
        medium = medium?.title.orEmpty(),
        description = description.orEmpty(),
        likeCount = likesCount ?: 0,
        isLiked = isLiked ?: false,
        shopUrl = shopLink.orEmpty(),
        price = price,
        ownerId = userId,
        ownerName = displayName?.takeIf { it.isNotBlank() } ?: artistDisplay(),
        artistId = artist?.artistId,
        dimensions = size?.toDimensionsDisplay(),
        aspectRatio = aspectRatio?.toFloat(),
    )

    /** Build a human-readable size string from the artwork's dimensions, or null if none are set. */
    @VisibleForTesting
    internal fun ArtworkSizeDto.toDimensionsDisplay(): String? {
        val h = heightCm?.trim()?.toDoubleOrNull()
        val w = widthCm?.trim()?.toDoubleOrNull()
        if (h == null && w == null) return null
        val u = unit?.trim()?.ifBlank { null } ?: "cm"
        fun fmt(n: Double) = if (n % 1.0 == 0.0) n.toLong().toString() else n.toString()
        return when {
            h != null && w != null -> "${fmt(h)} × ${fmt(w)} $u"
            h != null -> "Height: ${fmt(h)} $u"
            else -> "Width: ${fmt(w!!)} $u"
        }
    }

    @VisibleForTesting
    internal fun CurationDto.toCurationItem(): CurationItem {
        // Use the curation's natural artwork order from the API (do NOT sort). Sorting by id makes
        // every curation surface its lowest-id artworks first, so curations that share artworks
        // (common in the data) end up showing the same preview images. Natural order keeps each
        // curation's deck distinct and reflects its real 1-2-3 ordering.
        val ordered = artworks.orEmpty()
        val styleList = ordered.mapNotNull { it.medium?.title }.distinct()
        // Keep ids index-aligned with urls: filter the two together so a missing image can't shift them.
        // Also drop any blocked artwork so it never shows in the curation deck.
        val withImages = ordered.notBlocked().filter { (it.imageUrl ?: it.thumbnailUrl) != null }
        return CurationItem(
            id = id?.toString() ?: "",
            title = title.orEmpty(),
            curatorHandle = author?.username?.let { "@$it" } ?: "",
            curatorName = author?.displayName ?: author?.username ?: "Collector",
            curatorAvatarUrl = author?.profilePicture,
            artworkUrls = withImages.map { (it.imageUrl ?: it.thumbnailUrl)!! },
            artworkIds = withImages.map { it.id?.toString() ?: "" },
            // Blank when the API gives no real style/description — the UI hides those sections
            // rather than showing a fabricated placeholder (e.g. "Painting" for an empty curation).
            styles = styleList.joinToString(", "),
            description = description.orEmpty(),
            likeCount = likesCount ?: 0,
            isLiked = isLiked ?: false,
            authorId = author?.id,
        )
    }

    private companion object {
        const val PAGE = 1
        const val SIZE = 10
    }
}