package com.rinx.artRINXapp.feature.home.presentation.detail

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.feature.home.presentation.detail.components.ZoomableImage
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.DangerRed
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar
import com.rinx.artRINXapp.feature.profile.presentation.other.components.ConfirmActionDialog
import com.rinx.artRINXapp.feature.share.domain.model.ShareKind
import com.rinx.artRINXapp.feature.share.domain.model.ShareTarget
import com.rinx.artRINXapp.feature.share.presentation.ShareSheet
import com.rinx.artRINXapp.feature.upload.domain.model.CurationSource
import com.rinx.artRINXapp.feature.home.presentation.components.AddToCurationSheet
import com.rinx.artRINXapp.feature.home.presentation.components.ArtworkCard
import com.rinx.artRINXapp.feature.home.presentation.components.BottomNavBar
import com.rinx.artRINXapp.feature.home.presentation.components.ShopArtButton
import com.rinx.artRINXapp.feature.home.presentation.components.LikeButton
import com.rinx.artRINXapp.feature.home.presentation.components.ReportBottomSheet
import com.rinx.artRINXapp.feature.home.presentation.components.SectionHeader
import com.rinx.artRINXapp.feature.home.presentation.components.SendMessageBottomSheet
import com.rinx.artRINXapp.feature.home.presentation.components.ShopLinkDialog
import com.rinx.artRINXapp.feature.upload.presentation.components.DeleteConfirmDialog

@Composable
fun ArtDetailScreen(
    onBack: () -> Unit,
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
    var showAddToCuration by remember { mutableStateOf(false) }
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

    // Close the sheet and pop back once the art/user is blocked (toast survives the pop).
    LaunchedEffect(Unit) {
        viewModel.blocked.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            showReportSheet = false
            onBack()
        }
    }

    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onActionErrorShown()
        }
    }

    if (showReportSheet) {
        ReportBottomSheet(
            artTitle = uiState.post?.title ?: "",
            profileName = uiState.post?.ownerName?.takeIf { it.isNotBlank() } ?: uiState.post?.artistName ?: "",
            isReporting = uiState.isReporting,
            reportSent = uiState.reportSent,
            isBlocking = uiState.isBlocking,
            onSubmitReport = viewModel::submitReport,
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
        ConfirmActionDialog(
            title = if (kind == "art") "Are you sure want\nto block this art?"
                    else "Are you sure want\nto block \"${uiState.post?.ownerName?.takeIf { it.isNotBlank() } ?: uiState.post?.artistName.orEmpty()}\"?",
            confirmLabel = "Block",
            confirmColor = DangerRed,
            iconRes = R.drawable.ic_block,
            isLoading = uiState.isBlocking,
            onConfirm = { if (kind == "art") viewModel.blockArt() else viewModel.blockUser() },
            onDismiss = { blockConfirm = null },
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            itemLabel = "art",
            isDeleting = uiState.isDeleting,
            onConfirm = { viewModel.deleteArtwork() },
            onDismiss = { showDeleteDialog = false },
        )
    }

    val artworkId = uiState.post?.id?.toIntOrNull()
    if (showAddToCuration && artworkId != null) {
        AddToCurationSheet(
            source = CurationSource.Artwork(artworkId, uiState.post?.imageUrl),
            onDismiss = { showAddToCuration = false },
            onCreateNew = {
                showAddToCuration = false
                onNavigateToNewCuration()
            },
        )
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
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            // Scrollable content — LazyColumn starts at y=0 (behind status bar)
            when {
                uiState.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = BrandPrimary) }

                uiState.post != null -> ArtDetailContent(
                    uiState = uiState,
                    onLike = viewModel::onLikeToggled,
                    onNavigateToDetail = onNavigateToDetail,
                    onAddToCuration = { showAddToCuration = true },
                    onOpenProfile = onOpenProfile,
                    onOpenArtistArts = onOpenArtistArts,
                    onInviteSheetOpened = viewModel::onInviteSheetOpened,
                    onSendInvite = viewModel::onSendInvite,
                    onInviteSheetClosed = viewModel::onInviteSheetClosed,
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

            // Back / info buttons overlaid over the hero image — always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }
                Spacer(Modifier.weight(1f))
                // Hold the action until ownership is known so we never flash Report on our own art.
                if (uiState.ownershipResolved) {
                if (uiState.isOwn) {
                    // Edit/Delete only when opened from the user's own Profile tab. For own art opened
                    // from any other flow (feed/search/curation/notifications) show no action at all.
                    if (uiState.isFromProfile) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        IconButton(
                            onClick = {
                                viewModel.prepareEdit()
                                onEditArt()
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f)),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_edit),
                                contentDescription = "Edit",
                                tint = Color.White,
                            )
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f)),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = "Delete",
                                tint = Color.White,
                            )
                        }
                    }
                    }
                } else {
                    IconButton(
                        onClick = { showReportSheet = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_report),
                            contentDescription = "Report",
                            tint = Color.White,
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun ArtDetailContent(
    uiState: ArtDetailUiState,
    onLike: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onAddToCuration: () -> Unit = {},
    onOpenProfile: (Int) -> Unit = {},
    onOpenArtistArts: (name: String, artistId: Int?) -> Unit = { _, _ -> },
    onInviteSheetOpened: () -> Unit = {},
    onSendInvite: (String) -> Unit = {},
    onInviteSheetClosed: () -> Unit = {},
    showActions: Boolean = true,
    showContactActions: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val d = LocalDimens.current
    val post = uiState.post ?: return
    // Lift the hero above sibling items only while the image is actually zoomed, so a zoomed
    // image overlays the content below it — yet when not zoomed the like-heart pop (which hops
    // upward into the hero's region) still draws above the image instead of behind it.
    var imageZoomed by remember { mutableStateOf(false) }
    var descExpanded by remember { mutableStateOf(false) }
    var showSendSheet by remember { mutableStateOf(false) }
    var showShopDialog by remember { mutableStateOf(false) }
    var shareTarget by remember { mutableStateOf<ShareTarget?>(null) }

    shareTarget?.let { target ->
        ShareSheet(target = target, onDismiss = { shareTarget = null })
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

    LazyColumn(modifier = modifier) {

        // ── Hero image — full bleed from top of screen ─────────────────
        item(key = "hero") {
            Box(
                modifier = Modifier
                    // Above detail items only while zoomed; otherwise the like-heart pop wins.
                    .zIndex(if (imageZoomed) 1f else 0f)
                    .fillMaxWidth()
                    .height(d.artDetailImageHeight),
            ) {
                ZoomableImage(
                    model = post.imageUrl,
                    contentDescription = post.title,
                    onZoomedChange = { imageZoomed = it },
                    modifier = Modifier.fillMaxSize(),
                )
                // Subtle top gradient so back button stays readable
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(d.artDetailImageHeight * 0.35f)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent),
                            ),
                        ),
                )
            }
        }

        // ── Title + action icons ───────────────────────────────────────
        item(key = "title") {
            Row(
                modifier = Modifier
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
                                    shareTarget = ShareTarget(
                                        kind = ShareKind.ARTWORK,
                                        id = post.id,
                                        title = post.title,
                                        subtitle = "by ${post.artistName}",
                                        imageUrl = post.imageUrl,
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
                        style = MaterialTheme.typography.labelSmall,
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
                            text = "Style",
                            style = MaterialTheme.typography.labelSmall,
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

        // ── Description ────────────────────────────────────────────────
        item(key = "desc") {
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
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (descExpanded) "less" else "more",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { descExpanded = !descExpanded },
                    )
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = post.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = if (descExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.animateContentSize(),
                )
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
                        text = "Dimensions",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
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
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showSendSheet = true }
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Send message",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // ── More like this (hidden when there are no suggestions, e.g. from Profile) ──
        if (uiState.moreLikeThis.isNotEmpty()) {
            item(key = "more-header") {
                SectionHeader(title = "More like this")
            }
            item(key = "more-content") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    items(uiState.moreLikeThis, key = { it.id }) { artItem ->
                        ArtworkCard(
                            item = artItem,
                            onClick = { onNavigateToDetail(artItem.id) },
                        )
                    }
                }
            }
        }

        item(key = "bottom-space") { Spacer(Modifier.height(Spacing.xxl)) }
    }
}
