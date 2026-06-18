package com.rinx.artRINXapp.feature.profile.presentation.other

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
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
import com.rinx.artRINXapp.core.ui.AppToast
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

    var showActions by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<ConfirmKind?>(null) }
    var shareTarget by remember { mutableStateOf<ShareTarget?>(null) }

    // Pop back after a successful block (toast survives the pop — it's app-context level).
    LaunchedEffect(Unit) {
        viewModel.closed.collect {
            AppToast.show("Blocked ${uiState.profile?.displayName ?: "user"}")
            onBack()
        }
    }

    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            AppToast.show(it)
            viewModel.onActionErrorShown()
        }
    }

    LaunchedEffect(uiState.unblockedSuccess) {
        if (uiState.unblockedSuccess) {
            AppToast.show("Unblocked ${uiState.profile?.displayName ?: "user"}")
            viewModel.onUnblockedShown()
        }
    }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            AppToast.show(it)
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
                                shareTarget = ShareTarget(
                                    kind = ShareKind.PROFILE,
                                    id = profile.userId.toString(),
                                    title = profile.displayName,
                                    subtitle = profile.handle,
                                    imageUrl = profile.avatarUrl,
                                )
                            },
                            onPillClick = {
                                if (profile.iBlocked) confirm = ConfirmKind.UNBLOCK
                                else showActions = true
                            },
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
                        when (tab) {
                            ProfileTab.CURATIONS -> if (uiState.curations.isEmpty()) {
                                EmptyView(
                                    icon = Icons.Outlined.Collections,
                                    title = "No curations yet",
                                    subtitle = "This artist hasn't created any curations.",
                                    modifier = Modifier.padding(top = Spacing.md),
                                )
                            } else {
                                ProfileCurationsGrid(
                                    items = uiState.curations,
                                    modifier = Modifier.padding(top = Spacing.md),
                                    onItemClick = { onNavigateToCurationDetail(it.id) },
                                )
                            }
                            else -> if (uiState.artItems.isEmpty()) {
                                EmptyView(
                                    icon = Icons.Outlined.Image,
                                    title = "No art yet",
                                    subtitle = "This artist hasn't posted any art.",
                                    modifier = Modifier.padding(top = Spacing.md),
                                )
                            } else {
                                ProfileArtMasonryGrid(
                                    items = uiState.artItems,
                                    modifier = Modifier.padding(top = Spacing.md),
                                    onItemClick = { onNavigateToDetail(it.id) },
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }

    // ── Actions sheet (Following ▾) ──────────────────────────────────────────
    val profile = uiState.profile
    if (showActions && profile != null) {
        ProfileActionsSheet(
            displayName = profile.displayName,
            isFollowing = profile.isFollowing,
            iBlocked = profile.iBlocked,
            onFollow = { showActions = false; viewModel.follow() },
            onUnfollow = { showActions = false; confirm = ConfirmKind.UNFOLLOW },
            onBlock = { showActions = false; confirm = ConfirmKind.BLOCK },
            onUnblock = { showActions = false; confirm = ConfirmKind.UNBLOCK },
            onReport = { showActions = false; showReport = true },
            onDismiss = { showActions = false },
        )
    }

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
            onConfirm = { viewModel.block() }, // screen pops back via `closed`
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
    onPillClick: () -> Unit,
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
            FollowPill(
                label = when {
                    profile.iBlocked -> "Unblock"
                    profile.isFollowing -> "Following"
                    else -> "Follow"
                },
                showChevron = !profile.iBlocked,
                onClick = onPillClick,
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
                StatColumn(profile.curationCount, "Curations")
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
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (profile.canMessage) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Spacing.sm))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onMessage)
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                ) {
                    Text(
                        text = "Message",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
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

@Composable
private fun FollowPill(label: String, showChevron: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (showChevron) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(Spacing.lg),
            )
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileActionsSheet(
    displayName: String,
    isFollowing: Boolean,
    iBlocked: Boolean,
    onFollow: () -> Unit,
    onUnfollow: () -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onReport: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xxl),
        ) {
            Text(
                text = (if (isFollowing) "Following " else "") + displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
            )
            when {
                iBlocked -> ActionRow(R.drawable.ic_block, "Unblock profile", onUnblock)
                isFollowing -> ActionRow(R.drawable.ic_navigation_profile, "Unfollow profile", onUnfollow)
                else -> ActionRow(R.drawable.ic_navigation_profile, "Follow profile", onFollow)
            }
            if (!iBlocked) ActionRow(R.drawable.ic_block, "Block profile", onBlock)
            ActionRow(R.drawable.ic_report, "Report profile", onReport)
        }
    }
}

@Composable
private fun ActionRow(iconRes: Int, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(Spacing.xl),
        )
        Spacer(Modifier.width(Spacing.lg))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
    }
}
