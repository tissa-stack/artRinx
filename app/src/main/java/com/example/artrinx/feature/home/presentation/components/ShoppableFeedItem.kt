package com.example.artrinx.feature.home.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.artrinx.R
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.home.domain.model.ShoppablePost

@Composable
fun ShoppableFeedItem(
    post: ShoppablePost,
    onLike: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val d = LocalDimens.current
    var descriptionExpanded by remember { mutableStateOf(false) }
    var showShopDialog by remember { mutableStateOf(false) }

    if (showShopDialog) {
        ShopLinkDialog(
            artistName = post.artistName,
            shopUrl    = post.shopUrl,
            onDismiss  = { showShopDialog = false },
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {

        // ── Header: avatar + name + role ─────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(d.avatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (!post.artistAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = post.artistAvatarUrl,
                        contentDescription = post.artistName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
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
            Spacer(Modifier.width(Spacing.sm))
            Column {
                Text(
                    text = post.artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = post.artistRole,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }

        // ── Artwork image — full width, no corner radius ──────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d.feedImageHeight)
                .clickable(onClick = onClick),
        ) {
            AsyncImage(
                model = post.imageUrl,
                contentDescription = post.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // ── Title + actions ───────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = post.artistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            // Icons column: 3 icons row + count below (same as DiscoverFeedItem)
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_send),
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(Spacing.xxl),
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_like),
                        contentDescription = "Like",
                        tint = if (post.isLiked) BrandPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(Spacing.xxl)
                            .clickable { onLike() },
                    )
                }
                if (post.likeCount > 0) {
                    Text(
                        text = post.likeCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
        }

        // ── Medium + Shop Art button ──────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Medium",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = post.medium,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            // Shop Art — theme-aware button: light gray in light mode, dark gray in dark mode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Spacing.sm))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showShopDialog = true }
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Shop Art",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }


        // ── Description ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (descriptionExpanded) "less" else "more",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { descriptionExpanded = !descriptionExpanded },
                )
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = post.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = if (descriptionExpanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.animateContentSize(),
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = Spacing.md),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )
        Spacer(Modifier.height(Spacing.md))
    }
}
