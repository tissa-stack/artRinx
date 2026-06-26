package com.rinx.artRINXapp.core.util

import android.content.Context
import android.content.Intent

/** Opens the Android share sheet (social-media chooser) with plain [text]. No-op if blank. */
fun Context.shareText(text: String, chooserTitle: String = "Share via") {
    if (text.isBlank()) return
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(
        Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/** Share an artwork/post: title + artist, plus description and shop link when available. */
fun Context.shareArtwork(
    title: String,
    artistName: String?,
    description: String? = null,
    link: String? = null,
) {
    val text = buildString {
        append(title.ifBlank { "Check out this artwork on artRINX" })
        if (!artistName.isNullOrBlank()) append(" by $artistName")
        if (!description.isNullOrBlank()) append("\n\n").also { append(description) }
        if (!link.isNullOrBlank()) append("\n\n").also { append(link) }
    }
    shareText(text)
}

/**
 * Share any entity (artwork / curation / profile) to the system chooser: a title, optional
 * subtitle (e.g. "by Jane" or "@handle"), and the canonical link. Used by the in-app share sheet's
 * "Share to…" action.
 */
fun Context.shareEntity(title: String, subtitle: String?, link: String) {
    val text = buildString {
        append(title.ifBlank { "Check this out on artRINX" })
        if (!subtitle.isNullOrBlank()) append(" ").also { append(subtitle) }
        if (link.isNotBlank()) append("\n\n").also { append(link) }
    }
    shareText(text)
}

/** Share a curation: title + curator, plus description when available. */
fun Context.shareCuration(
    title: String,
    curatorName: String?,
    description: String? = null,
) {
    val text = buildString {
        append(title.ifBlank { "Check out this collection on artRINX" })
        if (!curatorName.isNullOrBlank()) append(" by $curatorName")
        if (!description.isNullOrBlank()) append("\n\n").also { append(description) }
    }
    shareText(text)
}
