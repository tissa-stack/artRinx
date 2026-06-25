package com.rinx.artRINXapp.feature.home.presentation.components.shimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * Loading skeleton for the curation detail screen. Mirrors CurationDetailContent's LazyColumn order:
 * card-stack deck → title + curator + 3 action icons → Mediums label/value → description lines.
 * Theme-aware via [rememberShimmerBrush].
 */
@Composable
fun CurationDetailShimmer(modifier: Modifier = Modifier) {
    val d = LocalDimens.current
    val brush = rememberShimmerBrush()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        // Card-stack deck — a single CENTERED card matching the real front card (≈78% of screen
        // width, artDetailImageHeight * 1.08 tall), not a full-bleed image like the art hero.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(d.artDetailImageHeight * 1.08f)
                    .clip(RoundedCornerShape(d.cardCornerRadius))
                    .background(brush),
            )
        }

        // Title + curator (left, weight 1) + 3 action icons (add / share / like).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ShimmerLine(brush, 0.70f, Spacing.lg)
                Spacer(Modifier.height(Spacing.xs))
                ShimmerLine(brush, 0.40f, Spacing.md)
            }
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

        // Mediums: label line + value line.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        ) {
            ShimmerLine(brush, 0.30f, Spacing.sm)
            Spacer(Modifier.height(Spacing.xs))
            ShimmerLine(brush, 0.50f, Spacing.md)
        }

        // Description: header + two text lines.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        ) {
            ShimmerLine(brush, 0.30f, Spacing.sm)
            Spacer(Modifier.height(Spacing.sm))
            ShimmerLine(brush, 1f, Spacing.sm)
            Spacer(Modifier.height(Spacing.xs))
            ShimmerLine(brush, 0.85f, Spacing.sm)
        }
        Spacer(Modifier.height(Spacing.xxl))
    }
}
