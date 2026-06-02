package com.example.artrinx.feature.search.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.search.domain.model.CardHeight
import com.example.artrinx.feature.search.domain.model.SearchResultItem

// ── Lazy staggered grid — for search results ──────────────────────────────────

@Composable
fun ArtMasonryResultsGrid(
    items: List<SearchResultItem>,
    modifier: Modifier = Modifier,
    onItemClick: (SearchResultItem) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(Spacing.md),
) {
    LazyVerticalStaggeredGrid(
        columns               = StaggeredGridCells.Fixed(2),
        modifier              = modifier,
        contentPadding        = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalItemSpacing   = Spacing.sm,
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
    val leftItems  = items.filterIndexed { i, _ -> i % 2 == 0 }
    val rightItems = items.filterIndexed { i, _ -> i % 2 != 0 }

    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Column(
            modifier              = Modifier.weight(1f),
            verticalArrangement   = Arrangement.spacedBy(Spacing.sm),
        ) {
            leftItems.forEach { item ->
                MasonryCard(item = item, onClick = { onItemClick(item) })
            }
        }
        Column(
            modifier              = Modifier.weight(1f),
            verticalArrangement   = Arrangement.spacedBy(Spacing.sm),
        ) {
            rightItems.forEach { item ->
                MasonryCard(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}

// ── Shared card composable ────────────────────────────────────────────────────

@Composable
fun MasonryCard(
    item: SearchResultItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val d = LocalDimens.current
    val imageHeight = when (item.cardHeight) {
        CardHeight.SHORT  -> d.masonryCardHeightShort
        CardHeight.MEDIUM -> d.masonryCardHeightMedium
        CardHeight.TALL   -> d.masonryCardHeightTall
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        AsyncImage(
            model              = item.imageRes,
            contentDescription = item.title,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .fillMaxWidth()
                .height(imageHeight),
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text       = item.title,
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onBackground,
            maxLines   = 2,
            overflow   = TextOverflow.Ellipsis,
        )
        Text(
            text     = "by ${item.artistName}",
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
