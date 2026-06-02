package com.example.artrinx.feature.upload.presentation.newart.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.artrinx.core.theme.ErrorDark
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing

/**
 * Reusable dark rounded card for all upload form fields.
 * Label + optional char counter appear at the top; content slot below.
 */
@Composable
fun ArtFormField(
    label: String,
    modifier: Modifier = Modifier,
    hasError: Boolean = false,
    charCount: String? = null,       // e.g. "5/40 characters"
    trailingHeader: (@Composable () -> Unit)? = null,  // e.g. Premium badge
    content: @Composable () -> Unit,
) {
    val d = LocalDimens.current
    val shape = RoundedCornerShape(d.cardCornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (hasError) Modifier.border(1.5.dp, ErrorDark, shape)
                else Modifier
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row: label + char counter or trailing widget
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text     = label,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (charCount != null) {
                    Text(
                        text  = charCount,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                trailingHeader?.invoke()
            }
            Spacer(Modifier.height(Spacing.xs))
            content()
        }
    }
}
