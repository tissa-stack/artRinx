package com.rinx.artRINXapp.feature.profile.presentation.view.components

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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rinx.artRINXapp.feature.profile.presentation.components.PortfolioLinkDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.profile.domain.model.UserProfileData

@Composable
fun ProfileHeaderSection(
    profile: UserProfileData,
    isBioExpanded: Boolean,
    onExpandBio: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onInviteFriendsClick: () -> Unit = {},
    /** Reports the invite button's window bounds so the first-launch tour can spotlight it. */
    onInviteBounds: ((Rect) -> Unit)? = null,
    onFollowersClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {},
    /** When non-null (own profile opened as a pushed screen), a Back arrow renders inline at the
     *  start of the username row — same line as the name + invite/settings icons. */
    onBack: (() -> Unit)? = null,
    /** Owner-only chrome (invite + settings buttons, clickable follower/following stats). Shown only
     *  on the real Profile tab; hidden when the own profile is opened as a pushed screen from elsewhere. */
    showOwnerActions: Boolean = true,
) {
    val d = LocalDimens.current
    var showPortfolio by remember { mutableStateOf(false) }

    if (showPortfolio && profile.website.isNotEmpty()) {
        PortfolioLinkDialog(
            name = profile.displayName,
            link = profile.website,
            onDismiss = { showPortfolio = false },
        )
    }

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
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    // Pull the button left by its own icon-centering inset so the arrow glyph aligns
                    // vertically with the avatar circle's left edge (both at the screen padding edge).
                    modifier = Modifier
                        .offset(x = -((Spacing.huge - Spacing.xl) / 2))
                        .size(Spacing.huge),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(Spacing.xl),
                    )
                }
            }
            Text(
                text = profile.handle.removePrefix("@"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // Invite + settings are owner-only chrome — only on the real Profile tab.
            if (showOwnerActions) {
                IconButton(
                    onClick = onInviteFriendsClick,
                    modifier = Modifier
                        .size(Spacing.huge)
                        .then(
                            if (onInviteBounds != null) {
                                Modifier.onGloballyPositioned { onInviteBounds(it.boundsInWindow()) }
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_invite_friends),
                        contentDescription = "Invite friends",
                        tint = BrandPrimary,
                        modifier = Modifier.size(Spacing.xxl),
                    )
                }
                IconButton(
                    onClick = onSettingsClick,
                    // Push right by the icon-centering inset so the (last) glyph lands on the content
                    // edge — mirrors the back button's left offset, aligning it with the feedback button.
                    modifier = Modifier
                        .offset(x = (Spacing.huge - Spacing.xxl) / 2)
                        .size(Spacing.huge),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = "Settings",
                        tint = BrandPrimary,
                        modifier = Modifier.size(Spacing.xxl),
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))

        // ── Avatar + stats row ────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            var showEnlargedAvatar by remember { mutableStateOf(false) }
            if (showEnlargedAvatar) {
                EnlargedAvatarDialog(
                    url = profile.avatarUrl,
                    name = profile.displayName,
                    onDismiss = { showEnlargedAvatar = false },
                )
            }
            Box(
                modifier = Modifier
                    .size(d.profileAvatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showEnlargedAvatar = true },
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
                    val initials = com.rinx.artRINXapp.core.util.initialsOf(profile.displayName)
                    if (initials != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(com.rinx.artRINXapp.core.util.pastelColorFor(profile.displayName)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = androidx.compose.ui.graphics.Color(0xFF1D1D1D),
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(d.profileAvatarSize * 0.55f),
                        )
                    }
                }
            }

            Spacer(Modifier.width(Spacing.lg))

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProfileStatColumn(value = profile.artCount, label = "Art")
                ProfileStatColumn(value = profile.curationCount, label = "Collections")
                ProfileStatColumn(
                    value = profile.followerCount,
                    label = "Followers",
                    onClick = if (showOwnerActions) onFollowersClick else null,
                )
                ProfileStatColumn(
                    value = profile.followingCount,
                    label = "Following",
                    onClick = if (showOwnerActions) onFollowingClick else null,
                )
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
        if (profile.role.isNotBlank()) {
            Text(
                text = profile.role,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // ── Website + bio (collapsible) ───────────────────────────────────
        if (profile.website.isNotEmpty() || profile.bio.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.sm))
            Column(modifier = Modifier.animateContentSize()) {
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
                // Portfolio link below the bio — tap opens the third-party-warning popup.
                if (profile.website.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = profile.website,
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { showPortfolio = true },
                    )
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