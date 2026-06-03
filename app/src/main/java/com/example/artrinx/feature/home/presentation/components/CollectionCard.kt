package com.example.artrinx.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import com.example.artrinx.feature.home.domain.model.CurationItem

@Composable
fun CollectionCard(
    item: CurationItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val d = LocalDimens.current

    // Each image is 68% of the card width; successive images offset 20% to the right.
    // The outer Column clips overflow → all three images are visible side-by-side, straight.
    val imageWidth  = d.collectionCardWidth * 0.68f
    val stackOffset = d.collectionCardWidth * 0.20f   // x-shift per level (no rotation)

    Column(
        modifier = modifier
            .width(d.collectionCardWidth)
            .shadow(4.dp, RoundedCornerShape(d.cardCornerRadius))
            .clip(RoundedCornerShape(d.cardCornerRadius))   // clips fan overflow at card edge
            .background(DarkCardSurface)
            .clickable(onClick = onClick),
    ) {
        // ── Fan image section ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d.collectionCardHeight),
            contentAlignment = Alignment.TopStart,
        ) {
            // Render back-to-front so higher zIndex is on top visually
            item.artworkUrls.indices.reversed().forEach { index ->
                val isMain = index == 0
                Box(
                    modifier = Modifier
                        .width(imageWidth)
                        .height(d.collectionCardHeight)
                        // offset() physically shifts each card to the right — no rotation
                        .offset(x = stackOffset * index.toFloat())
                        .zIndex((item.artworkUrls.size - index).toFloat())
                        .clip(RoundedCornerShape(d.cardCornerRadius))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    AsyncImage(
                        model = item.artworkUrls[index],
                        contentDescription = if (isMain) item.title else null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        // ── Title + avatar row BELOW images ─────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.curatorHandle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Box(
                modifier = Modifier
                    .size(d.avatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (!item.curatorAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.curatorAvatarUrl,
                        contentDescription = item.curatorHandle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(d.avatarSize * 0.6f),
                    )
                }
            }
        }
    }
}
