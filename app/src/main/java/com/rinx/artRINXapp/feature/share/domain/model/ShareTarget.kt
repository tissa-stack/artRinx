package com.rinx.artRINXapp.feature.share.domain.model

/** The kind of entity being shared. [wireType] is the value sent to the backend / used in links. */
enum class ShareKind(val wireType: String) {
    ARTWORK("artwork"),
    CURATION("curation"),
    PROFILE("profile"),
}

/**
 * A unified description of whatever the user is sharing (an artwork, a curation, or a profile),
 * carrying just what the share sheet needs: a title, an optional subtitle (e.g. "by Jane" /
 * "@handle"), and an optional thumbnail/avatar.
 */
data class ShareTarget(
    val kind: ShareKind,
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val imageUrl: String? = null,
) {
    /** Canonical public URL for "Copy link" / "Share to…" — matches the deep-link parser. */
    val webUrl: String get() = "https://www.artrinx.com/${kind.wireType}/$id"
}
