package com.rinx.artRINXapp.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import kotlin.math.ceil

private data class SlotConfig(
    val offsetX: Dp,
    val offsetY: Dp,
    val scale: Float,
    val alpha: Float,
)

// Pile slots: slot 0 = the centered front card; deeper slots peek with alternating L/R offsets.
private val STACK_CONFIGS = listOf(
    SlotConfig(0.dp, 0.dp, 1.000f, 1.00f),       // front — centered
    SlotConfig(20.dp, (-4).dp, 0.960f, 1.00f),   // 2nd — peeks right
    SlotConfig((-22).dp, (-7).dp, 0.920f, 0.95f), // 3rd — peeks left
    SlotConfig(26.dp, (-10).dp, 0.880f, 0.85f),  // 4th — peeks right
)
private val LAST_SLOT = (STACK_CONFIGS.size - 1).toFloat()

/**
 * Swipeable curation deck — a STACKED photo pile (front card centered, upcoming cards peeking behind)
 * driven by a [HorizontalPager] underneath. The pager owns the drag/fling/settle physics (no jerk) and
 * [HorizontalPager.beyondViewportPageCount] keeps neighbor images decoded so the card you swipe to —
 * in EITHER direction — is already loaded (no wrong-image flash). A per-page transform cancels the
 * pager's horizontal layout for the pile cards (so they stack centered) while letting the dismissed
 * card ride the scroll off to the side. Swipe left → next, swipe right → previous.
 */
@Composable
fun CurationCardStack(
    // Accepts either image-URL strings (home/detail) or drawable-res Ints (upload preview);
    // Coil's AsyncImage model takes both.
    artworks: List<Any>,
    modifier: Modifier = Modifier,
    onTopIndexChanged: (Int) -> Unit = {},
    onCardClick: (Int) -> Unit = {},
) {
    if (artworks.isEmpty()) return

    val d = LocalDimens.current
    val pagerState = rememberPagerState(pageCount = { artworks.size })

    // Report the SETTLED focused card (only once the swipe settles — no mid-gesture churn).
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect(onTopIndexChanged)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cardWidth = maxWidth * 0.78f
        val cardHeight = d.artDetailImageHeight * 1.08f
        val cardCorner = d.cardCornerRadius

        HorizontalPager(
            state = pagerState,
            // Keep the pile cards (+1..+3) AND the previous card composed + image-decoded → no flash.
            beyondViewportPageCount = 3,
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight + Spacing.lg),
        ) { page ->
            // Live signed position of this page relative to the focused card, in page-width units:
            //   0  = centered front
            //  >0  = upcoming → sits in the pile BEHIND the front (peek)
            //  <0  = already passed → rides the scroll off to the LEFT (dismissed / sliding back in)
            val pos = (page - pagerState.currentPage) - pagerState.currentPageOffsetFraction

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Front + dismissed cards draw above the pile; deeper pile cards sink behind.
                    .zIndex(-pos)
                    .graphicsLayer {
                        if (pos >= 0f) {
                            // Pile / front: cancel the pager's horizontal layout so the card stays
                            // centered, then apply the slot's peek offset + scale + fade by depth.
                            val depth = pos.coerceIn(0f, LAST_SLOT)
                            val lo = depth.toInt()
                            val hi = ceil(depth).toInt().coerceAtMost(STACK_CONFIGS.size - 1)
                            val f = depth - lo
                            val a = STACK_CONFIGS[lo]
                            val b = STACK_CONFIGS[hi]
                            translationX = -pos * size.width + lerp(a.offsetX.toPx(), b.offsetX.toPx(), f)
                            translationY = lerp(a.offsetY.toPx(), b.offsetY.toPx(), f)
                            val s = lerp(a.scale, b.scale, f)
                            scaleX = s
                            scaleY = s
                            // Hide cards deeper than the visible pile.
                            alpha = if (pos > LAST_SLOT) 0f else lerp(a.alpha, b.alpha, f)
                        } else {
                            // Dismissed / incoming-previous: ride the scroll (slides off-left / back in)
                            // and cross-fade so it appears/disappears smoothly at the screen edge.
                            translationX = 0f
                            scaleX = 1f
                            scaleY = 1f
                            alpha = (1f + pos).coerceIn(0f, 1f)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(cardWidth)
                        .height(cardHeight)
                        .clip(RoundedCornerShape(cardCorner))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onCardClick(page) },
                ) {
                    AsyncImage(
                        model = artworks[page],
                        contentDescription = "Artwork ${page + 1} of ${artworks.size}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
