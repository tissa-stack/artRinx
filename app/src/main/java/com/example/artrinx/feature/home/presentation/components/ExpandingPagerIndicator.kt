package com.example.artrinx.feature.home.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import kotlin.math.abs

@Composable
fun ExpandingPagerIndicator(
    pageCount: Int,
    currentPage: Int,
    currentPageOffsetFraction: Float,
    modifier: Modifier = Modifier,
) {
    val d = LocalDimens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val distance = abs(index.toFloat() - (currentPage + currentPageOffsetFraction))
            val targetSize by animateDpAsState(
                targetValue = when {
                    distance < 0.5f -> d.indicatorDotActive
                    distance < 1.5f -> d.indicatorDotMedium
                    else -> d.indicatorDotSmall
                },
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "dot-size-$index",
            )
            val alpha = (1f - (distance * 0.45f).coerceIn(0f, 0.6f))
            Box(
                modifier = Modifier
                    .padding(horizontal = Spacing.xs)
                    .size(targetSize)
                    .clip(CircleShape)
                    .background(BrandPrimary.copy(alpha = alpha)),
            )
        }
    }
}
