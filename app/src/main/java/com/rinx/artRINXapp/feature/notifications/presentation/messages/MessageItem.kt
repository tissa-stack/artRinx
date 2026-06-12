package com.rinx.artRINXapp.feature.notifications.presentation.messages

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.notifications.domain.model.ConversationItem
import com.rinx.artRINXapp.feature.notifications.domain.model.ConversationState
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val MSG_REVEAL_WIDTH = 160.dp

@Composable
fun SwipeableMessageItem(
    item: ConversationItem,
    onClick: () -> Unit,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit,
) {
    val scope    = rememberCoroutineScope()
    val density  = LocalDensity.current
    val revealPx = with(density) { MSG_REVEAL_WIDTH.toPx() }
    val threshPx = revealPx * 0.4f
    val offsetPx = remember(item.id) { Animatable(0f) }
    val d        = LocalDimens.current

    Box(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {

        // ── Action buttons ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(MSG_REVEAL_WIDTH),
        ) {
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
                Icon(Icons.Outlined.MailOutline, "Mark read",
                    tint = Color.White, modifier = Modifier.size(Spacing.xxl))
            }
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
                Icon(Icons.Filled.Delete, "Delete",
                    tint = Color.White, modifier = Modifier.size(Spacing.xxl))
            }
        }

        // ── Conversation row (slides left on swipe) ───────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetPx.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state       = rememberDraggableState { delta ->
                        scope.launch {
                            offsetPx.snapTo((offsetPx.value + delta).coerceIn(-revealPx, 0f))
                        }
                    },
                    onDragStopped = {
                        scope.launch {
                            if (offsetPx.value < -threshPx) offsetPx.animateTo(-revealPx)
                            else offsetPx.animateTo(0f)
                        }
                    },
                )
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onClick() }
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar — stable Coil cache key avoids re-downloading the signed URL on each refresh.
            RinxAvatar(
                url                = item.avatarUrl,
                fallbackRes        = item.avatarRes,
                contentDescription = item.userName,
                size               = d.avatarSizeLg,
                name               = item.userName,
            )

            Spacer(Modifier.width(Spacing.md))

            // Text content — name/timestamp / invitation badge / preview + unread dot
            Column(modifier = Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(
                        text       = item.userName,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onBackground,
                        modifier   = Modifier.weight(1f),
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                    )
                    // Timestamp, with the "invitation pending" badge stacked beneath it (right-aligned).
                    Column(horizontalAlignment = Alignment.End) {
                        Text(item.timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.state == ConversationState.INVITATION_PENDING) {
                            Text("invitation pending",
                                style     = MaterialTheme.typography.labelSmall,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = FontStyle.Italic)
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text     = item.lastMessage,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (item.isUnread) {
                        Spacer(Modifier.width(Spacing.sm))
                        Box(Modifier.size(Spacing.sm).background(BrandPrimary, CircleShape))
                    }
                }
            }
        }
    }
}

// Simple non-swipeable wrapper kept for backwards compat
@Composable
fun MessageItem(item: ConversationItem, onClick: () -> Unit) {
    SwipeableMessageItem(item = item, onClick = onClick, onMarkRead = {}, onDelete = {})
}
