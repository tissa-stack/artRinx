package com.example.artrinx.feature.notifications.presentation.notifications

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.artrinx.feature.notifications.domain.model.NotificationItem

@Composable
fun NotificationsContent(
    notifications: List<NotificationItem>,
    onDelete: (String) -> Unit,
    onMarkRead: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
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
