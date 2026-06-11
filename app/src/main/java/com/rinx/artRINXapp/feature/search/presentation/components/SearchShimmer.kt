package com.rinx.artRINXapp.feature.search.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.home.presentation.components.shimmer.rememberShimmerBrush

// ── Idle state shimmer (Trending Tags + Recommended grid) ─────────────────────

@Composable
fun SearchIdleShimmer(modifier: Modifier = Modifier) {
    val shimmer = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        // "Trending Tags" header shimmer
        Box(
            modifier = Modifier
                .width(Spacing.giant + Spacing.xxl + Spacing.lg)
                .height(Spacing.lg)
                .clip(RoundedCornerShape(Spacing.xs))
                .background(shimmer),
        )
        Spacer(Modifier.height(Spacing.md))

        // Tag chip shimmers (pill-shaped)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(Spacing.giant + Spacing.lg)
                        .height(Spacing.xl + Spacing.xs)
                        .clip(RoundedCornerShape(50))
                        .background(shimmer),
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .width(Spacing.huge + Spacing.md)
                        .height(Spacing.xl + Spacing.xs)
                        .clip(RoundedCornerShape(50))
                        .background(shimmer),
                )
            }
        }

        // Recommended-for-you was removed — the idle screen shows only trending tags.
    }
}

// ── Results staggered shimmer ─────────────────────────────────────────────────

private val shimmerHeights = listOf(
    false, true, true, false, false, true, false, true,
)

@Composable
fun SearchResultsShimmer(modifier: Modifier = Modifier) {
    val shimmer = rememberShimmerBrush()
    val d = LocalDimens.current

    LazyVerticalStaggeredGrid(
        columns               = StaggeredGridCells.Fixed(2),
        modifier              = modifier.fillMaxSize(),
        contentPadding        = PaddingValues(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalItemSpacing   = Spacing.sm,
    ) {
        items(count = shimmerHeights.size, key = { it }) { index ->
            val isTall = shimmerHeights[index]
            Column {
                // No rounded corners — matches final MasonryCard
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isTall) d.masonryCardHeightTall else d.masonryCardHeightShort)
                        .background(shimmer),
                )
                Spacer(Modifier.height(Spacing.xs))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(Spacing.md)
                        .clip(RoundedCornerShape(Spacing.xs))
                        .background(shimmer),
                )
                Spacer(Modifier.height(Spacing.xs))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(Spacing.sm + Spacing.xs)
                        .clip(RoundedCornerShape(Spacing.xs))
                        .background(shimmer),
                )
            }
        }
    }
}
