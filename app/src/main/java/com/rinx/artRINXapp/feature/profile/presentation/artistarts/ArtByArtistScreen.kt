package com.rinx.artRINXapp.feature.profile.presentation.artistarts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.core.ui.PagingFooter
import com.rinx.artRINXapp.feature.home.presentation.components.state.EmptyView
import com.rinx.artRINXapp.feature.home.presentation.components.state.ErrorView
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar
import com.rinx.artRINXapp.feature.profile.presentation.view.components.ProfileArtMasonryGrid

/**
 * "Art by <artist>" — reached by tapping the credited artist on an artwork. Shows the artist's
 * profile + follow (when they have a RINX id) or a placeholder + "No artRinx profile" (when they
 * don't), then a waterfall grid of art credited to that name.
 */
@Composable
fun ArtByArtistScreen(
    onBack: () -> Unit,
    onOpenProfile: (Int) -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: ArtByArtistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val d = LocalDimens.current
    val listState = rememberLazyListState()

    // Infinite scroll: load the next page once the list can't scroll any further.
    LaunchedEffect(listState) {
        snapshotFlow { listState.canScrollForward }.collect { canScroll ->
            if (!canScroll) viewModel.loadMore()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // ── Top bar: back + artist name ──────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = d.screenPaddingHorizontal, top = Spacing.xs, bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = state.artistName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = BrandPrimary) }

            // First-load failure with nothing to show → full-screen "No internet / Retry".
            state.error != null && state.arts.isEmpty() -> ErrorView(
                error = state.error!!,
                onRetry = viewModel::onRetry,
                modifier = Modifier.fillMaxSize(),
            )

            else -> LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
            ) {
                item(key = "artist-row") {
                    ArtistRow(
                        state = state,
                        onOpenProfile = { viewModel.profileId?.let(onOpenProfile) },
                    )
                }

                item(key = "art-by-title") {
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        text = "Art by ${state.artistName}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        // Align with the artist row + masonry grid (all share Spacing.md left padding).
                        modifier = Modifier.padding(horizontal = Spacing.md),
                    )
                    Spacer(Modifier.height(Spacing.sm))
                }

                if (state.arts.isEmpty()) {
                    item(key = "empty") {
                        EmptyView(
                            icon = Icons.Outlined.Image,
                            title = "No art yet",
                            subtitle = "Nothing credited to this artist yet.",
                            modifier = Modifier.fillMaxWidth().padding(top = Spacing.xl),
                        )
                    }
                } else {
                    item(key = "grid") {
                        ProfileArtMasonryGrid(
                            items = state.arts,
                            onItemClick = { onNavigateToDetail(it.id) },
                        )
                    }
                }

                item(key = "paging-footer") {
                    PagingFooter(state.artsPaging, onRetry = viewModel::retryLoadMore)
                }

                item(key = "bottom-spacer") { Spacer(Modifier.height(Spacing.xxl)) }
            }
        }
    }
}

@Composable
private fun ArtistRow(
    state: ArtByArtistUiState,
    onOpenProfile: () -> Unit,
) {
    val d = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (state.hasProfile) Modifier.clickable { onOpenProfile() } else Modifier)
            // Align the avatar with the "Art by" title + masonry grid (all share Spacing.md).
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RinxAvatar(
            url = state.profile?.avatarUrl,
            contentDescription = state.artistName,
            size = d.avatarSizeLg,
            name = if (state.hasProfile) state.profile?.displayName else state.artistName,
        )
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.profile?.displayName ?: state.artistName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Username (when the artist has an artRinx profile).
            val handle = state.profile?.handle?.removePrefix("@").orEmpty()
            if (state.hasProfile && handle.isNotBlank()) {
                Text(
                    text = "@$handle",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // No follow button (parity with iOS). The status line carries it: "Following" when
            // followed, the artist's follower count when not, or "No artRinx profile" for guests.
            Text(
                text = when {
                    !state.hasProfile -> "No artRinx profile"
                    state.isFollowing -> "Following"
                    else -> {
                        val count = state.profile?.followerCount ?: 0
                        "${formatCount(count)} ${if (count == 1) "follower" else "followers"}"
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Compact follower-count formatting (e.g. 1.2K, 3M) — mirrors the profile stat columns. */
private fun formatCount(value: Int): String = when {
    value >= 1_000_000 -> "${value / 1_000_000}M"
    value >= 1_000 -> "${value / 1_000}K"
    else -> value.toString()
}
