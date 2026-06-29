package com.rinx.artRINXapp.feature.home.presentation.components.shimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * Mirrors ArtworkCard / RecentlyViewedCard:
 * - Each card = artCardWidth × artCardHeight, rounded corners (cardCornerRadius)
 * - Text is inside the card as an overlay — no text boxes below
 * - Shows 1 full card + partial second (matches 62% width cards in LazyRow)
 */
@Composable
fun HorizontalListShimmer(modifier: Modifier = Modifier) {
    val d = LocalDimens.current
    val brush = rememberShimmerBrush()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .width(d.artCardWidth)
                    .height(d.artCardHeight)
                    .clip(RoundedCornerShape(d.cardCornerRadius))   // matches ArtworkCard's rounded corners
                    .background(brush),
            )
        }
    }
}
