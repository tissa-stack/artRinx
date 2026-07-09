package com.rinx.artRINXapp.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.upload.domain.model.UploadProgress

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
    contained: Boolean = false,
) {
    val label = when (progress) {
        is UploadProgress.Compressing -> "Preparing…"
        is UploadProgress.Uploading -> "Uploading artwork"
        is UploadProgress.Finalizing -> "Finishing up…"
        is UploadProgress.Success -> "Uploaded"
        is UploadProgress.Failed -> "Upload failed"
    }
    val percent = (progress as? UploadProgress.Uploading)?.percent
    val fraction = percent?.let { it / 100f }
    val failed = progress as? UploadProgress.Failed
    ProgressRowContent(
        thumbnail = progress.localThumb,
        label = label,
        fraction = fraction,
        percent = percent,
        errorMessage = failed?.message,
        retryable = failed?.retryable ?: false,
        isFailed = failed != null,
        onRetry = onRetry,
        onDismiss = onDismiss,
        modifier = modifier,
        contained = contained,
    )
}

/**
 * Shared progress-row UI used by both the artwork ([UploadProgressRow]) and curation
 * ([CurationProgressRow]) flows. [thumbnail] is any Coil model (Uri, URL string, or drawable res).
 * [fraction] null → indeterminate bar. [percent] non-null → shows a "NN%" readout on the label line
 * (artwork upload only; curation is indeterminate). Tokens only — no hardcoded dp/colors.
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
    percent: Int? = null,
    contained: Boolean = false,
) {
    val d = LocalDimens.current
    // Faint line that stays visible on BOTH the bare background and the surfaceVariant card (a plain
    // surfaceVariant track would vanish against the card fill), in light and dark.
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    // [contained] → a rounded surfaceVariant card matching the sibling cards on the Create screen;
    // otherwise the bare inset used on Profile / (dormant) Home.
    val containerModifier = if (contained) {
        Modifier
            .clip(RoundedCornerShape(d.cardCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Spacing.lg)
    } else {
        Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(containerModifier),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                if (percent != null) {
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.size(Spacing.xs))
            when {
                isFailed -> Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                // Custom determinate bar: a thin, fully-rounded cyan fill on a faint track. Matches
                // the reference exactly (M3's LinearProgressIndicator adds a gap + stop-dot in 1.3.0).
                fraction != null -> Box(
                    Modifier
                        .fillMaxWidth()
                        .height(Spacing.xs)
                        .clip(RoundedCornerShape(50))
                        .background(trackColor),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                else -> LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = trackColor,
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
