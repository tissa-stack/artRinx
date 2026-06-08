package com.rinx.artRINXapp.feature.notifications.presentation.notifications

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.notifications.domain.model.NotificationItem

@Composable
fun NotificationsContent(
    notifications: List<NotificationItem>,
    isLoading: Boolean,
    onDelete: (String) -> Unit,
    onMarkRead: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading && notifications.isEmpty() -> CircularProgressIndicator(
                color    = BrandPrimary,
                modifier = Modifier.align(Alignment.Center).size(Spacing.xxxl),
            )
            notifications.isEmpty() -> Text(
                text     = "No notifications yet",
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center).padding(Spacing.xl),
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(notifications, key = { it.id }) { item ->
                    SwipeableNotificationItem(
                        item       = item,
                        onDelete   = { onDelete(item.id) },
                        onMarkRead = { onMarkRead(item.id) },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}
