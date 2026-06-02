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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.artrinx.R
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.home.presentation.components.BottomNavBar
import com.example.artrinx.feature.home.presentation.components.CollectionCard
import com.example.artrinx.feature.home.presentation.components.CurationCardStack
import com.example.artrinx.feature.home.presentation.components.ReportBottomSheet
import com.example.artrinx.feature.home.presentation.components.SectionHeader
import com.example.artrinx.feature.home.presentation.components.SendMessageBottomSheet

@Composable
fun CurationDetailScreen(
    onBack: () -> Unit,
    onNavigateToCuration: (String) -> Unit = {},
    onNavigateHome: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    activeRoute: String = "home",
    viewModel: CurationDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var showReportSheet by remember { mutableStateOf(false) }

    if (showReportSheet) {
        ReportBottomSheet(
            artTitle    = uiState.curation?.title ?: "",
            profileName = uiState.curation?.curatorName ?: "",
            onDismiss   = { showReportSheet = false },
        )
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                activeRoute = activeRoute,
                onNavigate  = { route ->
                    when (route) {
                        "home"    -> onNavigateHome()
                        "search"  -> onNavigateToSearch()
                        "create"  -> onNavigateToCreate()
                        "profile" -> onNavigateToProfile()
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
                IconButton(onClick = { showReportSheet = true }) {
                    Icon(
                        painter            = painterResource(R.drawable.ic_report),
                        contentDescription = "Report",
                        tint               = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            // ── Scrollable content — card stack + metadata ─────────────────
            CurationDetailContent(
                uiState              = uiState,
                onLike               = viewModel::onLikeToggled,
                onBookmark           = viewModel::onBookmarkToggled,
                onNavigateToCuration = onNavigateToCuration,
                modifier             = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CurationDetailContent(
    uiState: CurationDetailUiState,
    onLike: () -> Unit,
    onBookmark: () -> Unit,
    onNavigateToCuration: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d        = LocalDimens.current
    val curation = uiState.curation ?: return
    var descExpanded by remember { mutableStateOf(true) }
    var showSendSheet by remember { mutableStateOf(false) }
    var currentArtworkIndex by remember { mutableIntStateOf(0) }
    val currentArtworkRes = curation.artworkRes.getOrElse(currentArtworkIndex) { curation.artworkRes.first() }

    if (showSendSheet) {
        SendMessageBottomSheet(
            artistName      = curation.curatorName,
            artistRole      = "Artist",
            artistAvatarRes = curation.curatorAvatarRes,
            artworkTitle    = curation.title,
            artworkImageRes = currentArtworkRes,
            onDismiss       = { showSendSheet = false },
        )
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {

        // ── Card stack — directly below the top bar, clean start ──────────
        item(key = "card-stack") {
            CurationCardStack(
                artworks           = curation.artworkRes,
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
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter            = painterResource(R.drawable.ic_add_to),
                            contentDescription = "Save",
                            tint               = if (uiState.isBookmarked) BrandPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier           = Modifier.size(Spacing.xxl).clickable { onBookmark() },
                        )
                        Icon(
                            painter            = painterResource(R.drawable.ic_send),
                            contentDescription = "Share",
                            tint               = MaterialTheme.colorScheme.onSurface,
                            modifier           = Modifier.size(Spacing.xxl),
                        )
                        Icon(
                            painter            = painterResource(R.drawable.ic_like),
                            contentDescription = "Like",
                            tint               = if (uiState.isLiked) BrandPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier           = Modifier.size(Spacing.xxl).clickable { onLike() },
                        )
                    }
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
                Box(
                    modifier         = Modifier
                        .size(d.avatarSizeLg)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (curation.curatorAvatarRes != null) {
                        AsyncImage(
                            model              = curation.curatorAvatarRes,
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
                Column(modifier = Modifier.weight(1f)) {
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

        // ── More like this ──────────────────────────────────────────────────
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

        item(key = "bottom-space") { Spacer(Modifier.height(Spacing.xxl)) }
    }
}
