package com.rinx.artRINXapp.feature.notifications.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rinx.artRINXapp.core.theme.DangerRed
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * The long-press action menu shared by the Notifications and Messages list rows: "Mark as read" +
 * a destructive delete (label varies — "Delete notification" / "Delete chat"). Anchored to the row
 * it's rendered inside, mirroring the chat message menu.
 */
@Composable
fun RowActionsMenu(
    expanded: Boolean,
    deleteLabel: String,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    showMarkRead: Boolean = true,
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        // Already-read rows only get Delete (no point offering "Mark as read").
        if (showMarkRead) {
            DropdownMenuItem(
                text = { Text("Mark as read", color = onBg) },
                leadingIcon = {
                    Icon(Icons.Outlined.MailOutline, null, Modifier.size(Spacing.lg), tint = onBg)
                },
                onClick = onMarkRead,
            )
        }
        DropdownMenuItem(
            text = { Text(deleteLabel, color = DangerRed) },
            leadingIcon = {
                Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(Spacing.lg), tint = DangerRed)
            },
            onClick = onDelete,
        )
    }
}
