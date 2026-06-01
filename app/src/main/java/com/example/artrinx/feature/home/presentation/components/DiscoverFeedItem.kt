package com.example.artrinx.feature.home.presentation.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.artrinx.feature.home.domain.model.FeedPost

@Composable
fun DiscoverFeedItem(
    post: FeedPost,
    onLike: () -> Unit,
    onBookmark: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val d = LocalDimens.current
    Column(modifier = modifier.fillMaxWidth()) {

        // ── Header: avatar + name + role ──────────────────────────────
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
                if (post.artistAvatarRes != null) {
                    AsyncImage(
                        model = post.artistAvatarRes,
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

        // ── Artwork image — full width ─────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d.feedImageHeight)
                .clickable(onClick = onClick),
        ) {
            AsyncImage(
                model = post.imageRes,
                contentDescription = post.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // ── Footer: title + artist LEFT | actions RIGHT ────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Spacing.md,
                    vertical = Spacing.sm,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Title + artist name
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

            // Action icons: 3 icons in a row, count right-aligned below
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_to),
                        contentDescription = "Save",
                        tint = if (post.isBookmarked) BrandPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(Spacing.xl)
                            .clickable { onBookmark() },
                    )
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
                            .size(Spacing.xl)
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

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = Spacing.md),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )
        Spacer(Modifier.height(Spacing.md))
    }
}
