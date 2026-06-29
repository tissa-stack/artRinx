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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.rinx.artRINXapp.core.theme.DarkCardSurface
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

// ── Users list shimmer (matches UserResultRow: avatar + name/subtitle) ────────

@Composable
fun SearchUsersShimmer(modifier: Modifier = Modifier) {
    val shimmer = rememberShimmerBrush()
    val d = LocalDimens.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        repeat(8) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(d.avatarSize)
                        .clip(CircleShape)
                        .background(shimmer),
                )
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    // Display name
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(Spacing.md)
                            .clip(RoundedCornerShape(Spacing.xs))
                            .background(shimmer),
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    // @handle · followers
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(Spacing.sm + Spacing.xs)
                            .clip(RoundedCornerShape(Spacing.xs))
                            .background(shimmer),
                    )
                }
            }
        }
    }
}

// ── Collections grid shimmer (matches CurationGridCard, 2-col fixed grid) ─────

@Composable
fun SearchCurationsShimmer(modifier: Modifier = Modifier) {
    val shimmer = rememberShimmerBrush()
    val d = LocalDimens.current
    val shape = RoundedCornerShape(d.cardCornerRadius)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                repeat(2) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(4.dp, shape)
                            .clip(shape)
                            .background(DarkCardSurface),
                    ) {
                        // Fanned image area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(d.collectionCardHeight)
                                .background(shimmer),
                        )
                        // Title row
                        Box(
                            modifier = Modifier
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                                .fillMaxWidth(0.7f)
                                .height(Spacing.md)
                                .clip(RoundedCornerShape(Spacing.xs))
                                .background(shimmer),
                        )
                    }
                }
            }
        }
    }
}
