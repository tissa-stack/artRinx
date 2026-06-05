package com.example.artrinx.feature.profile.presentation.view.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.example.artrinx.R
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.profile.domain.model.UserProfileData

@Composable
fun ProfileHeaderSection(
    profile: UserProfileData,
    isBioExpanded: Boolean,
    onExpandBio: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFollowersClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {},
) {
    val d = LocalDimens.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = d.screenPaddingHorizontal),
    ) {
        // ── Username + settings row ───────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = profile.handle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(Spacing.huge),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = "Settings",
                    tint = BrandPrimary,
                    modifier = Modifier.size(Spacing.xl),
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))

        // ── Avatar + stats row ────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(d.profileAvatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val avatarModel = profile.avatarUrl ?: profile.avatarRes
                if (avatarModel != null) {
                    AsyncImage(
                        model = avatarModel,
                        contentDescription = profile.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(d.profileAvatarSize * 0.55f),
                    )
                }
            }

            Spacer(Modifier.width(Spacing.lg))

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProfileStatColumn(value = profile.artCount, label = "Art")
                ProfileStatColumn(value = profile.curationCount, label = "Curations")
                ProfileStatColumn(value = profile.followerCount, label = "Followers", onClick = onFollowersClick)
                ProfileStatColumn(value = profile.followingCount, label = "Following", onClick = onFollowingClick)
            }
        }

        Spacer(Modifier.height(Spacing.md))

        // ── Display name + role ───────────────────────────────────────────
        Text(
            text = profile.displayName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = profile.role,
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // ── Website + bio (collapsible) ───────────────────────────────────
        if (profile.website.isNotEmpty() || profile.bio.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.sm))
            Column(modifier = Modifier.animateContentSize()) {
                if (profile.website.isNotEmpty()) {
                    Text(
                        text = profile.website,
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (profile.bio.isNotEmpty()) {
                    val truncateAt = 90
                    val isLong = profile.bio.length > truncateAt
                    if (!isBioExpanded && isLong) {
                        Row {
                            Text(
                                text = profile.bio.take(truncateAt) + "... ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "More",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandPrimary,
                                modifier = Modifier.clickable { onExpandBio() },
                            )
                        }
                    } else {
                        Text(
                            text = profile.bio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.lg))
    }
}

@Composable
private fun ProfileStatColumn(value: Int, label: String, onClick: (() -> Unit)? = null) {
    val displayValue = when {
        value >= 1_000_000 -> "${value / 1_000_000}M"
        value >= 1_000 -> "${value / 1_000}K"
        else -> value.toString()
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    ) {
        Text(
            text = displayValue,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}