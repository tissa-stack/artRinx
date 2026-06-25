package com.rinx.artRINXapp.feature.search.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.rinx.artRINXapp.feature.home.presentation.components.EmptyCurationPreview
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar
import com.rinx.artRINXapp.feature.home.domain.model.CurationItem

/**
 * Width-flexible variant of the app's standard [com.rinx.artRINXapp.feature.home.presentation.components.CollectionCard],
 * for use in the 2-column search results grid. Identical layout — fanned deck of the first 3
 * artworks with the title + curator in a dark row below — but fills its column instead of using a
 * fixed rail width.
 */
@Composable
fun CurationGridCard(
    item: CurationItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val d = LocalDimens.current

    val previews = item.artworkUrls.take(3)
    val count = previews.size.coerceAtLeast(1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(d.cardCornerRadius))
            .clip(RoundedCornerShape(d.cardCornerRadius))
            .background(DarkCardSurface)
            .clickable(onClick = onClick),
    ) {
        // ── Fan image section ────────────────────────────────────────────
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(d.collectionCardHeight),
            contentAlignment = Alignment.TopStart,
        ) {
            val cardWidth = maxWidth
            val imageWidth = if (count <= 1) cardWidth else cardWidth * 0.68f
            val stackOffset = if (count <= 1) 0.dp else (cardWidth - imageWidth) / (count - 1)

            if (previews.isEmpty()) {
                EmptyCurationPreview(modifier = Modifier.fillMaxSize())
            } else {
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
                            model = previews.getOrNull(index),
                            contentDescription = if (isMain) item.title else null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
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
            RinxAvatar(
                url = item.curatorAvatarUrl,
                contentDescription = item.curatorHandle,
                size = d.avatarSize,
                name = item.curatorName,
            )
        }
    }
}