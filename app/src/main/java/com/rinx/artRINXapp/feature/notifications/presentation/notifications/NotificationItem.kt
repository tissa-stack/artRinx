package com.rinx.artRINXapp.feature.notifications.presentation.notifications

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.notifications.domain.model.NotificationItem
import com.rinx.artRINXapp.feature.notifications.domain.model.NotificationKind
import com.rinx.artRINXapp.feature.notifications.presentation.SharedContentPreview
import com.rinx.artRINXapp.feature.notifications.presentation.components.RowActionsMenu
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar

/**
 * A non-event notification row: long-press opens a menu (Mark as read / Delete notification) anchored
 * to the row, with the row highlighted while the menu is open. The leading preview is
 * content-appropriate (curation → fan of cards, artwork → thumbnail, follow/profile → actor avatar),
 * and the message's named components (actor, content owner, title) are highlighted and navigable.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotificationRow(
    item: NotificationItem,
    preview: SharedContentPreview?,
    menuOpen: Boolean,
    onLongPress: () -> Unit,
    onDismissMenu: () -> Unit,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit,
    onLoadPreview: () -> Unit,
    onOpenProfile: (Long) -> Unit,
    onOpenArt: (Long) -> Unit,
    onOpenCuration: (Long) -> Unit,
    onClick: () -> Unit = {},
) {
    val d = LocalDimens.current

    val isCuration = item.kind == NotificationKind.CURATION_SHARE || item.kind == NotificationKind.CURATION_LIKE
    val isArtwork  = item.kind == NotificationKind.ARTWORK_SHARE || item.kind == NotificationKind.ARTWORK_LIKE

    // Resolve the owner + preview images once for curation/artwork rows (deduped in the ViewModel).
    if (isCuration || isArtwork) {
        LaunchedEffect(item.targetId) { onLoadPreview() }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        RowActionsMenu(
            expanded = menuOpen,
            deleteLabel = "Delete notification",
            onMarkRead = onMarkRead,
            onDelete = onDelete,
            onDismiss = onDismissMenu,
        )

        // ── Notification row (long-press → menu; highlighted while open) ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (menuOpen) BrandPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                )
                .combinedClickable(onClick = onClick, onLongClick = onLongPress)
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            // ── Leading preview ───────────────────────────────────────────
            when {
                // Curation → a small fan of the curation's artwork cards (falls back to the single
                // thumbnail / actor avatar while the preview resolves). Tap → curation detail.
                isCuration -> {
                    val urls = preview?.imageUrls?.takeIf { it.isNotEmpty() }
                        ?: listOfNotNull(item.thumbnailUrl)
                    MiniCurationFan(
                        imageUrls = urls,
                        size = d.avatarSizeLg,
                        onClick = { item.targetId?.let(onOpenCuration) },
                    )
                }
                // Artwork → its square thumbnail. Tap → art detail.
                isArtwork -> {
                    val url = preview?.imageUrls?.firstOrNull() ?: item.thumbnailUrl
                    Box(
                        modifier = Modifier
                            .size(d.avatarSizeLg)
                            .clip(RoundedCornerShape(Spacing.xs))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { item.targetId?.let(onOpenArt) },
                    ) {
                        if (url != null) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
                // Follow / profile-share / unknown → the actor's circular avatar. Tap → actor profile.
                else -> RinxAvatar(
                    url = item.avatarUrl,
                    fallbackRes = item.avatarRes,
                    contentDescription = null,
                    size = d.avatarSizeLg,
                    name = item.actorName,
                    modifier = Modifier.clip(CircleShape).clickable { item.actorId?.let(onOpenProfile) },
                )
            }

            Spacer(Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = buildNotificationMessage(item, preview, onOpenProfile, onOpenArt, onOpenCuration),
                    style      = MaterialTheme.typography.bodySmall,
                    color      = MaterialTheme.colorScheme.onBackground,
                    maxLines   = 3,
                    overflow   = TextOverflow.Ellipsis,
                )
                Text(
                    text     = item.timeAgo,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

/** A tappable highlighted span: the [text] substring inside the message links to [onClick]. */
private data class MsgSpan(val text: String?, val onClick: () -> Unit)

/**
 * Build the message as an [AnnotatedString] with each known component (actor / owner / title)
 * highlighted (BrandPrimary, SemiBold) and clickable. Spans whose text is blank or not found in the
 * server message are skipped — so wording differences degrade gracefully to plain text.
 */
private fun buildNotificationMessage(
    item: NotificationItem,
    preview: SharedContentPreview?,
    onOpenProfile: (Long) -> Unit,
    onOpenArt: (Long) -> Unit,
    onOpenCuration: (Long) -> Unit,
): AnnotatedString {
    val message = item.message
    val unreadWeight = if (item.isRead) FontWeight.Normal else FontWeight.SemiBold

    val title = (preview?.title ?: item.targetTitle)
    val spans = buildList {
        // The actor (who liked / shared / followed) → their profile.
        item.actorId?.let { add(MsgSpan(item.actorName) { onOpenProfile(it) }) }
        when (item.kind) {
            NotificationKind.CURATION_SHARE, NotificationKind.CURATION_LIKE -> {
                preview?.ownerId?.let { oid -> add(MsgSpan(preview.ownerName) { onOpenProfile(oid) }) }
                item.targetId?.let { tid -> add(MsgSpan(title) { onOpenCuration(tid) }) }
            }
            NotificationKind.ARTWORK_SHARE, NotificationKind.ARTWORK_LIKE -> {
                preview?.ownerId?.let { oid -> add(MsgSpan(preview.ownerName) { onOpenProfile(oid) }) }
                item.targetId?.let { tid -> add(MsgSpan(title) { onOpenArt(tid) }) }
            }
            NotificationKind.PROFILE_SHARE -> {
                // The shared profile (target) → that profile.
                item.targetId?.let { tid -> add(MsgSpan(item.targetName) { onOpenProfile(tid) }) }
            }
            else -> Unit
        }
    }

    // Resolve each span to a non-overlapping [start,end) range, earliest-first.
    val matches = spans
        .mapNotNull { s ->
            val t = s.text?.trim().orEmpty()
            if (t.isEmpty()) return@mapNotNull null
            val idx = message.indexOf(t)
            if (idx < 0) null else Triple(idx, idx + t.length, s)
        }
        .sortedBy { it.first }

    val ranges = mutableListOf<Triple<Int, Int, MsgSpan>>()
    var lastEnd = 0
    for (m in matches) if (m.first >= lastEnd) { ranges.add(m); lastEnd = m.second }

    if (ranges.isEmpty()) {
        return buildAnnotatedString { withStyle(SpanStyle(fontWeight = unreadWeight)) { append(message) } }
    }

    return buildAnnotatedString {
        var cursor = 0
        for ((start, end, span) in ranges) {
            if (start > cursor) {
                withStyle(SpanStyle(fontWeight = unreadWeight)) { append(message.substring(cursor, start)) }
            }
            withLink(
                LinkAnnotation.Clickable(
                    tag = "notif",
                    linkInteractionListener = { span.onClick() },
                ),
            ) {
                // Same color as the rest of the message — just bold to mark the tappable name/title.
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(message.substring(start, end))
                }
            }
            cursor = end
        }
        if (cursor < message.length) {
            withStyle(SpanStyle(fontWeight = unreadWeight)) { append(message.substring(cursor)) }
        }
    }
}

/** Compact overlapping fan of up to 3 curation artwork images (mirrors CollectionCard at row size). */
@Composable
private fun MiniCurationFan(
    imageUrls: List<String>,
    size: Dp,
    onClick: () -> Unit,
) {
    val previews = imageUrls.take(3)
    val count = previews.size.coerceAtLeast(1)
    // Occupy the SAME square footprint as the other rows' leading image (avatar / artwork thumb).
    val totalWidth = size
    val imageWidth = if (count <= 1) totalWidth else totalWidth * 0.7f
    val stackOffset = if (count <= 1) 0.dp else (totalWidth - imageWidth) / (count - 1)

    Box(
        modifier = Modifier.width(totalWidth).height(size).clickable { onClick() },
        contentAlignment = Alignment.TopStart,
    ) {
        if (previews.isEmpty()) {
            Box(
                Modifier
                    .width(imageWidth)
                    .height(size)
                    .clip(RoundedCornerShape(Spacing.xs))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        } else {
            previews.indices.reversed().forEach { index ->
                Box(
                    Modifier
                        .width(imageWidth)
                        .height(size)
                        .offset(x = stackOffset * index.toFloat())
                        .zIndex((previews.size - index).toFloat())
                        .clip(RoundedCornerShape(Spacing.xs))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    AsyncImage(
                        model = previews[index],
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
