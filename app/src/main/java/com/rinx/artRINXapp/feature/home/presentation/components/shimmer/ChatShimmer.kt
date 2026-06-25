package com.rinx.artRINXapp.feature.home.presentation.components.shimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * Loading skeleton for a chat conversation — alternating received (left) / sent (right) bubble
 * placeholders of varying widths. Theme-aware via [rememberShimmerBrush].
 */
@Composable
fun ChatShimmer(modifier: Modifier = Modifier) {
    val d = LocalDimens.current
    val brush = rememberShimmerBrush()
    // (widthFraction, isSent) — sent bubbles hug the right edge, received the left.
    val bubbles = listOf(
        0.55f to false,
        0.40f to true,
        0.70f to false,
        0.50f to true,
        0.62f to false,
        0.45f to true,
        0.58f to false,
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        bubbles.forEach { (widthFraction, isSent) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(widthFraction)
                        .height(d.authButtonHeight * 0.8f)
                        .clip(RoundedCornerShape(Spacing.lg))
                        .background(brush),
                    contentAlignment = Alignment.Center,
                ) {}
            }
        }
    }
}
