package com.rinx.artRINXapp.feature.profile.presentation.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileTab
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Profile tabs with the same sliding-pill + draggable behaviour as the Home tabs: a single
 * BrandPrimary pill tracks the [pagerState] position, the text colour cross-fades by proximity,
 * and the bar itself can be dragged horizontally to move between tabs.
 */
@Composable
fun ProfileTabBar(
    activeTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    tabs: List<ProfileTab> = ProfileTab.entries,
    swipeEnabled: Boolean = true,
) {
    val d = LocalDimens.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val tabCount = tabs.size

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = d.screenPaddingHorizontal, vertical = Spacing.sm),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(d.tabPillHeight)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(Spacing.xs),
        ) {
            val cellWidth = maxWidth / tabCount
            val cellWidthPx = with(density) { cellWidth.toPx() }

            // Sliding pill behind the labels, positioned by the pager offset.
            Box(
                modifier = Modifier
                    .width(cellWidth)
                    .fillMaxHeight()
                    .offset {
                        val pos = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                            .coerceIn(0f, (tabCount - 1).toFloat())
                        IntOffset((pos * cellWidthPx).roundToInt(), 0)
                    }
                    .clip(RoundedCornerShape(50))
                    .background(BrandPrimary),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .draggable(
                        enabled = swipeEnabled,
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            val pageSize = pagerState.layoutInfo.pageSize.takeIf { it > 0 }
                                ?: return@rememberDraggableState
                            if (cellWidthPx <= 0f) return@rememberDraggableState
                            // Positive delta = finger right → next tab (pill follows the finger).
                            scope.launch { pagerState.scrollBy(delta * (pageSize / cellWidthPx)) }
                        },
                        onDragStopped = {
                            scope.launch {
                                val target = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                                    .roundToInt().coerceIn(0, tabCount - 1)
                                pagerState.animateScrollToPage(target)
                            }
                        },
                    ),
            ) {
                tabs.forEach { tab ->
                    val pos = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                        .coerceIn(0f, (tabCount - 1).toFloat())
                    val dist = abs(pos - tabs.indexOf(tab)).coerceIn(0f, 1f)
                    val textColor = lerp(
                        Color.White,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        dist,
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onTabSelected(tab) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = tab.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
