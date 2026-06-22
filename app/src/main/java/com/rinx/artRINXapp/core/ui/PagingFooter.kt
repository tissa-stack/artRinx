package com.rinx.artRINXapp.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rinx.artRINXapp.core.paging.ListPage
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * Trailing footer for a paginated list. Shows a spinner while the next page is fetching, or an inline
 * error message + Retry when that fetch failed. Renders nothing when idle / no more pages. Drop it in
 * as the last `item { }` of a LazyColumn.
 */
@Composable
fun PagingFooter(state: ListPage, onRetry: () -> Unit) {
    when {
        state.isLoadingMore -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                color = BrandPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(Spacing.lg),
            )
        }

        state.loadMoreError != null -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.md, horizontal = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.loadMoreError,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.sm))
            OutlinedButton(onClick = onRetry) {
                Text(text = "Retry", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
