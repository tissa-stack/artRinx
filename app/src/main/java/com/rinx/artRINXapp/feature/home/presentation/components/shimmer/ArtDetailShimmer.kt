package com.rinx.artRINXapp.feature.home.presentation.components.shimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * Loading skeleton for the artwork detail screen. Mirrors ArtDetailContent's LazyColumn order:
 * hero image → title + 3 action icons → Artist/Medium meta columns → description → artist row +
 * send button → "More like this" rail. Theme-aware via [rememberShimmerBrush].
 */
@Composable
fun ArtDetailShimmer(modifier: Modifier = Modifier) {
    val d = LocalDimens.current
    val brush = rememberShimmerBrush()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        // Matches ArtDetailContent's contentPadding(top = statusBarTop): the hero starts below the bar.
        Spacer(Modifier.height(statusBarTop))

        // Hero image — full-bleed, square fallback (matches the default heroRatio of 1f).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(brush),
        )

        // Title (left, weight 1) + 3 action icons (add / share / like).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShimmerLine(brush, 0.55f, Spacing.lg, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(Spacing.xxl)
                            .clip(RoundedCornerShape(Spacing.xs))
                            .background(brush),
                    )
                }
            }
        }

        // Meta: Artist column + Medium column (label line + value line each).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        ) {
            repeat(2) { i ->
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerLine(brush, 0.35f, Spacing.sm)
                    Spacer(Modifier.height(Spacing.xs))
                    ShimmerLine(brush, 0.65f, Spacing.md)
                }
                if (i == 0) Spacer(Modifier.width(Spacing.sm))
            }
        }

        // Description: header row + two text lines.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        ) {
            ShimmerLine(brush, 0.30f, Spacing.sm)
            Spacer(Modifier.height(Spacing.sm))
            ShimmerLine(brush, 1f, Spacing.sm)
            Spacer(Modifier.height(Spacing.xs))
            ShimmerLine(brush, 0.80f, Spacing.sm)
        }

        // Artist row: avatar + name/role (weight 1) + Send-message button.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(d.avatarSizeLg)
                    .clip(CircleShape)
                    .background(brush),
            )
            Spacer(Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                ShimmerLine(brush, 0.45f, Spacing.md)
                Spacer(Modifier.height(Spacing.xs))
                ShimmerLine(brush, 0.30f, Spacing.sm)
            }
            Spacer(Modifier.width(Spacing.sm))
            Box(
                modifier = Modifier
                    .width(d.artCardWidth * 0.42f)
                    .height(Spacing.xxxl)
                    .clip(RoundedCornerShape(Spacing.sm))
                    .background(brush),
            )
        }

        // "More like this" header + a rail of two cards.
        Spacer(Modifier.height(Spacing.sm))
        ShimmerLine(
            brush, 0.40f, Spacing.md,
            modifier = Modifier.padding(horizontal = Spacing.md),
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .width(d.artCardWidth)
                        .height(d.artCardHeight)
                        .clip(RoundedCornerShape(d.cardCornerRadius))
                        .background(brush),
                )
            }
        }
        Spacer(Modifier.height(Spacing.xxl))
    }
}

/** A single rounded shimmer line of [widthFraction] of its parent width and [height] tall. */
@Composable
internal fun ShimmerLine(
    brush: Brush,
    widthFraction: Float,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(Spacing.xs))
            .background(brush),
    )
}
