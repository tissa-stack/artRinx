package com.rinx.artRINXapp.feature.home.domain.model

/**
 * A single page of a paginated feed. [total] is the full server-side result count and [size] is the
 * page size we requested — [endReached] uses `page * size >= total` (NOT the post-filter [items]
 * count) so blocked-content filtering can't cause a premature stop or an infinite empty-page loop.
 */
data class Paged<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val total: Int,
) {
    // Ended when this page came back empty, or the server's total says we've covered everything.
    // Guard on `total > 0` so an absent/zero total (some endpoints omit it) doesn't falsely end the
    // feed after a full first page — a later empty page will then stop it (≤1 extra request).
    val endReached: Boolean get() = items.isEmpty() || (total > 0 && page * size >= total)
}
