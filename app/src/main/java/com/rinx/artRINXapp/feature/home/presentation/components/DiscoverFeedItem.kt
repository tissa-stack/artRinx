package com.rinx.artRINXapp.feature.home.presentation.components

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
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.home.domain.model.FeedPost
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar
import com.rinx.artRINXapp.feature.share.domain.model.ShareKind
import com.rinx.artRINXapp.feature.share.domain.model.ShareTarget

@Composable
fun DiscoverFeedItem(
    post: FeedPost,
    onLike: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onAddToCuration: () -> Unit = {},
    onArtistClick: () -> Unit = {},
    onShare: (ShareTarget) -> Unit = {},
) {
    val d = LocalDimens.current
    Column(modifier = modifier.fillMaxWidth()) {

        // ── Header: avatar + name + role ──────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = post.ownerId != null, onClick = onArtistClick)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Header = the UPLOADER (the profile this row navigates to), NOT the credited artist.
            val uploaderName = post.ownerName.ifBlank { post.artistName }
            RinxAvatar(
                url = post.artistAvatarUrl,
                contentDescription = uploaderName,
                size = d.avatarSize,
                name = uploaderName,
            )
            Spacer(Modifier.width(Spacing.sm))
            Column {
                Text(
                    text = uploaderName,
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
                model = post.imageUrl,
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
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add_to),
                    contentDescription = "Add to curation",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(Spacing.xxl)
                        .clickable { onAddToCuration() },
                )
                Icon(
                    painter = painterResource(R.drawable.ic_send),
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(Spacing.xxl)
                        .clickable {
                            onShare(
                                ShareTarget(
                                    kind = ShareKind.ARTWORK,
                                    id = post.id,
                                    title = post.title,
                                    subtitle = "by ${post.artistName}",
                                    imageUrl = post.imageUrl,
                                ),
                            )
                        },
                )
                // Heart + count: count centered exactly below the heart.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LikeButton(
                        isLiked = post.isLiked,
                        onClick = onLike,
                        size = Spacing.xl,
                    )
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
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = Spacing.md),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )
        Spacer(Modifier.height(Spacing.md))
    }
}
