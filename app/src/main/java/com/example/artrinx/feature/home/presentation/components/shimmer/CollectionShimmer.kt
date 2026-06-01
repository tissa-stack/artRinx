package com.example.artrinx.feature.home.presentation.components.shimmer

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.artrinx.core.theme.DarkCardSurface
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing

/**
 * Mirrors CollectionCard exactly:
 * - Outer Column: RoundedCornerShape + DarkCardSurface background
 * - Top: image fan section (shimmer fill, collectionCardHeight)
 * - Bottom: text row (title placeholder + avatar circle)
 */
@Composable
fun CollectionShimmer(modifier: Modifier = Modifier) {
    val d = LocalDimens.current
    val brush = rememberShimmerBrush()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        repeat(2) {
            Column(
                modifier = Modifier
                    .width(d.collectionCardWidth)
                    .clip(RoundedCornerShape(d.cardCornerRadius))
                    .background(DarkCardSurface),
            ) {
                // Image fan section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(d.collectionCardHeight)
                        .background(brush),
                )
                // Text + avatar row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(Spacing.md)
                                .clip(RoundedCornerShape(Spacing.xs))
                                .background(brush),
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(Spacing.sm)
                                .clip(RoundedCornerShape(Spacing.xs))
                                .background(brush),
                        )
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    Box(
                        modifier = Modifier
                            .size(d.avatarSize)
                            .clip(CircleShape)
                            .background(brush),
                    )
                }
            }
        }
    }
}
