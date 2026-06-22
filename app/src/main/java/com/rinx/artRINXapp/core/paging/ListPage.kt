package com.rinx.artRINXapp.core.paging

import androidx.compose.runtime.Immutable
import com.rinx.artRINXapp.core.network.ApiResult

/**
 * Infinite-scroll bookkeeping for a simple page/size list feed.
 *
 * [page] is the most recently loaded page (1-based); [hasMore] is false once a short page comes back;
 * [isLoadingMore] gates concurrent loads and drives the footer spinner; [loadMoreError] holds a
 * user-facing message when the *next-page* fetch fails (the footer then shows it with a Retry button
 * and auto-loading pauses until the user retries).
 */
@Immutable
data class ListPage(
    val page: Int = 1,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val loadMoreError: String? = null,
) {
    /** True when a scroll-triggered next-page load should be skipped. */
    val blocked: Boolean get() = isLoadingMore || !hasMore || loadMoreError != null
}

/** Short footer message for a failed next-page fetch (distinct from full first-load error copy). */
fun ApiResult.Error.toLoadMoreMessage(): String = when (this) {
    is ApiResult.Error.Network -> "No internet connection."
    else -> "Couldn't load more."
}
