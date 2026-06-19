package com.rinx.artRINXapp.feature.home.data.repository

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.network.toApiError
import com.rinx.artRINXapp.core.util.BlockedArtworkStore
import com.rinx.artRINXapp.core.util.BlockedUsersStore
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
) : HomeRepository {

    // SWR cache — survives navigation (this is @Singleton); cleared on logout/delete.
    @Volatile private var feedCache: HomeFeed? = null
    @Volatile private var shopCache: List<ShoppablePost>? = null
    @Volatile private var forYouCache: List<FeedPost>? = null

    // Strip any blocked artwork (by id) AND any blocked user's content (by owner/author) from cached
    // reads so a re-seed / back-navigation never resurfaces it before the next network refresh.
    override fun cachedFeed(): HomeFeed? = feedCache?.let { f ->
        f.copy(
            newArt = f.newArt.filterNot { blockedStore.isBlocked(it.id) || isUserBlocked(it.ownerId, it.artistId) },
            posts = f.posts.filterNot { blockedStore.isBlocked(it.id) || isUserBlocked(it.ownerId) },
            recentlyViewed = f.recentlyViewed.filterNot { blockedStore.isBlocked(it.id) || isUserBlocked(it.ownerId, it.artistId) },
            curations = f.curations.filterNot { isUserBlocked(it.authorId) }.map { it.stripBlocked() },
        )
    }
    override fun cachedShop(): List<ShoppablePost>? =
        shopCache?.filterNot { blockedStore.isBlocked(it.id) || isUserBlocked(it.ownerId, it.artistId) }
    override fun cachedForYou(): List<FeedPost>? =
        forYouCache?.filterNot { blockedStore.isBlocked(it.id) || isUserBlocked(it.ownerId) }
    override fun clearCache() {
        feedCache = null
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
                    .filterNot { isUserBlocked(it.authorId) },
                posts = newArt.map { it.toFeedPost() },
                // Render in the exact order the backend returns recently_viewed.
                recentlyViewed = data?.recentlyViewed.orEmpty().notBlocked().map { it.toArtworkItem() },
            )
            feedCache = feed
            ApiResult.Success(feed)
        } else {
            errorFor(response)
        }
    }

    override suspend fun getShopArtworks(page: Int, size: Int): ApiResult<List<ShoppablePost>> = safeCall {
        val response = apiService.getShopArtworks(page, size)
        if (response.isSuccessful) {
            val items = response.body()?.data?.items.orEmpty().notBlocked().map { it.toShoppablePost() }
            if (page == PAGE) shopCache = items // cache only the first page (what the tab seeds from)
            ApiResult.Success(items)
        } else {
            errorFor(response)
        }
    }

    override suspend fun getRecommendedArtworks(page: Int, size: Int): ApiResult<List<FeedPost>> = safeCall {
        val response = apiService.getRecommendedArtworks(page, size)
        if (response.isSuccessful) {
            val items = response.body()?.data?.items.orEmpty().notBlocked().map { it.toFeedPost() }
            if (page == PAGE) forYouCache = items // cache only the first page (what the tab seeds from)
            ApiResult.Success(items)
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
            ApiResult.Success(dto.toShoppablePost())
        } else {
            errorFor(response)
        }
    }

    override suspend fun getSimilarArtworks(id: Int): ApiResult<List<ArtworkItem>> = safeCall {
        val response = apiService.getSimilarArtworks(id, PAGE, SIZE)
        if (response.isSuccessful) {
            ApiResult.Success(response.body()?.data?.items.orEmpty().notBlocked().map { it.toArtworkItem() })
        } else {
            errorFor(response)
        }
    }

    override suspend fun getCurationDetail(id: Int): ApiResult<CurationItem> = safeCall {
        val response = apiService.getCuration(id)
        val dto = response.body()?.data
        if (response.isSuccessful && dto != null) {
            ApiResult.Success(dto.toCurationItem())
        } else {
            errorFor(response)
        }
    }

    override suspend fun getMoreCurations(): ApiResult<List<CurationItem>> = safeCall {
        val response = apiService.getAllCurations(PAGE, SIZE)
        if (response.isSuccessful) {
            ApiResult.Success(response.body()?.data?.items.orEmpty().map { it.toCurationItem() })
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
            curatorName = author?.displayName ?: author?.username ?: "Curator",
            curatorAvatarUrl = author?.profilePicture,
            artworkUrls = withImages.map { (it.imageUrl ?: it.thumbnailUrl)!! },
            artworkIds = withImages.map { it.id?.toString() ?: "" },
            styles = styleList.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "Painting",
            description = description ?: "A carefully curated collection of remarkable artworks.",
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