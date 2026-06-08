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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * Mirrors DiscoverFeedItem exactly:
 * Header  : avatar circle + name line + role line
 * Image   : full width, feedImageHeight (no corner radius)
 * Footer  : [title + artist LEFT] | [3 icon boxes + count RIGHT]
 */
@Composable
fun FeedShimmer(modifier: Modifier = Modifier) {
    val d = LocalDimens.current
    val brush = rememberShimmerBrush()
    Column(modifier = modifier.fillMaxWidth()) {

        // ── Header: avatar + name + role ─────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(d.avatarSize)
                    .clip(CircleShape)
                    .background(brush),
            )
            Spacer(Modifier.width(Spacing.sm))
            Column {
                Box(
                    modifier = Modifier
                        .width(d.artCardWidth * 0.45f)
                        .height(Spacing.md)
                        .clip(RoundedCornerShape(Spacing.xs))
                        .background(brush),
                )
                Spacer(Modifier.height(Spacing.xs))
                Box(
                    modifier = Modifier
                        .width(d.artCardWidth * 0.25f)
                        .height(Spacing.sm)
                        .clip(RoundedCornerShape(Spacing.xs))
                        .background(brush),
                )
            }
        }

        // ── Artwork image — full width, no corner radius ──────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d.feedImageHeight)
                .background(brush),
        )

        // ── Footer: title+artist LEFT | icons+count RIGHT ─────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Title + artist
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.70f)
                        .height(Spacing.md)
                        .clip(RoundedCornerShape(Spacing.xs))
                        .background(brush),
                )
                Spacer(Modifier.height(Spacing.xs))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .height(Spacing.sm)
                        .clip(RoundedCornerShape(Spacing.xs))
                        .background(brush),
                )
            }

            Spacer(Modifier.width(Spacing.sm))

            // Icons column (3 icons row + count below)
            Column(horizontalAlignment = Alignment.End) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(Spacing.xl)
                                .clip(RoundedCornerShape(Spacing.xs))
                                .background(brush),
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
                Box(
                    modifier = Modifier
                        .width(Spacing.xl)
                        .height(Spacing.sm)
                        .clip(RoundedCornerShape(Spacing.xs))
                        .background(brush),
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = Spacing.md),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )
    }
}
