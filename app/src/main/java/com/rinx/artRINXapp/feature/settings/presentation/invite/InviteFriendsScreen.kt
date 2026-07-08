package com.rinx.artRINXapp.feature.settings.presentation.invite

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.core.ui.PagingFooter
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar
import com.rinx.artRINXapp.core.util.shareText
import com.rinx.artRINXapp.feature.settings.domain.model.Invitee

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteFriendsScreen(
    onBack: () -> Unit,
    viewModel: InviteFriendsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val dimens = LocalDimens.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val hasCode = state.code.isNotBlank()
    val listState = rememberLazyListState()

    // Load the next page of invitees when the list reaches the bottom.
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
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xs),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = "Invite friends",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        when {
            state.isLoading -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = BrandPrimary) }

            state.error != null -> Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = dimens.screenPaddingHorizontal),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = state.error!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.md))
                TextButton(onClick = viewModel::onRetry) {
                    Text("Retry", color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            else -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
              LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = dimens.screenPaddingHorizontal),
                contentPadding = PaddingValues(top = Spacing.md, bottom = Spacing.xxl),
            ) {
                item(key = "header") {
                    Text(
                        text = "Your unique invitation",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = "Send your friends by clicking the share button below!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.lg))

                    // Code box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BrandPrimary, RoundedCornerShape(dimens.authButtonHeight / 4))
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = state.code.ifBlank { "—" },
                            style = MaterialTheme.typography.bodyLarge,
                            color = BrandPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_copy),
                            contentDescription = "Copy invitation code",
                            tint = BrandPrimary,
                            modifier = Modifier
                                .size(Spacing.xl)
                                .clickable(enabled = hasCode) {
                                    clipboard.setText(AnnotatedString(state.code))
                                    Toast.makeText(context, "Invitation code copied", Toast.LENGTH_SHORT).show()
                                },
                        )
                    }
                    Spacer(Modifier.height(Spacing.lg))

                    // Share button (bordered pill)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                            .clickable(enabled = hasCode) {
                                // Share as a link the app already accepts (manifest autoVerify /invite +
                                // DeepLinkParser → pre-fills the invite field on signup). Same host as
                                // ShareTarget.webUrl. Message + code + tappable link.
                                val link = "https://www.artrinx.com/invite/${state.code}"
                                context.shareText(
                                    "Join me on artRINX! Use my invitation code: ${state.code}\n\n$link",
                                )
                            }
                            .padding(vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_share_invite),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(Spacing.lg),
                        )
                        Spacer(Modifier.size(Spacing.sm))
                        Text(
                            text = "Share invitation code",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Spacer(Modifier.height(Spacing.xl))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Your invitees",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        // Remaining/cap this month, e.g. "5/5" (none used) down to "0/5" (limit reached).
                        if (state.invitesPerMonth != null && state.invitesMonthlyCap != null) {
                            Text(
                                text = "${state.invitesPerMonth}/${state.invitesMonthlyCap}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandPrimary,
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                }

                if (state.invitees.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = "No invited users yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.sm),
                        )
                    }
                } else {
                    items(state.invitees, key = { it.id }) { invitee ->
                        InviteeRow(invitee = invitee, avatarSize = dimens.avatarSize)
                    }
                    item(key = "paging-footer") {
                        PagingFooter(state.paging, onRetry = viewModel::retryLoadMore)
                    }
                }

                if (state.invitesPerMonth != null) {
                    item(key = "note") {
                        Spacer(Modifier.height(Spacing.lg))
                        Text(
                            text = "Note: you have ${state.invitesPerMonth} invites per month",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
              }
            }
        }
    }
}

@Composable
private fun InviteeRow(invitee: Invitee, avatarSize: Dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RinxAvatar(
            url = invitee.avatarUrl,
            contentDescription = invitee.name,
            size = avatarSize,
            name = invitee.name,
        )
        Spacer(Modifier.size(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = invitee.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = invitee.handle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (invitee.date.isNotBlank()) {
            Text(
                text = invitee.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
