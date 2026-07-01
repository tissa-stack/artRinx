package com.rinx.artRINXapp.feature.home.presentation.components.shimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * Loading skeleton for a chat conversation — alternating received (left) / sent (right) bubble
 * placeholders of varying widths. Theme-aware via [rememberShimmerBrush].
 *
 * Repeats the bubble pattern enough times to fill the FULL available height (computed from the
 * measured constraints), so the skeleton covers the whole message area on any screen size instead
 * of clustering a fixed handful of bubbles at the top.
 */
@Composable
fun ChatShimmer(modifier: Modifier = Modifier) {
    val d = LocalDimens.current
    val brush = rememberShimmerBrush()
    val bubbleHeight = d.authButtonHeight * 0.8f
    val spacing = Spacing.md
    // (widthFraction, isSent) — sent bubbles hug the right edge, received the left.
    val pattern = listOf(
        0.55f to false,
        0.40f to true,
        0.70f to false,
        0.50f to true,
        0.62f to false,
        0.45f to true,
        0.58f to false,
    )
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // How many bubble rows fit in the available height (rows + gaps); never fewer than the
        // base pattern. Slight overfill is fine — extra rows just clip at the bottom edge.
        val fitted = ((maxHeight + spacing) / (bubbleHeight + spacing)).toInt()
        val count = maxOf(pattern.size, fitted)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            repeat(count) { index ->
                val (widthFraction, isSent) = pattern[index % pattern.size]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(widthFraction)
                            .height(bubbleHeight)
                            .clip(RoundedCornerShape(Spacing.lg))
                            .background(brush),
                        contentAlignment = Alignment.Center,
                    ) {}
                }
            }
        }
    }
}
