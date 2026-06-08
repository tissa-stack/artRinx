package com.rinx.artRINXapp.feature.home.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rinx.artRINXapp.feature.upload.domain.model.CurationProgress

/**
 * Top-of-feed indicator for an in-progress (or just-finished/failed) curation create.
 * Curation create is a single POST, so it only has an indeterminate "Creating…" state.
 * Delegates to the shared [ProgressRowContent]; the thumbnail is a remote URL string.
 */
@Composable
fun CurationProgressRow(
    progress: CurationProgress,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when (progress) {
        is CurationProgress.Creating -> "Creating curation…"
        is CurationProgress.Success -> "Curation created"
        is CurationProgress.Failed -> "Couldn't create curation"
    }
    val failed = progress as? CurationProgress.Failed
    ProgressRowContent(
        thumbnail = progress.thumbnail,
        label = label,
        fraction = null, // single POST → always indeterminate
        errorMessage = failed?.message,
        retryable = failed?.retryable ?: false,
        isFailed = failed != null,
        onRetry = onRetry,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}
