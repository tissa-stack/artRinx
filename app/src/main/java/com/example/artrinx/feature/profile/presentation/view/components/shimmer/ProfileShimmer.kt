package com.example.artrinx.feature.profile.presentation.view.components.shimmer

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
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.home.presentation.components.shimmer.rememberShimmerBrush

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
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            Box(
                modifier = Modifier
                    .size(Spacing.xl)
                    .clip(RoundedCornerShape(Spacing.sm))
                    .background(shimmer),
            )
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d.tabPillHeight)
                .clip(RoundedCornerShape(50))
                .background(shimmer),
        )

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