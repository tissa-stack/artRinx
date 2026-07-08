package com.rinx.artRINXapp.feature.home.presentation.detail

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.feature.home.presentation.detail.components.ZoomableImage
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.DangerRed
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar
import com.rinx.artRINXapp.feature.profile.presentation.other.components.BlockConfirmDialog
import com.rinx.artRINXapp.feature.profile.presentation.other.components.ConfirmActionDialog
import com.rinx.artRINXapp.feature.share.domain.model.ShareKind
import com.rinx.artRINXapp.feature.share.domain.model.ShareTarget
import com.rinx.artRINXapp.feature.share.presentation.ShareSheet
import com.rinx.artRINXapp.feature.upload.domain.model.CurationSource
import com.rinx.artRINXapp.feature.home.presentation.components.AddToCurationSheet
import com.rinx.artRINXapp.feature.home.presentation.components.shimmer.ArtDetailShimmer
import com.rinx.artRINXapp.feature.home.presentation.components.shimmer.rememberShimmerBrush
import com.rinx.artRINXapp.feature.home.presentation.components.ArtworkCard
import com.rinx.artRINXapp.feature.home.presentation.components.BottomNavBar
import com.rinx.artRINXapp.feature.home.presentation.components.ShopArtButton
import com.rinx.artRINXapp.feature.home.presentation.components.LikeButton
import com.rinx.artRINXapp.core.ui.ErrorSnackbarHost
import com.rinx.artRINXapp.feature.home.presentation.components.ReportBottomSheet
import com.rinx.artRINXapp.feature.home.presentation.components.ReportSentDialog
import com.rinx.artRINXapp.feature.home.presentation.components.SectionHeader
import com.rinx.artRINXapp.feature.home.presentation.components.SendMessageBottomSheet
import com.rinx.artRINXapp.feature.home.presentation.components.ShopLinkDialog
import com.rinx.artRINXapp.feature.upload.presentation.components.DeleteConfirmDialog
import kotlinx.coroutines.flow.distinctUntilChanged

/** Prefetch the next "More like this" page once within this many cards of the rail's right end. */
private const val SIMILAR_PREFETCH = 3

@Composable
fun ArtDetailScreen(
    onBack: () -> Unit,
    /** Leave for the originating tab, skipping now-broken intermediate screens — used after
     *  blocking the artist (whose art/detail we may have come through). */
    onExitToSafeScreen: () -> Unit = onBack,
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateHome: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNewCuration: () -> Unit = {},
    onEditArt: () -> Unit = {},
    onOpenProfile: (Int) -> Unit = {},
    onOpenArtistArts: (name: String, artistId: Int?) -> Unit = { _, _ -> },
    activeRoute: String = "home",
    viewModel: ArtDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showReportSheet by remember { mutableStateOf(false) }
    // Single source of truth for the mutually-exclusive bottom sheets (Share + Add-to-Curation), hosted
    // together below so opening one closes the other — they can't co-exist / dismiss-and-reopen (SCRUM-55).
    var activeSheet by remember { mutableStateOf<ArtDetailSheet>(ArtDetailSheet.None) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    // "art" or "user" while a block confirmation dialog is up (asked before any block).
    var blockConfirm by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    // Pop back once the artwork is deleted.
    LaunchedEffect(Unit) {
        viewModel.deleted.collect { onBack() }
    }

    // Artwork no longer exists server-side (404) → toast + pop instead of showing stale cached detail.
    LaunchedEffect(Unit) {
        viewModel.gone.collect {
            Toast.makeText(context, "This artwork is no longer available.", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    // Close the sheet and leave once the art/user is blocked (toast survives the navigation).
    // Blocking the USER exits to a safe tab (the previous screen may be the blocked artist's own
    // now-broken art/detail); blocking a single ART just pops one level (artist still valid).
    LaunchedEffect(Unit) {
        viewModel.blocked.collect { outcome ->
            Toast.makeText(context, outcome.message, Toast.LENGTH_SHORT).show()
            showReportSheet = false
            if (outcome.wasUserBlock) onExitToSafeScreen() else onBack()
        }
    }

    // A report/action failure closes the report sheet and surfaces the reason in the red error
    // banner (Scaffold snackbarHost). Only flip the local flag here — do NOT call
    // onReportSheetClosed(), which would null actionError before the banner reads it.
    LaunchedEffect(uiState.actionError) {
        if (uiState.actionError != null) showReportSheet = false
    }

    val reporterName = uiState.post?.ownerName?.takeIf { it.isNotBlank() } ?: uiState.post?.artistName ?: ""
    // Step 1 — reason picker stays a bottom sheet for art reports.
    if (showReportSheet && !uiState.reportSent) {
        ReportBottomSheet(
            artTitle = uiState.post?.title ?: "",
            profileName = reporterName,
            subjectLabel = "art",
            isReporting = uiState.isReporting,
            reportSent = false,
            onSubmitReport = viewModel::submitReport,
            onDismiss = {
                showReportSheet = false
                viewModel.onReportSheetClosed()
            },
        )
    }
    // Step 2 — the "Report sent" confirmation is a centered pop-up for art reports.
    if (showReportSheet && uiState.reportSent) {
        ReportSentDialog(
            artTitle = uiState.post?.title ?: "",
            profileName = reporterName,
            isBlocking = uiState.isBlocking,
            onBlockArt = { blockConfirm = "art" },
            onBlockUser = { blockConfirm = "user" },
            onDismiss = {
                showReportSheet = false
                viewModel.onReportSheetClosed()
            },
        )
    }

    // Always confirm before blocking (whether reached directly or after a report).
    blockConfirm?.let { kind ->
        if (kind == "art") {
            ConfirmActionDialog(
                title = "Are you sure want\nto block this art?",
                confirmLabel = "Block",
                confirmColor = DangerRed,
                iconRes = R.drawable.ic_block,
                isLoading = uiState.isBlocking,
                onConfirm = { viewModel.blockArt() },
                onDismiss = { blockConfirm = null },
            )
        } else {
            BlockConfirmDialog(
                name = uiState.post?.ownerName?.takeIf { it.isNotBlank() } ?: uiState.post?.artistName.orEmpty(),
                isLoading = uiState.isBlocking,
                onConfirm = { viewModel.blockUser() },
                onDismiss = { blockConfirm = null },
            )
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            itemLabel = "art",
            isDeleting = uiState.isDeleting,
            onConfirm = { viewModel.deleteArtwork() },
            onDismiss = { showDeleteDialog = false },
        )
    }

    // Both sheets hosted here under one state → only one is ever composed at a time.
    val artworkId = uiState.post?.id?.toIntOrNull()
    when (val sheet = activeSheet) {
        is ArtDetailSheet.AddToCuration -> if (artworkId != null) {
            AddToCurationSheet(
                source = CurationSource.Artwork(artworkId, uiState.post?.imageUrl),
                onDismiss = { activeSheet = ArtDetailSheet.None },
                onCreateNew = {
                    activeSheet = ArtDetailSheet.None
                    onNavigateToNewCuration()
                },
            )
        }
        is ArtDetailSheet.Share -> ShareSheet(
            target = sheet.target,
            onDismiss = { activeSheet = ArtDetailSheet.None },
        )
        ArtDetailSheet.None -> Unit
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                activeRoute = activeRoute,
                onNavigate  = { route ->
                    when (route) {
                        "home"          -> onNavigateHome()
                        "search"        -> onNavigateToSearch()
                        "create"        -> onNavigateToCreate()
                        "notifications" -> onNavigateToNotifications()
                        "profile"       -> onNavigateToProfile()
                    }
                },
            )
        },
        snackbarHost = {
            ErrorSnackbarHost(message = uiState.actionError, onShown = viewModel::onActionErrorShown)
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            when {
                uiState.isLoading -> ArtDetailShimmer(modifier = Modifier.fillMaxSize())

                uiState.post != null -> ArtDetailContent(
                    uiState = uiState,
                    onLike = viewModel::onLikeToggled,
                    onNavigateToDetail = onNavigateToDetail,
                    onAddToCuration = { activeSheet = ArtDetailSheet.AddToCuration },
                    onShare = { target -> activeSheet = ArtDetailSheet.Share(target) },
                    onOpenProfile = onOpenProfile,
                    onOpenArtistArts = onOpenArtistArts,
                    onInviteSheetOpened = viewModel::onInviteSheetOpened,
                    onSendInvite = viewModel::onSendInvite,
                    onInviteSheetClosed = viewModel::onInviteSheetClosed,
                    onLoadMoreSimilar = viewModel::loadMoreSimilar,
                    onRetryLoadMoreSimilar = viewModel::retryLoadMoreSimilar,
                    onRetrySimilarFirstPage = viewModel::retrySimilarFirstPage,
                    // Add-to-curation / Share / Like show on every artwork, including your own.
                    showActions = true,
                    // Send-message + Shop-Art only make sense on someone else's art — you can't
                    // message or buy from yourself.
                    showContactActions = !uiState.isOwn,
                    modifier = Modifier.fillMaxSize(),
                )

                else -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Couldn't load this artwork.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Floating top icons — overlaid so the artwork can scroll up behind the status bar; each
            // sits on a translucent scrim so it stays legible over both the background and the image.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScrimIconButton(R.drawable.ic_arrow_back, "Back", onClick = onBack)
                Spacer(Modifier.weight(1f))
                // Hold the action until ownership is known so we never flash Report on our own art.
                if (uiState.ownershipResolved) {
                    if (uiState.isOwn) {
                        // Edit/Delete only when opened from the user's own Profile ▸ Art section.
                        // NOT from Profile ▸ Liked (a liked artwork you own) nor any other flow
                        // (feed/search/curation) — those show no action at all.
                        if (uiState.isFromOwnArtSection) {
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                ScrimIconButton(R.drawable.ic_edit, "Edit") {
                                    viewModel.prepareEdit(); onEditArt()
                                }
                                ScrimIconButton(R.drawable.ic_delete, "Delete") {
                                    showDeleteDialog = true
                                }
                            }
                        }
                    } else {
                        ScrimIconButton(R.drawable.ic_report, "Report") { showReportSheet = true }
                    }
                }
            }
        }
    }
}

/** Top-bar icon button with a translucent dark scrim + white tint, legible over the artwork. */
@Composable
private fun ScrimIconButton(
    @androidx.annotation.DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f)),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Color.White,
        )
    }
}

/** First-page placeholder for the "More like this" rail — shimmer cards sized like [ArtworkCard],
 *  so a cold/slow similar load shows a skeleton instead of the section popping in from nothing. */
@Composable
private fun SimilarRailFirstLoad() {
    val d = LocalDimens.current
    val brush = rememberShimmerBrush()
    Row(
        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .width(d.artCardWidth)
                    .height(d.artCardHeight)
                    .clip(RoundedCornerShape(d.cardCornerRadius))
                    .background(brush),
            )
        }
    }
}

@Composable
private fun ArtDetailContent(
    uiState: ArtDetailUiState,
    onLike: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onAddToCuration: () -> Unit = {},
    onShare: (ShareTarget) -> Unit = {},
    onOpenProfile: (Int) -> Unit = {},
    onOpenArtistArts: (name: String, artistId: Int?) -> Unit = { _, _ -> },
    onInviteSheetOpened: () -> Unit = {},
    onSendInvite: (String) -> Unit = {},
    onInviteSheetClosed: () -> Unit = {},
    onLoadMoreSimilar: () -> Unit = {},
    onRetryLoadMoreSimilar: () -> Unit = {},
    onRetrySimilarFirstPage: () -> Unit = {},
    showActions: Boolean = true,
    showContactActions: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val d = LocalDimens.current
    val post = uiState.post ?: return
    // Reserve the status-bar height at the top so the artwork initially sits below the status bar;
    // as the user scrolls up this gap scrolls away and the image passes behind the status bar
    // (the floating back/action icons carry their own scrim to stay legible over it).
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var descExpanded by remember { mutableStateOf(false) }
    // While the hero is pinch-zoomed it must draw ABOVE the content below it (title, meta, "More
    // like this") instead of being clipped by them. The like row is kept above the hero so the
    // heart (and its pop animation) still shows on top of the image.
    var heroZoomed by remember { mutableStateOf(false) }
    // Reserve with the declared ratio, then snap to the image's TRUE ratio on load so the hero shows
    // the whole work with no letterbox bars (the declared value can be missing/wrong).
    var heroRatio by remember(post.id) { mutableStateOf(post.aspectRatio?.takeIf { it > 0f } ?: 1f) }
    var showSendSheet by remember { mutableStateOf(false) }
    var showShopDialog by remember { mutableStateOf(false) }

    // A send failure (e.g. 403 "Cannot message blocked user") surfaces in the parent's red error
    // banner — close this modal sheet so the banner isn't hidden behind it.
    LaunchedEffect(uiState.actionError) {
        if (uiState.actionError != null) showSendSheet = false
    }

    // Horizontal infinite scroll for the "More like this" rail — fetch the next page as it nears its
    // right edge. The ViewModel guards against duplicate/end/errored loads, so firing eagerly is cheap.
    val similarRowState = rememberLazyListState()
    LaunchedEffect(similarRowState) {
        snapshotFlow {
            val info = similarRowState.layoutInfo
            info.totalItemsCount > 0 &&
                (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - SIMILAR_PREFETCH
        }.distinctUntilChanged().collect { nearEnd -> if (nearEnd) onLoadMoreSimilar() }
    }

    if (showShopDialog) {
        ShopLinkDialog(
            artistName = post.artistName,
            shopUrl    = post.shopUrl,
            onDismiss  = { showShopDialog = false },
        )
    }

    if (showSendSheet) {
        LaunchedEffect(Unit) { onInviteSheetOpened() }
        SendMessageBottomSheet(
            artistName      = post.ownerName.ifBlank { post.artistName },
            artistRole      = post.artistRole,
            artistAvatarUrl = post.artistAvatarUrl,
            artworkTitle    = post.title,
            artworkImageUrl = post.imageUrl,
            invitationsLeft = uiState.invitationsLeft,
            isSending       = uiState.isSendingInvite,
            sent            = uiState.inviteSent,
            onSend          = onSendInvite,
            onDismiss       = { showSendSheet = false; onInviteSheetClosed() },
            mode            = uiState.sendMode,
            ready           = uiState.sendModeReady,
        )
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = statusBarTop),
    ) {

        // ── Hero image — shown whole at its natural aspect ratio; pinch / double-tap to zoom in
        //    place (single-finger drag still scrolls the page). No separate zoom button. ──
        item(key = "hero") {
            ZoomableImage(
                model = post.imageUrl,
                contentDescription = post.title,
                contentScale = ContentScale.Crop,
                onZoomedChange = { heroZoomed = it },
                onIntrinsicRatio = { heroRatio = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(heroRatio)
                    // Zoomed: lift the image above EVERYTHING (title/like/etc.). Unzoomed: drop below
                    // the title row so the like-button pop animation renders above the picture.
                    .zIndex(if (heroZoomed) 2f else 0f),
            )
        }

        // ── Title + action icons ───────────────────────────────────────
        item(key = "title") {
            Row(
                modifier = Modifier
                    // Above the hero when unzoomed (so the like pop shows over the picture); the
                    // zoomed hero (zIndex 2) still lifts above this row.
                    .zIndex(1f)
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    // Show the full title where it fits; ellipsis only guards extreme lengths.
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (showActions) {
                    Spacer(Modifier.width(Spacing.sm))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add_to),
                            contentDescription = "Add to collection",
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
                                size = Spacing.xxl,
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
            }
        }

        // ── Artist | Style | Shop Art ──────────────────────────────────
        item(key = "meta") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        // Tapping the artist name opens "Art by <artist>".
                        .then(
                            if (post.artistName.isNotBlank()) {
                                Modifier.clickable { onOpenArtistArts(post.artistName, post.artistId) }
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    Text(
                        text = "Artist",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = post.artistName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Medium / style — only shown when the artwork has one.
                if (post.medium.isNotBlank()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Medium",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = post.medium,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                // Shop Art — only when this artwork has a shop link.
                if (showContactActions && post.shopUrl.isNotBlank()) {
                    ShopArtButton(price = post.price, onClick = { showShopDialog = true })
                }
            }
        }

        // ── Dimensions (only when the artwork has a physical size) ─────
        post.dimensions?.let { dimensions ->
            item(key = "dimensions") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                ) {
                    Text(
                        text = "Size",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = dimensions,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }

        // ── Description ────────────────────────────────────────────────
        item(key = "desc") {
            // Whether the description actually needs the more/less toggle. Correct in both
            // collapsed and expanded states: while collapsed hasVisualOverflow reports the
            // truncation; while expanded lineCount reveals the true length. Resets if the
            // description changes (e.g. SWR refresh).
            var descOverflow by remember(post.description) { mutableStateOf(false) }
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
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (descOverflow) {
                        Text(
                            text = if (descExpanded) "less" else "more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { descExpanded = !descExpanded },
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = post.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = if (descExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { descOverflow = it.hasVisualOverflow || it.lineCount > 2 },
                    modifier = Modifier.animateContentSize(),
                )
            }
        }

        // ── Artist row + Send message ──────────────────────────────────
        item(key = "artist-row") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val artistId = post.ownerId
                val artistClick = Modifier.then(
                    if (artistId != null) Modifier.clickable { onOpenProfile(artistId) } else Modifier,
                )
                RinxAvatar(
                    url = post.artistAvatarUrl,
                    contentDescription = post.ownerName,
                    size = d.avatarSizeLg,
                    name = post.ownerName.ifBlank { post.artistName },
                    modifier = artistClick,
                )
                Spacer(Modifier.width(Spacing.sm))
                Column(modifier = Modifier.weight(1f).then(artistClick)) {
                    Text(
                        text = post.ownerName.ifBlank { post.artistName },
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
                    )
                }
                // "Send message" shows for other artists' art (incl. liked arts), never your own.
                if (showContactActions) {
                    Spacer(Modifier.width(Spacing.sm))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Spacing.sm))
                            .background(BrandPrimary)
                            .clickable { showSendSheet = true }
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Send message",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                        )
                    }
                }
            }
        }

        // ── More like this ──
        // Shown for non-Profile flows: a populated rail, a first-page shimmer while it loads, or a
        // Retry if the first page failed with nothing to show (so a transient blip is recoverable
        // instead of the section silently vanishing). From Profile the fetch is skipped, so all
        // three flags stay false/empty and the whole section is hidden — the intended clean preview.
        if (uiState.moreLikeThis.isNotEmpty() || uiState.similarFirstLoading || uiState.similarFirstError) {
            item(key = "more-header") {
                SectionHeader(title = "More like this")
            }
            when {
                uiState.moreLikeThis.isNotEmpty() -> item(key = "more-content") {
                    LazyRow(
                        state = similarRowState,
                        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        items(uiState.moreLikeThis, key = { it.id }) { artItem ->
                            ArtworkCard(
                                item = artItem,
                                onClick = { onNavigateToDetail(artItem.id) },
                            )
                        }
                        // Trailing rail footer: spinner while the next page loads, or a Retry chip on failure.
                        val railPaging = uiState.moreLikeThisPaging
                        if (railPaging.isLoadingMore) {
                            item(key = "more-loading") {
                                Box(
                                    // Match the card height so the spinner sits centered against the cards,
                                    // not at the top (a LazyRow item wraps content, so fillMaxHeight collapses).
                                    modifier = Modifier
                                        .height(d.artCardHeight)
                                        .padding(horizontal = Spacing.md),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        color = BrandPrimary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(Spacing.lg),
                                    )
                                }
                            }
                        } else if (railPaging.loadMoreError != null) {
                            item(key = "more-error") {
                                Box(
                                    modifier = Modifier
                                        .height(d.artCardHeight)
                                        .padding(horizontal = Spacing.md),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    OutlinedButton(onClick = onRetryLoadMoreSimilar) {
                                        Text(text = "Retry", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }
                // First page still loading with nothing to show yet → shimmer placeholder cards.
                uiState.similarFirstLoading -> item(key = "more-first-loading") {
                    SimilarRailFirstLoad()
                }
                // First page failed and the rail is empty → offer Retry.
                else -> item(key = "more-first-error") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(d.artCardHeight)
                            .padding(horizontal = Spacing.md),
                        contentAlignment = Alignment.Center,
                    ) {
                        OutlinedButton(onClick = onRetrySimilarFirstPage) {
                            Text(text = "Retry", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        item(key = "bottom-space") { Spacer(Modifier.height(Spacing.xxl)) }
    }
}

/** Which mutually-exclusive bottom sheet is open on the art-detail screen (only one at a time). */
private sealed interface ArtDetailSheet {
    data object None : ArtDetailSheet
    data object AddToCuration : ArtDetailSheet
    data class Share(val target: ShareTarget) : ArtDetailSheet
}
