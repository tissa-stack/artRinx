package com.rinx.artRINXapp.feature.notifications.presentation.notifications

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.notifications.domain.model.NotificationItem
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val REVEAL_WIDTH = 160.dp
private const val SNAP_OPEN_THRESHOLD = -60f   // drag this far in dp to snap open

@Composable
fun SwipeableNotificationItem(
    item: NotificationItem,
    onDelete: () -> Unit,
    onMarkRead: () -> Unit,
    onClick: () -> Unit = {},
) {
    val scope        = rememberCoroutineScope()
    val density      = LocalDensity.current
    // Convert 160dp → pixels so offset and reveal width are in the same unit
    val revealPx     = with(density) { REVEAL_WIDTH.toPx() }
    val snapThreshPx = with(density) { SNAP_OPEN_THRESHOLD.dp.toPx() }
    val offsetPx     = remember { Animatable(0f) }
    val d            = LocalDimens.current

    Box(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {

        // ── Action buttons revealed on swipe ──────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(REVEAL_WIDTH),
        ) {
            // Mark read — BrandPrimary (left half)
            Box(
                modifier         = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(BrandPrimary)
                    .clickable {
                        scope.launch { offsetPx.animateTo(0f) }
                        onMarkRead()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Outlined.MailOutline,
                    contentDescription = "Mark read",
                    tint               = Color.White,
                    modifier           = Modifier.size(Spacing.xxl),
                )
            }
            // Delete — red (right half)
            Box(
                modifier         = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFFE53935))
                    .clickable {
                        scope.launch { offsetPx.animateTo(0f) }
                        onDelete()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint               = Color.White,
                    modifier           = Modifier.size(Spacing.xxl),
                )
            }
        }

        // ── Notification row (slides left) ────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetPx.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state       = rememberDraggableState { delta ->
                        scope.launch {
                            val new = (offsetPx.value + delta).coerceIn(-revealPx, 0f)
                            offsetPx.snapTo(new)
                        }
                    },
                    onDragStopped = {
                        scope.launch {
                            if (offsetPx.value < snapThreshPx)
                                offsetPx.animateTo(-revealPx)
                            else
                                offsetPx.animateTo(0f)
                        }
                    },
                )
                // Tap routes by notification type (set by the caller); horizontal drag still
                // reveals the mark-read / delete actions.
                .clickable { onClick() }
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            // Thumbnail (square art) takes priority over the actor avatar (circle).
            val thumbModel:  Any? = item.thumbnailUrl ?: item.thumbnailRes
            if (thumbModel != null) {
                AsyncImage(
                    model              = thumbModel,
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .size(d.avatarSizeLg)
                        .clip(RoundedCornerShape(Spacing.xs)),
                )
            } else {
                // Circular actor avatar — image, then initials-on-pastel, then person icon.
                com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar(
                    url = item.avatarUrl,
                    fallbackRes = item.avatarRes,
                    contentDescription = null,
                    size = d.avatarSizeLg,
                    name = item.actorName,
                )
            }

            Spacer(Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = item.message,
                    style      = MaterialTheme.typography.bodySmall,
                    fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.SemiBold,
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
