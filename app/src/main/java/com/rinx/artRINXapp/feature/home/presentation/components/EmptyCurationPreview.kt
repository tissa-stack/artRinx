package com.rinx.artRINXapp.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.zIndex
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * Placeholder shown wherever a curation/collection preview has NO artworks — a clean fanned stack of
 * grey cards with an "add image" glyph on the front card (mirrors the empty-state on the New
 * Collection editor). Fills the bounds given by [modifier]; the caller supplies the height.
 *
 * Shades are derived from theme tokens (front = surfaceVariant, cards behind blend toward onSurface
 * so they read progressively darker) so it looks right in both light and dark themes.
 */
@Composable
fun EmptyCurationPreview(modifier: Modifier = Modifier) {
    val corner = LocalDimens.current.cardCornerRadius
    val base = MaterialTheme.colorScheme.surfaceVariant
    val deep = MaterialTheme.colorScheme.onSurface
    val cardCount = 3

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.TopStart) {
        val cardWidth = maxWidth * 0.68f
        val stackOffset = (maxWidth - cardWidth) / (cardCount - 1)

        // Render back-to-front: deeper cards sit further right and blend darker; the front card
        // (index 0, on top, no offset) carries the add-photo glyph.
        (cardCount - 1 downTo 0).forEach { index ->
            val isFront = index == 0
            Box(
                modifier = Modifier
                    .width(cardWidth)
                    .fillMaxHeight()
                    .offset(x = stackOffset * index.toFloat())
                    .zIndex((cardCount - index).toFloat())
                    .clip(RoundedCornerShape(corner))
                    .background(lerp(base, deep, index * 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                if (isFront) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_photo),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(Spacing.giant),
                    )
                }
            }
        }
    }
}
