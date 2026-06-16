package com.rinx.artRINXapp.feature.profile.presentation.view.components.shimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.home.presentation.components.shimmer.rememberShimmerBrush

@Composable
fun ProfileShimmer(modifier: Modifier = Modifier) {
    val shimmer = rememberShimmerBrush()
    val d = LocalDimens.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = d.screenPaddingHorizontal),
    ) {
        // ── Username + settings row shimmer ───────────────────────────────
        // Fixed height matches the real header's IconButton-driven row (Spacing.huge) so the
        // avatar/stats below line up exactly between skeleton and loaded content.
        Row(
            modifier = Modifier.fillMaxWidth().height(Spacing.huge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .height(Spacing.xl)
                    .clip(RoundedCornerShape(Spacing.xs))
                    .background(shimmer),
            )
            // Two header icons (invite + settings) to match the real header.
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .size(Spacing.xl)
                            .clip(CircleShape)
                            .background(shimmer),
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))

        // ── Avatar + stats shimmer ────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(d.profileAvatarSize)
                    .clip(CircleShape)
                    .background(shimmer),
            )
            Spacer(Modifier.width(Spacing.lg))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                repeat(4) {
                    // Bound each column to an equal slot (weight) so the fillMaxWidth fractions
                    // resolve against the per-column width. Without this the boxes size against the
                    // whole row, overflow, and the next column's clipped edge leaks in at the right.
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .height(Spacing.lg + Spacing.xs)
                                .clip(RoundedCornerShape(Spacing.xs))
                                .background(shimmer),
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(Spacing.sm + Spacing.xs)
                                .clip(RoundedCornerShape(Spacing.xs))
                                .background(shimmer),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))

        // ── Display name shimmer ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth(0.48f)
                .height(Spacing.lg + Spacing.xs)
                .clip(RoundedCornerShape(Spacing.xs))
                .background(shimmer),
        )
        Spacer(Modifier.height(Spacing.xs))

        // ── Role shimmer ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth(0.28f)
                .height(Spacing.md)
                .clip(RoundedCornerShape(Spacing.xs))
                .background(shimmer),
        )

        Spacer(Modifier.height(Spacing.lg))

        // ── Tab bar shimmer ───────────────────────────────────────────────
        // Mirror ProfileTabBar's own vertical padding (Spacing.sm top & bottom) so the pill sits
        // at the same Y as the real sticky tab bar.
        Spacer(Modifier.height(Spacing.sm))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d.tabPillHeight)
                .clip(RoundedCornerShape(50))
                .background(shimmer),
        )
        Spacer(Modifier.height(Spacing.sm))

        // Matches the LazyColumn content item's top padding (Spacing.md).
        Spacer(Modifier.height(Spacing.md))

        // ── Masonry content shimmer ───────────────────────────────────────
        val leftHeights = listOf(d.masonryCardHeightTall, d.masonryCardHeightShort, d.masonryCardHeightMedium)
        val rightHeights = listOf(d.masonryCardHeightShort, d.masonryCardHeightMedium, d.masonryCardHeightTall)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                leftHeights.forEach { h ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(h)
                            .clip(RoundedCornerShape(d.cardCornerRadius))
                            .background(shimmer),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                rightHeights.forEach { h ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(h)
                            .clip(RoundedCornerShape(d.cardCornerRadius))
                            .background(shimmer),
                    )
                }
            }
        }
    }
}