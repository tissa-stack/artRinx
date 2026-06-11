package com.rinx.artRINXapp.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Collapsing-header layout with pinned tabs and a horizontally-swipeable pager — the "profile"
 * scroll experience: scrolling up moves [header] off the top while [tabBar] anchors to the top, and
 * the active page then scrolls full-screen below the tabs. Each [pageContent] should be its own
 * scrollable container (e.g. a `LazyColumn` with `fillMaxSize`); its nested scroll drives the
 * header collapse/expand automatically.
 */
@Composable
fun CollapsingHeaderTabsPager(
    pagerState: PagerState,
    header: @Composable () -> Unit,
    tabBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    swipeEnabled: Boolean = true,
    pageContent: @Composable (page: Int) -> Unit,
) {
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableIntStateOf(0) }
    var tabsHeightPx by remember { mutableIntStateOf(0) }
    var headerOffsetPx by remember { mutableFloatStateOf(0f) }

    val maxCollapse = headerHeightPx.toFloat()

    // Collapse the header before the list scrolls (scroll up); expand it from the list's leftover
    // when it's already at the top (scroll down).
    val connection = remember(maxCollapse) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0f && maxCollapse > 0f) {
                    val prev = headerOffsetPx
                    headerOffsetPx = (headerOffsetPx + delta).coerceIn(-maxCollapse, 0f)
                    return Offset(0f, headerOffsetPx - prev)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta > 0f && maxCollapse > 0f) {
                    val prev = headerOffsetPx
                    headerOffsetPx = (headerOffsetPx + delta).coerceIn(-maxCollapse, 0f)
                    return Offset(0f, headerOffsetPx - prev)
                }
                return Offset.Zero
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(connection),
    ) {
        val viewportHeight = maxHeight
        val tabsDp = with(density) { tabsHeightPx.toDp() }
        // The pager fills everything below the pinned tab bar so the page is full-screen once the
        // header has collapsed.
        val pagerHeight = (viewportHeight - tabsDp).coerceAtLeast(0.dp)

        Column(
            modifier = Modifier.offset { IntOffset(0, headerOffsetPx.roundToInt()) },
        ) {
            Box(modifier = Modifier.onSizeChanged { headerHeightPx = it.height }) { header() }
            Box(modifier = Modifier.onSizeChanged { tabsHeightPx = it.height }) { tabBar() }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pagerHeight),
                userScrollEnabled = swipeEnabled,
                beyondViewportPageCount = 1,
            ) { page -> pageContent(page) }
        }
    }
}
