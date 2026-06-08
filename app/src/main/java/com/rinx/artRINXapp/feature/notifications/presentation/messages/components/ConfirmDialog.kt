package com.rinx.artRINXapp.feature.notifications.presentation.messages.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * Themed two-action confirmation dialog. Mirrors [BlockedDialog]'s surface/shape so all chat
 * dialogs read consistently. The confirm action defaults to destructive (uses `colorScheme.error`).
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    cancelLabel: String = "Cancel",
    destructive: Boolean = true,
) {
    val confirmBg = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val confirmFg = if (destructive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape    = RoundedCornerShape(Spacing.xl),
            color    = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            ) {
                Text(
                    text       = title,
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text  = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.xl))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    DialogButton(
                        label    = cancelLabel,
                        bg       = MaterialTheme.colorScheme.surfaceVariant,
                        fg       = MaterialTheme.colorScheme.onSurface,
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    DialogButton(
                        label    = confirmLabel,
                        bg       = confirmBg,
                        fg       = confirmFg,
                        onClick  = onConfirm,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    bg: androidx.compose.ui.graphics.Color,
    fg: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier         = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = Spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelLarge,
            color      = fg,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
