package com.example.artrinx.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.home.domain.model.BannerItem
import kotlin.math.absoluteValue

@Composable
fun FeaturedCarousel(
    items: List<BannerItem>,
    modifier: Modifier = Modifier,
    onClick: (BannerItem) -> Unit = {},
) {
    if (items.isEmpty()) return
    val d = LocalDimens.current
    val pagerState = rememberPagerState(pageCount = { items.size })
    // Auto-scroll removed — user swipes manually

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (items.isNotEmpty() && items[pagerState.currentPage].isSponsored) "Sponsored" else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
        )

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = Spacing.md),
            pageSpacing = Spacing.sm,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            FeaturedCarouselItem(
                item = items[page],
                modifier = Modifier.graphicsLayer {
                    val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1f)
                    scaleX = lerp(0.96f, 1f, 1f - absOffset)
                    scaleY = lerp(0.96f, 1f, 1f - absOffset)
                    alpha = lerp(0.78f, 1f, 1f - absOffset)
                },
                onClick = { onClick(items[page]) },
            )
        }

        ExpandingPagerIndicator(
            pageCount = items.size,
            currentPage = pagerState.currentPage,
            currentPageOffsetFraction = pagerState.currentPageOffsetFraction,
        )
    }
}

@Composable
fun FeaturedCarouselItem(
    item: BannerItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val d = LocalDimens.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(d.bannerHeight)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.45f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.85f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
