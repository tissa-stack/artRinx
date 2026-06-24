package com.rinx.artRINXapp.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

/**
 * Renders an artwork pinned to the full width of its parent, with the height driven by the image's
 * TRUE aspect ratio — so the whole work shows with no letterbox/pillarbox bars and no crop.
 *
 * We reserve space up front using the server's [declaredRatio] (so the feed doesn't jump while
 * loading) and draw with [ContentScale.Crop] so the box is always filled — never barred — even if
 * that declared ratio is wrong or missing. The moment the image decodes we snap the box to the real
 * ratio from the bitmap's intrinsic size, after which Crop shows the entire work (box == image).
 */
@Composable
fun FullWidthArtImage(
    model: Any?,
    contentDescription: String?,
    declaredRatio: Float?,
    modifier: Modifier = Modifier,
) {
    // Seed from the declared ratio, then let the decoded image override it. Keyed on [model] so a
    // recycled LazyColumn slot doesn't keep the previous artwork's ratio.
    var ratio by remember(model) { mutableStateOf(declaredRatio?.takeIf { it > 0f }) }
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        onSuccess = { state ->
            val w = state.result.drawable.intrinsicWidth
            val h = state.result.drawable.intrinsicHeight
            if (w > 0 && h > 0) ratio = w.toFloat() / h
        },
        modifier = modifier
            .fillMaxWidth()
            .then(ratio?.let { Modifier.aspectRatio(it) } ?: Modifier)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}
