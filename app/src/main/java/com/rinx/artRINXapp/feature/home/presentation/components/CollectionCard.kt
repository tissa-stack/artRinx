package com.rinx.artRINXapp.feature.home.presentation.components

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
import com.rinx.artRINXapp.core.theme.DarkCardSurface
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.home.domain.model.CurationItem

@Composable
fun CollectionCard(
    item: CurationItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val d = LocalDimens.current

    // Deck shows at most the first 3 artworks (the full list lives on the detail screen).
    val previews = item.artworkUrls.take(3)
    val count = previews.size.coerceAtLeast(1)

    // The fan always spans the full card width regardless of count, so 1 image fills the card,
    // 2 split it, 3 overlap — never leaving blank space on the right.
    val imageWidth = if (count <= 1) d.collectionCardWidth else d.collectionCardWidth * 0.68f
    val stackOffset = if (count <= 1) 0.dp else (d.collectionCardWidth - imageWidth) / (count - 1)

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
            previews.indices.reversed().forEach { index ->
                val isMain = index == 0
                Box(
                    modifier = Modifier
                        .width(imageWidth)
                        .height(d.collectionCardHeight)
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
