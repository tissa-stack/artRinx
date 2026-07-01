package com.rinx.artRINXapp.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * Placeholder shown wherever a curation/collection preview has NO artworks — a clean fanned stack of
 * cards with an "add image" glyph on the front card (mirrors the empty-state on the New Collection
 * editor). Fills the bounds given by [modifier]; the caller supplies the height.
 *
 * Theme-aware: front card uses [surfaceVariant], cards behind blend toward [onSurface] so they read
 * progressively darker; every card gets a 1dp outline border so edges stay crisp in both themes, and
 * only the front card casts a shadow (no muddy overlapping halo).
 */
@Composable
fun EmptyCurationPreview(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(LocalDimens.current.cardCornerRadius)
    val base = MaterialTheme.colorScheme.surfaceVariant
    val deep = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline
    val cardCount = 3
    // [deep] = onSurface is dark in light theme but near-white in dark theme, which inverts the
    // intended depth (back cards glow lighter). In dark theme, reverse the gradient order so the
    // darkest card lands at the back of the fan ("last"), front card lightest.
    val darkTheme = isSystemInDarkTheme()

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.TopStart) {
        val cardWidth = maxWidth * 0.68f
        val stackOffset = (maxWidth - cardWidth) / (cardCount - 1)

        // Render back-to-front: deeper cards sit further right and blend darker; the front card
        // (index 0, on top, no offset) carries the add-photo glyph and the only shadow.
        (cardCount - 1 downTo 0).forEach { index ->
            val isFront = index == 0
            // Light: front (index 0) lightest → back darkest. Dark: reversed so the darkest is the
            // back/last card of the fan.
            val shadeStep = if (darkTheme) (cardCount - 1 - index) else index
            Box(
                modifier = Modifier
                    .width(cardWidth)
                    .fillMaxHeight()
                    .offset(x = stackOffset * index.toFloat())
                    .zIndex((cardCount - index).toFloat())
                    .then(if (isFront) Modifier.shadow(6.dp, shape) else Modifier)
                    .clip(shape)
                    .background(lerp(base, deep, shadeStep * 0.16f))
                    .border(1.dp, outline, shape),
                contentAlignment = Alignment.Center,
            ) {
                if (isFront) {
                    // Plain art glyph (no plus) — this is a preview/placeholder, not an action.
                    Icon(
                        painter = painterResource(R.drawable.ic_photo),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(Spacing.giant),
                    )
                }
            }
        }
    }
}
