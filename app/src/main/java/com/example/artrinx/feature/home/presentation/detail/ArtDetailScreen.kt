package com.example.artrinx.feature.home.presentation.detail

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.artrinx.R
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.core.util.shareArtwork
import com.example.artrinx.feature.upload.domain.model.CurationSource
import com.example.artrinx.feature.home.presentation.components.AddToCurationSheet
import com.example.artrinx.feature.home.presentation.components.ArtworkCard
import com.example.artrinx.feature.home.presentation.components.BottomNavBar
import com.example.artrinx.feature.home.presentation.components.LikeButton
import com.example.artrinx.feature.home.presentation.components.ReportBottomSheet
import com.example.artrinx.feature.home.presentation.components.SectionHeader
import com.example.artrinx.feature.home.presentation.components.SendMessageBottomSheet
import com.example.artrinx.feature.home.presentation.components.ShopLinkDialog
import com.example.artrinx.feature.upload.presentation.components.DeleteConfirmDialog

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
    activeRoute: String = "home",
    viewModel: ArtDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showReportSheet by remember { mutableStateOf(false) }
    var showAddToCuration by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Pop back once the artwork is deleted.
    LaunchedEffect(Unit) {
        viewModel.deleted.collect { onBack() }
    }

    if (showReportSheet) {
        ReportBottomSheet(
            artTitle    = uiState.post?.title ?: "",
            profileName = uiState.post?.artistName ?: "",
            onDismiss   = { showReportSheet = false },
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
                    // Your OWN art → read-only (just the details). Anyone else's art (incl. liked
                    // arts in the Profile tab) keeps all the actions + Send message.
                    showActions = !uiState.isOwn,
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
                if (uiState.isOwn) {
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

@Composable
private fun ArtDetailContent(
    uiState: ArtDetailUiState,
    onLike: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onAddToCuration: () -> Unit = {},
    showActions: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val d = LocalDimens.current
    val context = LocalContext.current
    val post = uiState.post ?: return
    var descExpanded by remember { mutableStateOf(false) }
    var showSendSheet by remember { mutableStateOf(false) }
    var showShopDialog by remember { mutableStateOf(false) }

    if (showShopDialog) {
        ShopLinkDialog(
            artistName = post.artistName,
            shopUrl    = post.shopUrl,
            onDismiss  = { showShopDialog = false },
        )
    }

    if (showSendSheet) {
        SendMessageBottomSheet(
            artistName      = post.artistName,
            artistRole      = post.artistRole,
            artistAvatarUrl = post.artistAvatarUrl,
            artworkTitle    = post.title,
            artworkImageUrl = post.imageUrl,
            onDismiss       = { showSendSheet = false },
        )
    }

    LazyColumn(modifier = modifier) {

        // ── Hero image — full bleed from top of screen ─────────────────
        item(key = "hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(d.artDetailImageHeight),
            ) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = post.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
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
                    maxLines = 2,
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
                                    context.shareArtwork(
                                        title = post.title,
                                        artistName = post.artistName,
                                        description = post.description,
                                        link = post.shopUrl.ifBlank { null },
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Artist",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = post.artistName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
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
                if (showActions) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Spacing.sm))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showShopDialog = true }
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Shop Art",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
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

        // ── Artist row + Send message ──────────────────────────────────
        item(key = "artist-row") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(d.avatarSizeLg)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!post.artistAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = post.artistAvatarUrl,
                            contentDescription = post.artistName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(d.avatarSizeLg * 0.6f),
                        )
                    }
                }
                Spacer(Modifier.width(Spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.artistName,
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
                if (showActions) {
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
