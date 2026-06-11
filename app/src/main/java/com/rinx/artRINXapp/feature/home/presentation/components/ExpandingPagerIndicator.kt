package com.rinx.artRINXapp.feature.home.presentation.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * Sliding-window page indicator. Shows at most [maxDots] dots regardless of page count:
 * the focused dot is BrandPrimary and larger, the rest are ash; the window slides so the
 * active dot stays centered (except near the list ends). Edge dots that still have pages
 * beyond them shrink to hint there's more.
 */
@Composable
fun ExpandingPagerIndicator(
    pageCount: Int,
    currentPage: Int,
    @Suppress("UNUSED_PARAMETER") currentPageOffsetFraction: Float,
    modifier: Modifier = Modifier,
    maxDots: Int = 5,
) {
    if (pageCount <= 1) return
    val d = LocalDimens.current
    val window = minOf(maxDots, pageCount)
    val ash = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    // Stateful sliding window: the active dot MOVES across the visible dots, and the window only
    // scrolls once the active dot reaches an edge — so every banner change shows real progress.
    var windowStart by remember { mutableIntStateOf(0) }
    LaunchedEffect(currentPage, pageCount, window) {
        val maxStart = (pageCount - window).coerceAtLeast(0)
        windowStart = when {
            currentPage < windowStart -> currentPage
            currentPage > windowStart + window - 1 -> currentPage - window + 1
            else -> windowStart
        }.coerceIn(0, maxStart)
    }
    val end = (windowStart + window).coerceAtMost(pageCount)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (index in windowStart until end) {
            val isActive = index == currentPage
            // Dots at the window edge that still have pages beyond them shrink to hint "there's more".
            val isOverflowEdge =
                (index == windowStart && windowStart > 0) || (index == end - 1 && end < pageCount)
            val targetSize = when {
                isActive -> d.indicatorDotActive
                isOverflowEdge -> d.indicatorDotSmall
                else -> d.indicatorDotMedium
            }
            val animatedSize by animateDpAsState(
                targetValue = targetSize,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "dot-size-$index",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = Spacing.xs)
                    .size(animatedSize)
                    .clip(CircleShape)
                    .background(if (isActive) BrandPrimary else ash),
            )
        }
    }
}
