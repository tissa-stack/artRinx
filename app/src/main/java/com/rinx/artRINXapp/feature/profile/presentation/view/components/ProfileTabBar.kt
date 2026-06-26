package com.rinx.artRINXapp.feature.profile.presentation.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
                    .fillMaxHeight(),
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
                            .clickable { scope.launch { pagerState.animateScrollToPage(tabs.indexOf(tab)) } },
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
