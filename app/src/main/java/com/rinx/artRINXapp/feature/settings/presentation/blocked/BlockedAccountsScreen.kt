package com.rinx.artRINXapp.feature.settings.presentation.blocked

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.core.ui.PagingFooter
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar
import com.rinx.artRINXapp.feature.search.presentation.components.SearchMessageView
import com.rinx.artRINXapp.feature.settings.domain.model.BlockedAccount

@Composable
fun BlockedAccountsScreen(
    onBack: () -> Unit,
    viewModel: BlockedAccountsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val dimens = LocalDimens.current
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Load the next page when the list reaches the bottom.
    LaunchedEffect(listState) {
        snapshotFlow { listState.canScrollForward }.collect { canScroll ->
            if (!canScroll) viewModel.loadMore()
        }
    }

    LaunchedEffect(state.unblockError) {
        state.unblockError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onUnblockErrorShown()
        }
    }

    LaunchedEffect(state.unblockedName) {
        state.unblockedName?.let {
            Toast.makeText(context, "Unblocked $it", Toast.LENGTH_SHORT).show()
            viewModel.onUnblockMessageShown()
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
                text = "Blocked Accounts",
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

            state.accounts.isEmpty() -> SearchMessageView(
                title    = "No blocked users",
                subtitle = "You haven't blocked any user yet.",
                icon     = Icons.Outlined.Block,
                modifier = Modifier.weight(1f),
            )

            else -> LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(top = Spacing.sm, bottom = Spacing.xxl),
            ) {
                items(state.accounts, key = { it.id }) { account ->
                    BlockedRow(
                        account = account,
                        avatarSize = dimens.avatarSizeLg,
                        onUnblock = { viewModel.onUnblockRequest(account) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                }
                item(key = "paging-footer") {
                    PagingFooter(state.paging, onRetry = viewModel::retryLoadMore)
                }
            }
        }
    }

    state.pendingUnblock?.let { account ->
        UnblockDialog(
            profileName = account.name,
            onConfirm = viewModel::onConfirmUnblock,
            onDismiss = viewModel::onDismissUnblock,
            isUnblocking = state.isUnblocking,
        )
    }
}

@Composable
private fun BlockedRow(
    account: BlockedAccount,
    avatarSize: Dp,
    onUnblock: () -> Unit,
) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.screenPaddingHorizontal, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        RinxAvatar(
            url = account.avatarUrl,
            contentDescription = account.name,
            size = avatarSize,
            name = account.name,
        )
        Spacer(Modifier.size(Spacing.md))

        // Name + role
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (account.role.isNotBlank()) {
                Text(
                    text = account.role,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Unblock pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(BrandPrimary)
                .clickable(onClick = onUnblock)
                .padding(horizontal = Spacing.xl, vertical = Spacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Unblock",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
