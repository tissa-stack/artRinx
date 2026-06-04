package com.example.artrinx.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.upload.domain.model.UploadProgress

/**
 * Top-of-feed indicator for an in-progress (or just-finished/failed) artwork upload.
 * Delegates rendering to the shared [ProgressRowContent].
 */
@Composable
fun UploadProgressRow(
    progress: UploadProgress,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when (progress) {
        is UploadProgress.Compressing -> "Preparing…"
        is UploadProgress.Uploading -> "Uploading"
        is UploadProgress.Finalizing -> "Finishing up…"
        is UploadProgress.Success -> "Uploaded"
        is UploadProgress.Failed -> "Upload failed"
    }
    val fraction = (progress as? UploadProgress.Uploading)?.let { it.percent / 100f }
    val failed = progress as? UploadProgress.Failed
    ProgressRowContent(
        thumbnail = progress.localThumb,
        label = label,
        fraction = fraction,
        errorMessage = failed?.message,
        retryable = failed?.retryable ?: false,
        isFailed = failed != null,
        onRetry = onRetry,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

/**
 * Shared progress-row UI used by both the artwork ([UploadProgressRow]) and curation
 * ([CurationProgressRow]) flows. [thumbnail] is any Coil model (Uri, URL string, or drawable res).
 * [fraction] null → indeterminate bar. Tokens only — no hardcoded dp/colors.
 */
@Composable
internal fun ProgressRowContent(
    thumbnail: Any?,
    label: String,
    fraction: Float?,
    errorMessage: String?,
    retryable: Boolean,
    isFailed: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = LocalDimens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(d.avatarSizeLg)
                .clip(RoundedCornerShape(d.cardCornerRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.size(Spacing.xs))
            when {
                isFailed -> Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                fraction != null -> LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                else -> LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
        if (isFailed) {
            Spacer(Modifier.width(Spacing.md))
            val actionLabel = if (retryable) "Retry" else "Dismiss"
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (retryable) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(Spacing.sm))
                    .clickable { if (retryable) onRetry() else onDismiss() }
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            )
        }
    }
}
