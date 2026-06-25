package com.rinx.artRINXapp.feature.profile.presentation.other.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.rinx.artRINXapp.core.theme.DangerRed
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * Confirmation shown before blocking a user (parity with iOS). A red no-entry glyph, "Block <name>?",
 * and the three things a blocked user can no longer do, then Cancel / Block. For blocking artwork or
 * for unfollow/unblock, keep using the generic [ConfirmActionDialog].
 */
@Composable
fun BlockConfirmDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false,
) {
    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(Spacing.xl),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Block,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(Spacing.giant),
                )
                Spacer(Modifier.height(Spacing.md))

                Text(
                    text = "Block ${name.ifBlank { "this user" }}?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.md))

                Text(
                    text = "When you block someone, they won't be able to:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.md))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    BlockEffectRow(Icons.Outlined.Visibility, "See your profile and Arts")
                    BlockEffectRow(Icons.Outlined.ChatBubbleOutline, "Send you messages")
                    BlockEffectRow(Icons.Outlined.PersonOutline, "Follow you")
                }

                Spacer(Modifier.height(Spacing.xl))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f).height(Spacing.giant),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(Spacing.xs / 4, MaterialTheme.colorScheme.outline),
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f).height(Spacing.giant),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DangerRed,
                            contentColor = Color.White,
                        ),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(Spacing.xl),
                                color = Color.White,
                                strokeWidth = Spacing.xs / 2,
                            )
                        } else {
                            Text("Block", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockEffectRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Spacing.lg),
        )
        Spacer(Modifier.size(Spacing.sm))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
