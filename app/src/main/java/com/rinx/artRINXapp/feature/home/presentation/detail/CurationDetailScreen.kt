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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.DangerRed
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.profile.presentation.other.components.ConfirmActionDialog
import com.rinx.artRINXapp.feature.share.domain.model.ShareKind
import com.rinx.artRINXapp.feature.share.domain.model.ShareTarget
import com.rinx.artRINXapp.feature.share.presentation.ShareSheet
import com.rinx.artRINXapp.feature.upload.domain.model.CurationSource
import com.rinx.artRINXapp.feature.home.presentation.components.BottomNavBar
import com.rinx.artRINXapp.feature.home.presentation.components.CollectionCard
import com.rinx.artRINXapp.feature.home.presentation.components.CurationCardStack
import com.rinx.artRINXapp.feature.home.presentation.components.LikeButton
import com.rinx.artRINXapp.feature.home.presentation.components.ReportBottomSheet
import com.rinx.artRINXapp.feature.home.presentation.components.SectionHeader
import com.rinx.artRINXapp.feature.home.presentation.components.SendMessageBottomSheet
import com.rinx.artRINXapp.feature.home.presentation.components.AddToCurationSheet
import com.rinx.artRINXapp.feature.upload.presentation.components.DeleteConfirmDialog

@Composable
fun CurationDetailScreen(
    onBack: () -> Unit,
    onNavigateToCuration: (String) -> Unit = {},
    onNavigateHome: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNewCuration: () -> Unit = {},
    onEditCuration: () -> Unit = {},
    onOpenProfile: (Int) -> Unit = {},
    activeRoute: String = "home",
    viewModel: CurationDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showReportSheet by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }
    var showAddToCuration by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Pop back once the curation is deleted.
    LaunchedEffect(Unit) {
        viewModel.deleted.collect { onBack() }
    }

    // Close the sheet and pop back once the curation's author is blocked (toast survives the pop).
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
            artTitle = uiState.curation?.title ?: "",
            profileName = uiState.curation?.curatorName ?: "",
            subjectLabel = "curation",
            isReporting = uiState.isReporting,
            reportSent = uiState.reportSent,
            isBlocking = uiState.isBlocking,
            onSubmitReport = viewModel::submitReport,
            onBlockArt = null, // no curation-block API; only the curator can be blocked
            onBlockUser = { showBlockConfirm = true },
            onDismiss = {
                showReportSheet = false
                viewModel.onReportSheetClosed()
            },
        )
    }

    // Always confirm before blocking the curator (whether direct or after a report).
    if (showBlockConfirm) {
        ConfirmActionDialog(
            title = "Are you sure want\nto block \"${uiState.curation?.curatorName.orEmpty()}\"?",
            confirmLabel = "Block",
            confirmColor = DangerRed,
            iconRes = R.drawable.ic_block,
            isLoading = uiState.isBlocking,
            onConfirm = { viewModel.blockUser() },
            onDismiss = { showBlockConfirm = false },
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            itemLabel = "curation",
            isDeleting = uiState.isDeleting,
            onConfirm = { viewModel.deleteCuration() },
            onDismiss = { showDeleteDialog = false },
        )
    }

    val curationId = uiState.curation?.id?.toIntOrNull()
    if (showAddToCuration && curationId != null) {
        AddToCurationSheet(
            source = CurationSource.Curation(curationId),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            // ── Top bar — solid, NOT overlaid ─────────────────────────────
            // Card stack starts naturally below this, no overlap.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter            = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = "Back",
                        tint               = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (uiState.isOwn) {
                    IconButton(onClick = {
                        viewModel.prepareEdit()
                        onEditCuration()
                    }) {
                        Icon(
                            painter            = painterResource(R.drawable.ic_edit),
                            contentDescription = "Edit",
                            tint               = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            painter            = painterResource(R.drawable.ic_delete),
                            contentDescription = "Delete",
                            tint               = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                } else {
                    IconButton(onClick = { showReportSheet = true }) {
                        Icon(
                            painter            = painterResource(R.drawable.ic_report),
                            contentDescription = "Report",
                            tint               = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }

            // ── Scrollable content — card stack + metadata ─────────────────
            when {
                uiState.isLoading -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = BrandPrimary) }

                uiState.curation != null -> CurationDetailContent(
                    uiState              = uiState,
                    onLike               = viewModel::onLikeToggled,
                    onNavigateToCuration = onNavigateToCuration,
                    onAddToCuration      = { showAddToCuration = true },
                    onOpenProfile        = onOpenProfile,
                    onInviteSheetOpened  = viewModel::onInviteSheetOpened,
                    onSendInvite         = viewModel::onSendInvite,
                    onInviteSheetClosed  = viewModel::onInviteSheetClosed,
                    modifier             = Modifier.weight(1f),
                )

                else -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Couldn't load this curation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CurationDetailContent(
    uiState: CurationDetailUiState,
    onLike: () -> Unit,
    onNavigateToCuration: (String) -> Unit,
    onAddToCuration: () -> Unit = {},
    onOpenProfile: (Int) -> Unit = {},
    onInviteSheetOpened: () -> Unit = {},
    onSendInvite: (String) -> Unit = {},
    onInviteSheetClosed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val d        = LocalDimens.current
    val curation = uiState.curation ?: return
    var descExpanded by remember { mutableStateOf(true) }
    var showSendSheet by remember { mutableStateOf(false) }
    var currentArtworkIndex by remember { mutableIntStateOf(0) }
    var shareTarget by remember { mutableStateOf<ShareTarget?>(null) }

    shareTarget?.let { target ->
        ShareSheet(target = target, onDismiss = { shareTarget = null })
    }
    val currentArtworkUrl = curation.artworkUrls.getOrElse(currentArtworkIndex) {
        curation.artworkUrls.firstOrNull() ?: ""
    }

    if (showSendSheet) {
        LaunchedEffect(Unit) { onInviteSheetOpened() }
        SendMessageBottomSheet(
            artistName      = curation.curatorName,
            artistRole      = "Artist",
            artistAvatarUrl = curation.curatorAvatarUrl,
            artworkTitle    = curation.title,
            artworkImageUrl = currentArtworkUrl,
            invitationsLeft = uiState.invitationsLeft,
            isSending       = uiState.isSendingInvite,
            sent            = uiState.inviteSent,
            onSend          = onSendInvite,
            onDismiss       = { showSendSheet = false; onInviteSheetClosed() },
            mode            = uiState.sendMode,
            ready           = uiState.sendModeReady,
        )
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {

        // ── Card stack — directly below the top bar, clean start ──────────
        item(key = "card-stack") {
            CurationCardStack(
                artworks           = curation.artworkUrls,
                modifier           = Modifier.fillMaxWidth(),
                onTopIndexChanged  = { currentArtworkIndex = it },
            )
        }

        // ── Title + action icons ───────────────────────────────────────────
        item(key = "title") {
            Row(
                modifier           = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment  = Alignment.CenterVertically,
            ) {
                Text(
                    text     = curation.title,
                    style    = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color    = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(Spacing.sm))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment     = Alignment.Top,
                ) {
                    Icon(
                        painter            = painterResource(R.drawable.ic_add_to),
                        contentDescription = "Add to curation",
                        tint               = MaterialTheme.colorScheme.onSurface,
                        modifier           = Modifier
                            .size(Spacing.xxl)
                            .clickable { onAddToCuration() },
                    )
                    Icon(
                        painter            = painterResource(R.drawable.ic_send),
                        contentDescription = "Share",
                        tint               = MaterialTheme.colorScheme.onSurface,
                        modifier           = Modifier
                            .size(Spacing.xxl)
                            .clickable {
                                shareTarget = ShareTarget(
                                    kind = ShareKind.CURATION,
                                    id = curation.id,
                                    title = curation.title,
                                    subtitle = "by ${curation.curatorName}",
                                    imageUrl = curation.artworkUrls.firstOrNull(),
                                )
                            },
                    )
                    // Heart + count: count centered exactly below the heart.
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LikeButton(
                            isLiked = uiState.isLiked,
                            onClick = onLike,
                            size    = Spacing.xxl,
                        )
                        if (uiState.likeCount > 0) {
                            Text(
                                text     = uiState.likeCount.toString(),
                                style    = MaterialTheme.typography.labelSmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = Spacing.xs),
                            )
                        }
                    }
                }
            }
        }

        // ── Styles ─────────────────────────────────────────────────────────
        item(key = "styles") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            ) {
                Text(
                    text  = "Styles",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text       = curation.styles,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        // ── Description ────────────────────────────────────────────────────
        item(key = "desc") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text       = "Description",
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier   = Modifier.weight(1f),
                    )
                    Text(
                        text     = if (descExpanded) "less" else "more",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { descExpanded = !descExpanded },
                    )
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text     = curation.description,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onBackground,
                    maxLines = if (descExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.animateContentSize(),
                )
            }
        }

        // ── Curator row + Send message ──────────────────────────────────────
        item(key = "curator-row") {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val authorId = curation.authorId
                val authorClick = Modifier.then(
                    if (authorId != null) Modifier.clickable { onOpenProfile(authorId) } else Modifier,
                )
                Box(
                    modifier         = Modifier
                        .size(d.avatarSizeLg)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .then(authorClick),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!curation.curatorAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model              = curation.curatorAvatarUrl,
                            contentDescription = curation.curatorName,
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize().clip(CircleShape),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint        = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier    = Modifier.size(d.avatarSizeLg * 0.6f),
                        )
                    }
                }
                Spacer(Modifier.width(Spacing.sm))
                Column(modifier = Modifier.weight(1f).then(authorClick)) {
                    Text(
                        text       = curation.curatorName,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onBackground,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                    )
                    Text(
                        text  = "Artist",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // No "Send message" on your own curation.
                if (!uiState.isOwn) {
                    Spacer(Modifier.width(Spacing.sm))
                    Box(
                        modifier         = Modifier
                            .clip(RoundedCornerShape(Spacing.sm))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showSendSheet = true }
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text  = "Send message",
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
                    contentPadding        = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    items(uiState.moreLikeThis, key = { it.id }) { item ->
                        CollectionCard(
                            item    = item,
                            onClick = { onNavigateToCuration(item.id) },
                        )
                    }
                }
            }
        }

        item(key = "bottom-space") { Spacer(Modifier.height(Spacing.xxl)) }
    }
}
