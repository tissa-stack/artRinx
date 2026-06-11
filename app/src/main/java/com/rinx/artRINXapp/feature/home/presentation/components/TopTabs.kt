package com.rinx.artRINXapp.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import coil.compose.AsyncImage
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.home.presentation.HomeTab
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TopTabs(
    activeTab: HomeTab,
    pagerState: PagerState,
    onTabSelected: (HomeTab) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onTabBounds: ((HomeTab, Rect) -> Unit)? = null,
    swipeEnabled: Boolean = true,
) {
    val d = LocalDimens.current
    val tabCount = HomeTab.entries.size
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(
                start = d.screenPaddingHorizontal,
                end = d.screenPaddingHorizontal,
                top = d.logoPaddingVertical,
                bottom = Spacing.sm,
            ),
    ) {
        // Logo centred
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = if (isDarkTheme) R.drawable.artrinx_logo_dark_theme else R.drawable.artrinx_logo_light_theme,
                contentDescription = "ArtRinx",
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(d.logoHeight),
            )
        }

        // Tab container — outer pill with surfaceVariant background + 4dp internal padding.
        // A single BrandPrimary "pill" slides between the tabs, driven by the pager position,
        // and the whole bar can be dragged horizontally to move through the tabs.
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(d.tabPillHeight)
                .clip(RoundedCornerShape(Spacing.lg))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(Spacing.xs),           // 4dp inset on all sides
        ) {
            val cellWidth = maxWidth / tabCount
            val cellWidthPx = with(density) { cellWidth.toPx() }

            // Sliding indicator pill (behind the labels), positioned by the pager offset.
            Box(
                modifier = Modifier
                    .width(cellWidth)
                    .fillMaxHeight()
                    .offset {
                        val pos = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                            .coerceIn(0f, (tabCount - 1).toFloat())
                        IntOffset((pos * cellWidthPx).roundToInt(), 0)
                    }
                    .clip(RoundedCornerShape(Spacing.md))
                    .background(BrandPrimary),
            )

            // Transparent, clickable tab cells with labels on top of the pill. The row itself is
            // horizontally draggable so the user can swipe the pill through the tabs; taps still
            // fall through to the per-cell clickable.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .draggable(
                        enabled = swipeEnabled,
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            // Scale tab-bar pixels → pager pixels so dragging across one tab cell
                            // advances roughly one full page.
                            val pageSize = pagerState.layoutInfo.pageSize.takeIf { it > 0 }
                                ?: return@rememberDraggableState
                            if (cellWidthPx <= 0f) return@rememberDraggableState
                            scope.launch { pagerState.scrollBy(-delta * (pageSize / cellWidthPx)) }
                        },
                        onDragStopped = {
                            scope.launch {
                                val target = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                                    .roundToInt()
                                    .coerceIn(0, tabCount - 1)
                                pagerState.animateScrollToPage(target)
                            }
                        },
                    ),
            ) {
                HomeTab.entries.forEach { tab ->
                    val isActive = tab == activeTab
                    // Continuous position lets the text colour hand off smoothly as the pill slides.
                    val pos = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                        .coerceIn(0f, (tabCount - 1).toFloat())
                    val dist = abs(pos - tab.ordinal).coerceIn(0f, 1f)
                    val textColor = lerp(
                        Color.White,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                        dist,
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(
                                if (onTabBounds != null) {
                                    Modifier.onGloballyPositioned { onTabBounds(tab, it.boundsInWindow()) }
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { onTabSelected(tab) }
                            .semantics {
                                role = Role.Tab
                                selected = isActive
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = tab.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = textColor,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
