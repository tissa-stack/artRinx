package com.rinx.artRINXapp.feature.notifications.presentation.notifications

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.notifications.domain.model.NotificationItem
import com.rinx.artRINXapp.feature.notifications.domain.model.NotificationKind
import com.rinx.artRINXapp.feature.search.presentation.components.SearchMessageView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsContent(
    notifications: List<NotificationItem>,
    isLoading: Boolean,
    onDelete: (String) -> Unit,
    onMarkRead: (String) -> Unit,
    onOpenEvent: (Long) -> Unit,
    onOpenProfile: (Long) -> Unit,
    onOpenArt: (Long) -> Unit,
    onOpenCuration: (Long) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    onRetry: () -> Unit = {},
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading && notifications.isEmpty() -> CircularProgressIndicator(
                color    = BrandPrimary,
                modifier = Modifier.align(Alignment.Center).size(Spacing.xxxl),
            )
            // Network/server failure with nothing cached to fall back on → show the reason + Retry.
            notifications.isEmpty() && error != null -> SearchMessageView(
                title       = "Couldn't load notifications",
                subtitle    = error,
                icon        = Icons.Outlined.CloudOff,
                actionLabel = "Retry",
                onAction    = onRetry,
                modifier    = Modifier.align(Alignment.Center),
            )
            notifications.isEmpty() -> Text(
                text     = "No notifications yet",
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center).padding(Spacing.xl),
            )
            else -> PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
              LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(notifications, key = { it.id }) { item ->
                    // Auto-mark-read on appear (handout §7): the first time an unread row is
                    // composed (i.e. scrolled into view), clear it server-side + locally.
                    if (!item.isRead) {
                        LaunchedEffect(item.id) { onMarkRead(item.id) }
                    }
                    if (item.kind.isEvent) {
                        EventNotificationRow(
                            item          = item,
                            onOpenEvent   = onOpenEvent,
                            onOpenProfile = onOpenProfile,
                        )
                    } else {
                        SwipeableNotificationItem(
                            item       = item,
                            onDelete   = { onDelete(item.id) },
                            onMarkRead = { onMarkRead(item.id) },
                            onClick    = { routeNotification(item, onOpenArt, onOpenCuration, onOpenProfile) },
                        )
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    )
                }
              }
            }
        }
    }
}

/**
 * Whole-row tap routing by type (handout §7): like/share of an artwork → art detail; of a
 * curation → curation detail; follow / profile-share → the actor's profile. Unknown types and
 * rows missing their target id are non-tappable (no-op).
 */
internal fun routeNotification(
    item: NotificationItem,
    onOpenArt: (Long) -> Unit,
    onOpenCuration: (Long) -> Unit,
    onOpenProfile: (Long) -> Unit,
) {
    when (item.kind) {
        NotificationKind.ARTWORK_LIKE, NotificationKind.ARTWORK_SHARE ->
            item.targetId?.let(onOpenArt)
        NotificationKind.CURATION_LIKE, NotificationKind.CURATION_SHARE ->
            item.targetId?.let(onOpenCuration)
        NotificationKind.FOLLOW, NotificationKind.PROFILE_SHARE ->
            item.actorId?.let(onOpenProfile)
        else -> Unit
    }
}
