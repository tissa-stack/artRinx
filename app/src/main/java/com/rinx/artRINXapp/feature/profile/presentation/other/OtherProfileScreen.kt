package com.rinx.artRINXapp.feature.profile.presentation.other

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import com.rinx.artRINXapp.core.ui.ProfileHeaderTabsPager
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.navigation.NavRoutes
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.DangerRed
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.home.presentation.components.BottomNavBar
import com.rinx.artRINXapp.feature.home.presentation.components.ReportBottomSheet
import com.rinx.artRINXapp.feature.home.presentation.components.state.EmptyView
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileTab
import com.rinx.artRINXapp.feature.profile.domain.model.PublicProfile
import com.rinx.artRINXapp.feature.profile.presentation.components.PortfolioLinkDialog
import com.rinx.artRINXapp.feature.share.domain.model.ShareKind
import com.rinx.artRINXapp.feature.share.domain.model.ShareTarget
import com.rinx.artRINXapp.feature.share.presentation.ShareSheet
import com.rinx.artRINXapp.feature.profile.presentation.other.components.ConfirmActionDialog
import com.rinx.artRINXapp.feature.profile.presentation.view.components.ProfileArtMasonryGrid
import com.rinx.artRINXapp.feature.profile.presentation.view.components.EnlargedAvatarDialog
import com.rinx.artRINXapp.feature.profile.presentation.view.components.ProfileCurationsGrid
import com.rinx.artRINXapp.feature.profile.presentation.view.components.ProfileTabBar
import com.rinx.artRINXapp.feature.profile.presentation.view.components.shimmer.ProfileShimmer

private enum class ConfirmKind { UNFOLLOW, BLOCK, UNBLOCK }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherProfileScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCurationDetail: (String) -> Unit,
    onMessage: (userId: Int) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    activeRoute: String,
    viewModel: OtherProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showReport by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<ConfirmKind?>(null) }
    var shareTarget by remember { mutableStateOf<ShareTarget?>(null) }

    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onActionErrorShown()
        }
    }

    LaunchedEffect(uiState.unblockedSuccess) {
        if (uiState.unblockedSuccess) {
            Toast.makeText(context, "Unblocked ${uiState.profile?.displayName ?: "user"}", Toast.LENGTH_SHORT).show()
            viewModel.onUnblockedShown()
        }
    }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onActionMessageShown()
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                activeRoute = activeRoute,
                onNavigate = { route ->
                    when (route) {
                        NavRoutes.HOME -> onNavigateToHome()
                        NavRoutes.SEARCH -> onNavigateToSearch()
                        NavRoutes.CREATE -> onNavigateToCreate()
                        NavRoutes.NOTIFICATIONS -> onNavigateToNotifications()
                        NavRoutes.PROFILE -> onNavigateToProfile()
                    }
                },
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { innerPadding ->
        when {
            uiState.isLoading -> ProfileShimmer(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .statusBarsPadding(),
            )

            uiState.error != null || uiState.profile == null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .statusBarsPadding()
                    .padding(horizontal = Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = uiState.error ?: "Profile unavailable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.md))
                TextButton(onClick = viewModel::onRetry) {
                    Text("Retry", color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            // The other user has blocked the viewer → don't render their profile/actions/content.
            uiState.profile?.theyBlocked == true -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .statusBarsPadding()
                    .padding(horizontal = Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = uiState.profile?.blockReason ?: "This profile isn't available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            else -> {
                val profile = uiState.profile!!
                val tabs = listOf(ProfileTab.ART, ProfileTab.CURATIONS)
                // Fixed header + tab bar above a swipeable pager (mirrors Home): swipe or drag the
                // bar to change tabs; the header/tabs stay put while pages slide.
                val pagerState = rememberPagerState(
                    initialPage = tabs.indexOf(uiState.activeTab).coerceAtLeast(0),
                ) { tabs.size }
                // Key on settledPage (not currentPage) so a non-adjacent tab jump doesn't fire for
                // intermediate pages and leave the header stuck mid-way.
                LaunchedEffect(pagerState.settledPage) {
                    val swiped = tabs[pagerState.settledPage]
                    if (swiped != uiState.activeTab) viewModel.onTabSelected(swiped)
                }
                LaunchedEffect(uiState.activeTab) {
                    val idx = tabs.indexOf(uiState.activeTab).coerceAtLeast(0)
                    if (pagerState.currentPage != idx) pagerState.animateScrollToPage(idx)
                }
                val artListState = rememberLazyListState()
                val curationListState = rememberLazyListState()
                fun listStateFor(tab: ProfileTab) =
                    if (tab == ProfileTab.CURATIONS) curationListState else artListState
                val activeListState = listStateFor(uiState.activeTab)
                LaunchedEffect(activeListState, uiState.activeTab) {
                    snapshotFlow { activeListState.canScrollForward }
                        .collect { canScroll -> if (!canScroll) viewModel.loadMore() }
                }
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                ProfileHeaderTabsPager(
                    pagerState = pagerState,
                    listStateFor = { listStateFor(tabs[it]) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                        .statusBarsPadding(),
                    header = {
                        OtherProfileHeader(
                            profile = profile,
                            isBioExpanded = uiState.isBioExpanded,
                            onExpandBio = viewModel::onBioExpandToggle,
                            onBack = onBack,
                            onMessage = { onMessage(profile.userId) },
                            onShare = {
                                // A blocked profile can't be shared (server rejects with 400) — block it here.
                                if (profile.iBlocked) {
                                    Toast.makeText(context, "You can't share a blocked profile.", Toast.LENGTH_SHORT).show()
                                } else {
                                    shareTarget = ShareTarget(
                                        kind = ShareKind.PROFILE,
                                        id = profile.userId.toString(),
                                        title = profile.displayName,
                                        subtitle = profile.handle,
                                        imageUrl = profile.avatarUrl,
                                    )
                                }
                            },
                            onFollow = viewModel::follow,
                            onUnfollow = { confirm = ConfirmKind.UNFOLLOW },
                            onBlock = { confirm = ConfirmKind.BLOCK },
                            onReport = { showReport = true },
                            onUnblock = { confirm = ConfirmKind.UNBLOCK },
                        )
                    },
                    tabBar = {
                        ProfileTabBar(
                            activeTab = uiState.activeTab,
                            onTabSelected = viewModel::onTabSelected,
                            pagerState = pagerState,
                            tabs = tabs,
                        )
                    },
                ) { page ->
                    val tab = tabs[page]
                    item(key = "content_${tab.name}") {
                        when {
                            // Blocked → both tabs show the "Profile Blocked" panel instead of content.
                            profile.iBlocked -> ProfileBlockedPanel(
                                name = profile.displayName,
                                modifier = Modifier.padding(top = Spacing.xxxl),
                            )
                            tab == ProfileTab.CURATIONS -> when {
                                uiState.curations.isNotEmpty() -> ProfileCurationsGrid(
                                    items = uiState.curations,
                                    modifier = Modifier.padding(top = Spacing.md),
                                    onItemClick = { onNavigateToCurationDetail(it.id) },
                                )
                                // Content is being (re)fetched with nothing cached yet — e.g. right
                                // after an unblock repopulates the grids. Show a spinner, not "empty".
                                uiState.isRefreshing -> ContentLoading()
                                else -> EmptyView(
                                    icon = Icons.Outlined.Collections,
                                    title = "No collections yet",
                                    subtitle = "This artist hasn't created any collections.",
                                    modifier = Modifier.padding(top = Spacing.md),
                                )
                            }
                            else -> when {
                                uiState.artItems.isNotEmpty() -> ProfileArtMasonryGrid(
                                    items = uiState.artItems,
                                    modifier = Modifier.padding(top = Spacing.md),
                                    onItemClick = { onNavigateToDetail(it.id) },
                                )
                                uiState.isRefreshing -> ContentLoading()
                                else -> EmptyView(
                                    icon = Icons.Outlined.Image,
                                    title = "No art yet",
                                    subtitle = "This artist hasn't posted any art.",
                                    modifier = Modifier.padding(top = Spacing.md),
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }

    val profile = uiState.profile

    shareTarget?.let { target ->
        ShareSheet(target = target, onDismiss = { shareTarget = null })
    }

    // ── Confirmations ────────────────────────────────────────────────────────
    when (confirm) {
        ConfirmKind.UNFOLLOW -> ConfirmActionDialog(
            title = "Are you sure want\nto unfollow \"${profile?.displayName.orEmpty()}\"?",
            confirmLabel = "Unfollow",
            iconRes = R.drawable.ic_navigation_profile,
            isLoading = uiState.isActioning,
            onConfirm = { viewModel.unfollow(); confirm = null },
            onDismiss = { confirm = null },
        )
        ConfirmKind.BLOCK -> ConfirmActionDialog(
            title = "Are you sure want\nto block \"${profile?.displayName.orEmpty()}\"?",
            confirmLabel = "Block",
            confirmColor = DangerRed,
            iconRes = R.drawable.ic_block,
            isLoading = uiState.isActioning,
            onConfirm = { viewModel.block(); confirm = null }, // stay on screen → must dismiss the dialog
            onDismiss = { confirm = null },
        )
        ConfirmKind.UNBLOCK -> ConfirmActionDialog(
            title = "Are you sure want\nto unblock \"${profile?.displayName.orEmpty()}\"?",
            confirmLabel = "Unblock",
            iconRes = R.drawable.ic_block,
            isLoading = uiState.isActioning,
            onConfirm = { viewModel.unblock(); confirm = null },
            onDismiss = { confirm = null },
        )
        null -> Unit
    }

    // ── Report profile ───────────────────────────────────────────────────────
    if (showReport && profile != null) {
        ReportBottomSheet(
            artTitle = "",
            profileName = profile.displayName,
            subjectLabel = "profile",
            isReporting = uiState.isReporting,
            reportSent = uiState.reportSent,
            isBlocking = uiState.isActioning,
            onSubmitReport = viewModel::submitReport,
            onBlockArt = null,
            onBlockUser = { showReport = false; viewModel.onReportClosed(); confirm = ConfirmKind.BLOCK },
            onUnfollowUser = if (profile.isFollowing) {
                { showReport = false; viewModel.onReportClosed(); confirm = ConfirmKind.UNFOLLOW }
            } else {
                null
            },
            onDismiss = { showReport = false; viewModel.onReportClosed() },
        )
    }
}

@Composable
private fun OtherProfileHeader(
    profile: PublicProfile,
    isBioExpanded: Boolean,
    onExpandBio: () -> Unit,
    onBack: () -> Unit,
    onMessage: () -> Unit,
    onShare: () -> Unit,
    onFollow: () -> Unit,
    onUnfollow: () -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit,
    onUnblock: () -> Unit,
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = d.screenPaddingHorizontal),
    ) {
        // ── Back + username + share + follow pill ─────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.offset(x = -Spacing.sm).size(Spacing.huge)) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = profile.handle.removePrefix("@"),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).offset(x = -Spacing.sm),
            )
            // The paper-plane is the Share button: opens the in-app share sheet (not chat).
            IconButton(onClick = onShare, modifier = Modifier.size(Spacing.huge)) {
                Icon(
                    painter = painterResource(R.drawable.ic_send),
                    contentDescription = "Share profile",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(Spacing.xl),
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            FollowControl(
                profile = profile,
                onFollow = onFollow,
                onUnfollow = onUnfollow,
                onBlock = onBlock,
                onReport = onReport,
                onUnblock = onUnblock,
            )
        }

        Spacer(Modifier.height(Spacing.md))

        // ── Avatar + stats ────────────────────────────────────────────────
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
                if (profile.avatarUrl != null) {
                    AsyncImage(
                        model = profile.avatarUrl,
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
                            Icons.Filled.Person,
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
                StatColumn(profile.artCount, "Art")
                StatColumn(profile.curationCount, "Collections")
                StatColumn(profile.followerCount, "Followers")
                StatColumn(profile.followingCount, "Following")
            }
        }

        Spacer(Modifier.height(Spacing.md))

        // ── Name + role + (website/bio) + Message ─────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // "Art Curious" is the lurker role — never shown publicly (handout §17).
                val isArtCurious = profile.role.trim().replace(" ", "").equals("artcurious", ignoreCase = true) ||
                    profile.role.trim().equals("curious", ignoreCase = true)
                if (profile.role.isNotBlank() && !isArtCurious) {
                    Text(
                        text = profile.role,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // Always offer Message — regardless of chat history OR block state. A brand-new chat is
            // handled by the invite gate; a blocked chat opens in its blocked state where the user can
            // unblock (if they blocked) — keeping block/unblock reachable from both profile and chat.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Spacing.sm))
                    .background(BrandPrimary)
                    .clickable(onClick = onMessage)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            ) {
                Text(
                    text = "Message",
                    style = MaterialTheme.typography.labelLarge,
                    color = androidx.compose.ui.graphics.Color.White,
                )
            }
        }

        if (profile.bio.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.xs))
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
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = profile.website,
                style = MaterialTheme.typography.bodySmall,
                color = BrandPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { showPortfolio = true },
            )
        }

        Spacer(Modifier.height(Spacing.lg))
    }
}

/**
 * Follow control (iOS parity):
 *  - Not following → solid blue "Follow"; tapping follows directly (no menu).
 *  - Following → outlined "Following ▾"; tapping opens a dropdown anchored to the button with
 *    Unfollow (red) / Report / Block.
 *  - Blocked → solid red "Blocked"; tapping starts the unblock confirmation.
 */
@Composable
private fun FollowControl(
    profile: PublicProfile,
    onFollow: () -> Unit,
    onUnfollow: () -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit,
    onUnblock: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val following = profile.isFollowing && !profile.iBlocked
    val label = when {
        profile.iBlocked -> "Blocked"
        profile.isFollowing -> "Following"
        else -> "Follow"
    }
    val contentColor = if (following) MaterialTheme.colorScheme.onBackground else Color.White

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .then(
                    when {
                        profile.iBlocked -> Modifier.background(DangerRed)
                        following -> Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                        else -> Modifier.background(BrandPrimary) // "Follow" → solid blue
                    },
                )
                .clickable {
                    when {
                        profile.iBlocked -> onUnblock()
                        following -> menuExpanded = true
                        else -> onFollow()
                    }
                }
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
            // Chevron only on the "Following" state (it opens the dropdown).
            if (following) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(Spacing.lg),
                )
            }
        }

        // Dropdown anchored to the button (replaces the old bottom sheet). Light theme = white.
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else Color.White,
        ) {
            DropdownMenuItem(
                text = { Text("Unfollow", color = DangerRed) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_navigation_profile),
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(Spacing.xl),
                    )
                },
                onClick = { menuExpanded = false; onUnfollow() },
            )
            DropdownMenuItem(
                text = { Text("Report", color = MaterialTheme.colorScheme.onBackground) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_report),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(Spacing.xl),
                    )
                },
                onClick = { menuExpanded = false; onReport() },
            )
            DropdownMenuItem(
                text = { Text("Block", color = MaterialTheme.colorScheme.onBackground) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_block),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(Spacing.xl),
                    )
                },
                onClick = { menuExpanded = false; onBlock() },
            )
        }
    }
}

/** Inline spinner shown while a tab's content is being (re)fetched with nothing to display yet. */
@Composable
private fun ContentLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.xxxl),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = BrandPrimary, modifier = Modifier.size(Spacing.xxl))
    }
}

/** Replaces both content tabs once you've blocked this profile (iOS parity). */
@Composable
private fun ProfileBlockedPanel(name: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxl, vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_block),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(Spacing.giant),
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = "Profile Blocked",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "You have blocked ${name.ifBlank { "this user" }}. Their content is no longer visible to you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatColumn(value: Int, label: String) {
    val display = when {
        value >= 1_000_000 -> "${value / 1_000_000}M"
        value >= 1_000 -> "${value / 1_000}K"
        else -> value.toString()
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(display, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

