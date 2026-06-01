package com.example.artrinx.feature.home.presentation.components.shimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing

@Composable
fun BannerShimmer(modifier: Modifier = Modifier) {
    val d = LocalDimens.current
    val brush = rememberShimmerBrush()
    Column(modifier = modifier.fillMaxWidth()) {
        // "Sponsored" label placeholder — matches actual label padding
        Box(
            modifier = Modifier
                .padding(horizontal = Spacing.md, vertical = Spacing.xs)
                .width(Spacing.huge)
                .height(Spacing.sm)
                .clip(RoundedCornerShape(Spacing.xs))
                .background(brush),
        )
        // Banner image — full width with minimal side padding, NO corner radius (matches actual)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .height(d.bannerHeight)
                .background(brush),          // RectangleShape — no clip, matches FeaturedCarouselItem
        )
        // Expanding dot indicator row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.sm),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(4) { i ->
                val size = if (i == 0) d.indicatorDotActive else d.indicatorDotSmall
                Box(
                    modifier = Modifier
                        .padding(horizontal = Spacing.xs)
                        .size(size)
                        .clip(CircleShape)
                        .background(brush),
                )
            }
        }
    }
}
