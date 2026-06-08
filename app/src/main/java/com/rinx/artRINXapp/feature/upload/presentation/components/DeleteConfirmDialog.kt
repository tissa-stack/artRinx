package com.rinx.artRINXapp.feature.upload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * Confirm dialog shown before deleting an artwork or curation.
 * [itemLabel] is the noun used in the prompt, e.g. "art" or "curation".
 */
@Composable
fun DeleteConfirmDialog(
    itemLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDeleting: Boolean = false,
) {
    Dialog(onDismissRequest = { if (!isDeleting) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(Spacing.xl),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isDeleting,
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Icon(
                            Icons.Default.Close, "Close",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }

                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(Spacing.giant),
                )
                Spacer(Modifier.height(Spacing.lg))

                Text(
                    text = "Are you sure you want\nto delete this $itemLabel?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.xl))

                PillButton(
                    text = if (isDeleting) "Deleting..." else "Delete",
                    containerColor = BrandPrimary,
                    textColor = MaterialTheme.colorScheme.onPrimary,
                    enabled = !isDeleting,
                    onClick = onConfirm,
                )
                Spacer(Modifier.height(Spacing.md))
                PillButton(
                    text = "Cancel",
                    containerColor = InactiveButton,
                    textColor = MaterialTheme.colorScheme.onSecondary,
                    enabled = !isDeleting,
                    onClick = onDismiss,
                )
                Spacer(Modifier.height(Spacing.sm))
            }
        }
    }
}

@Composable
private fun PillButton(
    text: String,
    containerColor: Color,
    textColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = Spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
        )
    }
}
