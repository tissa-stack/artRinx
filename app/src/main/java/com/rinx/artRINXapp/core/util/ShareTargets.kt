package com.rinx.artRINXapp.core.util

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

/** An installed app that can receive a text/plain share, for the custom share-target row. */
data class ShareAppTarget(
    val label: String,
    val icon: Drawable,
    val packageName: String,
    val activityName: String,
)

/** Common messaging/social apps surfaced first in the share row; everything else sorts by label. */
private val SHARE_PRIORITY = listOf(
    "com.whatsapp",
    "com.whatsapp.w4b",
    "com.google.android.apps.messaging", // Messages
    "org.telegram.messenger",
    "com.facebook.orca",                 // Messenger
    "com.instagram.android",
    "com.google.android.gm",             // Gmail
)

/**
 * The shared text payload (title + optional subtitle + link). Kept identical to [shareEntity] so the
 * custom per-app share row and the system "More" chooser send the same content.
 */
fun buildShareText(title: String, subtitle: String?, link: String): String = buildString {
    append(title.ifBlank { "Check this out on artRINX" })
    if (!subtitle.isNullOrBlank()) append(" ").also { append(subtitle) }
    if (link.isNotBlank()) append("\n\n").also { append(link) }
}

/**
 * Installed apps that can handle a `text/plain` [Intent.ACTION_SEND], as [ShareAppTarget]s for a custom
 * share row. Requires the `<queries>` ACTION_SEND/text-plain declaration in the manifest (Android 11+
 * package visibility). Excludes our own app, de-dupes by package, and orders common apps first.
 *
 * Note: this is app-level only — it cannot surface another app's per-contact "Direct Share" targets;
 * those remain reachable via the system chooser ("More").
 */
fun Context.resolveShareTargets(): List<ShareAppTarget> {
    val pm = packageManager
    val intent = Intent(Intent.ACTION_SEND).setType("text/plain")
    val resolved = runCatching { pm.queryIntentActivities(intent, 0) }.getOrDefault(emptyList())
    return resolved.asSequence()
        .mapNotNull { ri ->
            val act = ri.activityInfo ?: return@mapNotNull null
            if (act.packageName == packageName) return@mapNotNull null
            ShareAppTarget(
                label = ri.loadLabel(pm).toString(),
                icon = ri.loadIcon(pm),
                packageName = act.packageName,
                activityName = act.name,
            )
        }
        .distinctBy { it.packageName }
        .sortedWith(
            compareBy(
                { SHARE_PRIORITY.indexOf(it.packageName).let { i -> if (i < 0) Int.MAX_VALUE else i } },
                { it.label.lowercase() },
            ),
        )
        .toList()
}

/** Launch [text] straight into [target]'s share screen; falls back to the system chooser on failure. */
fun Context.shareTextTo(target: ShareAppTarget, text: String) {
    if (text.isBlank()) return
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setClassName(target.packageName, target.activityName)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }.onFailure { shareText(text) }
}
