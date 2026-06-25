package com.rinx.artRINXapp.feature.notifications.presentation.messages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.notifications.domain.model.ConversationItem
import com.rinx.artRINXapp.feature.notifications.domain.model.ConversationState
import com.rinx.artRINXapp.feature.notifications.presentation.components.RowActionsMenu
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageRow(
    item: ConversationItem,
    menuOpen: Boolean,
    onLongPress: () -> Unit,
    onDismissMenu: () -> Unit,
    onClick: () -> Unit,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit,
) {
    val d = LocalDimens.current

    Box(modifier = Modifier.fillMaxWidth()) {
        RowActionsMenu(
            expanded = menuOpen,
            deleteLabel = "Delete chat",
            showMarkRead = item.isUnread,
            onMarkRead = onMarkRead,
            onDelete = onDelete,
            onDismiss = onDismissMenu,
        )

        // ── Conversation row (long-press → menu; highlighted while open) ───
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .background(
                    if (menuOpen) BrandPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                )
                .combinedClickable(onClick = onClick, onLongClick = onLongPress)
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

// Simple row with no long-press actions (e.g. new-message search results).
@Composable
fun MessageItem(item: ConversationItem, onClick: () -> Unit) {
    MessageRow(
        item = item,
        menuOpen = false,
        onLongPress = {},
        onDismissMenu = {},
        onClick = onClick,
        onMarkRead = {},
        onDelete = {},
    )
}
