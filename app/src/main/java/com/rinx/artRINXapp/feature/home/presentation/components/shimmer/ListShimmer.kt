package com.rinx.artRINXapp.feature.home.presentation.components.shimmer

import androidx.compose.foundation.background
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
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * A vertical list of avatar-row skeletons — the loading placeholder for any "avatar + two text
 * lines + trailing meta" list (notifications, messages/inbox, followers/following). Theme-aware via
 * [rememberShimmerBrush]. Drop in where a full-screen list loader was shown.
 */
@Composable
fun ListShimmer(modifier: Modifier = Modifier, rowCount: Int = 8) {
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(rowCount) { ListRowShimmer() }
    }
}

/** A single list row skeleton: avatar circle + name/subtitle lines + a trailing meta block. */
@Composable
fun ListRowShimmer(modifier: Modifier = Modifier) {
    val d = LocalDimens.current
    val brush = rememberShimmerBrush()
    Row(
        modifier = modifier
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
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(Spacing.md)
                    .clip(RoundedCornerShape(Spacing.xs))
                    .background(brush),
            )
            Spacer(Modifier.height(Spacing.xs))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.70f)
                    .height(Spacing.sm)
                    .clip(RoundedCornerShape(Spacing.xs))
                    .background(brush),
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Box(
            modifier = Modifier
                .width(Spacing.xl)
                .height(Spacing.sm)
                .clip(RoundedCornerShape(Spacing.xs))
                .background(brush),
        )
    }
}
