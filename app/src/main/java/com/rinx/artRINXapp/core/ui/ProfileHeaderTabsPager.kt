package com.rinx.artRINXapp.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.rinx.artRINXapp.core.theme.Spacing
import kotlin.math.roundToInt

/**
 * Profile scroll surface: a collapsing [header] above a sticky [tabBar], over a horizontally-swipeable
 * [HorizontalPager]. Scrolling a page up slides the header off the top first (then the list scrolls);
 * scrolling back down re-expands it. The [tabBar] stays pinned just below the status bar. Swiping (or
 * dragging the tab bar) changes tabs.
 *
 * Design that keeps it robust:
 * - The pager fills the **full** height and each page is a plain [LazyColumn], so every tab scrolls
 *   cleanly to its last item — no cut-off, no dead-space.
 * - The header + tab bar are an overlay drawn on top, translated up by a single **shared** collapse
 *   offset. Each page reserves room for them via top content-padding that shrinks as the header
 *   collapses, so content and header move in lockstep (no gap).
 * - Because the collapse offset is one shared value, switching tabs never shifts the header/tabs.
 *
 * The caller should apply `statusBarsPadding()` (and any bottom inset) to [modifier].
 */
@Composable
fun ProfileHeaderTabsPager(
    pagerState: PagerState,
    header: @Composable () -> Unit,
    tabBar: @Composable () -> Unit,
    listStateFor: (page: Int) -> LazyListState,
    modifier: Modifier = Modifier,
    swipeEnabled: Boolean = true,
    pageContent: LazyListScope.(page: Int) -> Unit,
) {
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableIntStateOf(0) }
    var tabBarHeightPx by remember { mutableIntStateOf(0) }
    // How far the header is collapsed, in px: 0 = expanded, headerHeightPx = fully collapsed.
    var collapse by remember { mutableFloatStateOf(0f) }

    // Collapse the header before the list scrolls (up-drags); re-expand once the list is back at its
    // top (leftover down-drags). Only the header collapses — the tab bar stays pinned.
    val nestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta >= 0f) return Offset.Zero
                val max = headerHeightPx.toFloat()
                val prev = collapse
                val next = (prev - delta).coerceIn(0f, max)
                collapse = next
                return Offset(0f, prev - next) // negative: the up-scroll we consumed to collapse
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta <= 0f) return Offset.Zero
                val max = headerHeightPx.toFloat()
                val prev = collapse
                val next = (prev - delta).coerceIn(0f, max)
                collapse = next
                return Offset(0f, prev - next) // positive: leftover down-scroll used to expand
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().nestedScroll(nestedScroll).clipToBounds()) {
        // Full-height pager underneath: each page reserves space for the (shrinking) header + tab bar
        // via top padding, so it scrolls fully with no cut-off.
        val topPadding = with(density) { (headerHeightPx + tabBarHeightPx - collapse).coerceAtLeast(0f).toDp() }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = swipeEnabled,
            beyondViewportPageCount = 1,
        ) { page ->
            LazyColumn(
                state = listStateFor(page),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = topPadding, bottom = Spacing.xxl),
            ) {
                pageContent(page)
            }
        }

        // Header + tab bar overlay, opaque, sliding up as the header collapses (tab bar stays pinned).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, -collapse.roundToInt()) }
                .background(MaterialTheme.colorScheme.background),
        ) {
            Box(modifier = Modifier.onSizeChanged { headerHeightPx = it.height }) { header() }
            Box(modifier = Modifier.onSizeChanged { tabBarHeightPx = it.height }) { tabBar() }
        }
    }
}
