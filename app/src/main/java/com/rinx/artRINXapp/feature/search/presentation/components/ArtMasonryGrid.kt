package com.rinx.artRINXapp.feature.search.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.search.domain.model.CardHeight
import com.rinx.artRINXapp.feature.search.domain.model.SearchResultItem

// ── Lazy staggered grid — for search results ──────────────────────────────────

@Composable
fun ArtMasonryResultsGrid(
    items: List<SearchResultItem>,
    modifier: Modifier = Modifier,
    onItemClick: (SearchResultItem) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(Spacing.md),
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalItemSpacing = Spacing.sm,
    ) {
        items(items = items, key = { it.id }) { item ->
            MasonryCard(item = item, onClick = { onItemClick(item) })
        }
    }
}

// ── Non-lazy manual 2-col grid — for recommended section ─────────────────────

@Composable
fun ManualMasonryGrid(
    items: List<SearchResultItem>,
    modifier: Modifier = Modifier,
    onItemClick: (SearchResultItem) -> Unit = {},
) {
    val leftItems = items.filterIndexed { i, _ -> i % 2 == 0 }
    val rightItems = items.filterIndexed { i, _ -> i % 2 != 0 }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            leftItems.forEach { item ->
                MasonryCard(item = item, onClick = { onItemClick(item) })
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            rightItems.forEach { item ->
                MasonryCard(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}

// ── Shared card composable — image with title/artist overlaid on a bottom scrim ─

@Composable
fun MasonryCard(
    item: SearchResultItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val d = LocalDimens.current
    val imageHeight = when (item.cardHeight) {
        CardHeight.SHORT -> d.masonryCardHeightShort
        CardHeight.MEDIUM -> d.masonryCardHeightMedium
        CardHeight.TALL -> d.masonryCardHeightTall
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(imageHeight)
            .clip(RoundedCornerShape(Spacing.sm))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
    ) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        CardTextScrim(
            title = item.title,
            subtitle = item.artistName.takeIf { it.isNotBlank() }?.let { "by $it" },
        )
    }
}

/** Bottom gradient + title/subtitle overlay used on art & curation cards. */
@Composable
fun BoxScope.CardTextScrim(
    title: String,
    subtitle: String?,
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.5f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.78f),
                    ),
                ),
            ),
    )
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}