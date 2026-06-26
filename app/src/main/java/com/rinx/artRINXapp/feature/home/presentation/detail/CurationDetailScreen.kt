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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar
import com.rinx.artRINXapp.feature.share.domain.model.ShareKind
import com.rinx.artRINXapp.feature.share.domain.model.ShareTarget
import com.rinx.artRINXapp.feature.share.presentation.ShareSheet
import com.rinx.artRINXapp.feature.upload.domain.model.CurationSource
import com.rinx.artRINXapp.feature.home.presentation.components.BottomNavBar
import com.rinx.artRINXapp.feature.home.presentation.components.shimmer.CurationDetailShimmer
import com.rinx.artRINXapp.feature.home.presentation.components.CollectionCard
import com.rinx.artRINXapp.feature.home.presentation.components.CurationCardStack
import com.rinx.artRINXapp.feature.home.presentation.components.EmptyCurationStack
import com.rinx.artRINXapp.feature.home.presentation.components.LikeButton
import com.rinx.artRINXapp.feature.home.presentation.components.SectionHeader
import com.rinx.artRINXapp.feature.home.presentation.components.SendMessageBottomSheet
import com.rinx.artRINXapp.feature.home.presentation.components.AddToCurationSheet
import com.rinx.artRINXapp.feature.upload.presentation.components.DeleteConfirmDialog

@Composable
fun CurationDetailScreen(
    onBack: () -> Unit,
    onNavigateToCuration: (String) -> Unit = {},
    onNavigateToArtDetail: (String) -> Unit = {},
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
    var showAddToCuration by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Pop back once the curation is deleted.
    LaunchedEffect(Unit) {
        viewModel.deleted.collect { onBack() }
    }

    // Curation no longer exists server-side (404) → toast + pop instead of showing stale cached detail.
    LaunchedEffect(Unit) {
        viewModel.gone.collect {
            Toast.makeText(context, "This collection is no longer available.", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    // Pop back once the curation's author is blocked (toast survives the pop).
    LaunchedEffect(Unit) {
        viewModel.blocked.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onActionErrorShown()
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            itemLabel = "collection",
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
                }
                // Non-owners have no actions here: report-collection is intentionally omitted
                // (parity with iOS, which has no report option for collections).
            }

            // ── Scrollable content — card stack + metadata ─────────────────
            when {
                uiState.isLoading -> CurationDetailShimmer(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )

                uiState.curation != null -> CurationDetailContent(
                    uiState              = uiState,
                    onLike               = viewModel::onLikeToggled,
                    onNavigateToArtDetail = onNavigateToArtDetail,
                    onAddToCuration      = { showAddToCuration = true },
                    modifier             = Modifier.weight(1f),
                )

                else -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Couldn't load this collection.",
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
    onNavigateToArtDetail: (String) -> Unit = {},
    onAddToCuration: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val curation = uiState.curation ?: return
    val d = LocalDimens.current
    var descExpanded by remember { mutableStateOf(true) }
    var shareTarget by remember { mutableStateOf<ShareTarget?>(null) }

    shareTarget?.let { target ->
        ShareSheet(target = target, onDismiss = { shareTarget = null })
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {

        // ── Card stack — directly below the top bar, clean start ──────────
        // Empty curation → show a placeholder message in the deck's place (the stack itself
        // renders nothing when empty), so the screen never looks broken/blank.
        item(key = "card-stack") {
            if (curation.artworkUrls.isEmpty()) {
                EmptyCurationStack(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(d.artDetailImageHeight)
                        .padding(horizontal = Spacing.md),
                )
            } else {
                CurationCardStack(
                    artworks           = curation.artworkUrls,
                    modifier           = Modifier.fillMaxWidth(),
                    // Navigable everywhere EXCEPT my own collection opened from my Profile tab.
                    onCardClick        = { index ->
                        if (uiState.artworksNavigable) {
                            curation.artworkIds.getOrNull(index)
                                ?.takeIf { it.isNotBlank() }
                                ?.let(onNavigateToArtDetail)
                        }
                    },
                )
            }
        }

        // ── Title + action icons ───────────────────────────────────────────
        item(key = "title") {
            Row(
                modifier           = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment  = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text     = curation.title,
                        style    = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color    = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // Collection owner's name, just below the title.
                    if (curation.curatorName.isNotBlank()) {
                        Text(
                            text     = curation.curatorName,
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(Spacing.sm))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment     = Alignment.Top,
                ) {
                    Icon(
                        painter            = painterResource(R.drawable.ic_add_to),
                        contentDescription = "Add to collection",
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
        // Hidden when the curation has no real style info (e.g. empty curation) — no fabricated text.
        if (curation.styles.isNotBlank()) {
            item(key = "styles") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                ) {
                    Text(
                        text  = "Mediums",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
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
        }

        // ── Description ────────────────────────────────────────────────────
        // Hidden when the API gives no description — no fabricated placeholder sentence.
        if (curation.description.isNotBlank()) {
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
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
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
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onBackground,
                    maxLines = if (descExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.animateContentSize(),
                )
                }
            }
        }

        // Curator profile row, "Send message", and "More like this" intentionally omitted —
        // the curation detail shows only collection-related data.

        item(key = "bottom-space") { Spacer(Modifier.height(Spacing.xxl)) }
    }
}
