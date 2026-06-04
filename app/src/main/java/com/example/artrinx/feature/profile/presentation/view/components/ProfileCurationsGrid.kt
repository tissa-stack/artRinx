package com.example.artrinx.feature.profile.presentation.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.artrinx.core.theme.DarkCardSurface
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.profile.domain.model.ProfileCurationItem

@Composable
fun ProfileCurationsGrid(
    items: List<ProfileCurationItem>,
    modifier: Modifier = Modifier,
    onItemClick: (ProfileCurationItem) -> Unit = {},
) {
    val d = LocalDimens.current

    val leftItems = items.filterIndexed { i, _ -> i % 2 == 0 }
    val rightItems = items.filterIndexed { i, _ -> i % 2 != 0 }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            leftItems.forEach { item ->
                ProfileCurationCard(item = item, onClick = { onItemClick(item) })
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            rightItems.forEach { item ->
                ProfileCurationCard(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
fun ProfileCurationCard(
    item: ProfileCurationItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val d = LocalDimens.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(d.cardCornerRadius))
            .clip(RoundedCornerShape(d.cardCornerRadius))
            .background(DarkCardSurface)
            .clickable(onClick = onClick),
    ) {
        // ── Stacked artwork image section ─────────────────────────────────
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(d.profileCurationCardHeight),
            contentAlignment = Alignment.TopStart,
        ) {
            val cardWidth = maxWidth
            val imageWidth = cardWidth * 0.70f
            val stackOffset = cardWidth * 0.18f

            // Prefer remote URLs (real curations); fall back to drawable res (mock/preview).
            val previews: List<Any> = item.artworkUrls.ifEmpty { item.artworkRes }

            // Render back-to-front
            previews.indices.reversed().forEach { index ->
                val isMain = index == 0
                Box(
                    modifier = Modifier
                        .width(imageWidth)
                        .height(d.profileCurationCardHeight)
                        .offset(x = stackOffset * index.toFloat())
                        .zIndex((previews.size - index).toFloat())
                        .clip(RoundedCornerShape(d.cardCornerRadius))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    AsyncImage(
                        model = previews[index],
                        contentDescription = if (isMain) item.title else null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Private lock icon overlay
            if (item.isPrivate) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Private",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(Spacing.sm)
                        .size(Spacing.lg),
                )
            }
        }

        // ── Title + handle row ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.handle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}