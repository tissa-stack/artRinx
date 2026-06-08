package com.rinx.artRINXapp.feature.home.data.local

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory hand-off of the artwork-image order shown in a curation's HOME preview deck, keyed by
 * curation id. The detail screen reads this to open its card-stack with the SAME first images the
 * user saw in the preview (the home feed and the detail endpoint don't guarantee the same artwork
 * order). Images not present in the preview simply load afterwards in the detail's own order.
 *
 * Best-effort and ephemeral: if empty (e.g. the curation wasn't reached via a preview), the detail
 * just uses its natural order.
 */
@Singleton
class CurationPreviewStore @Inject constructor() {

    private val previewOrder = ConcurrentHashMap<String, List<String>>()

    fun put(curationId: String, imageUrls: List<String>) {
        if (curationId.isNotEmpty() && imageUrls.isNotEmpty()) {
            previewOrder[curationId] = imageUrls
        }
    }

    fun orderFor(curationId: String): List<String> = previewOrder[curationId].orEmpty()
}
