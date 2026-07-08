package com.rinx.artRINXapp.feature.upload.presentation.newart

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.home.presentation.components.FullWidthArtImage
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar
import com.rinx.artRINXapp.feature.upload.domain.model.PrivacyOption
import com.rinx.artRINXapp.feature.upload.presentation.components.CreationStatusOverlay
import androidx.compose.animation.animateColorAsState
import com.rinx.artRINXapp.core.theme.DarkCardSurface

@Composable
fun NewArtPreviewScreen(
    viewModel: NewArtViewModel,
    onBack: () -> Unit,
    onUploadStarted: (isPrivate: Boolean) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val d = LocalDimens.current
    var descExpanded by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // ── Top bar ───────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), "Back",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                text       = "New Art",
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground,
                modifier   = Modifier.weight(1f),
                textAlign  = TextAlign.Center,
            )
            // Upload button — always enabled from preview
            val bgColor by animateColorAsState(BrandPrimary, label = "previewUpload")
            Box(
                modifier         = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(bgColor)
                    .clickable {
                        // Public → Home (progress row). Private → stay; overlay shows.
                        if (viewModel.onUpload() && state.privacy != PrivacyOption.PRIVATE) {
                            onUploadStarted(false)
                        }
                    }
                    .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                Text("Upload",
                    style      = MaterialTheme.typography.labelLarge,
                    color      = Color.White,
                    fontWeight = FontWeight.SemiBold)
            }
        }

        // ── Scrollable preview content ─────────────────────────────────────
        LazyColumn(
            modifier       = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = Spacing.xxl),
        ) {
            // Hero photo — full width at the image's true aspect ratio, exactly how the detail
            // screen renders it (no fixed-height crop).
            item(key = "photo") {
                FullWidthArtImage(
                    model = state.imageUri,
                    contentDescription = "Art preview",
                    declaredRatio = null,
                )
            }

            // Title + action icons
            item(key = "title") {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text       = state.title.ifEmpty { "Title" },
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onBackground,
                        maxLines   = 4,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        Icon(painterResource(R.drawable.ic_add_to), "Save",
                            tint     = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(Spacing.xxl))
                        Icon(painterResource(R.drawable.ic_send), "Share",
                            tint     = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(Spacing.xxl))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.FavoriteBorder, "Like",
                                tint     = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(Spacing.xxl))
                            Text("0", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item(key = "divider1") {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Spacing.md),
                    color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }

            // Artist | Medium | Shop Art
            item(key = "meta") {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Artist",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text       = state.selectedArtist?.displayName ?: "—",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = BrandPrimary,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Medium",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text       = state.selectedMedium ?: "—",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onBackground,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                        )
                    }
                    Box(
                        modifier         = Modifier
                            .clip(RoundedCornerShape(Spacing.sm))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Shop Art",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            item(key = "divider2") {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Spacing.md),
                    color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }

            // Dimensions — only when a physical size was entered (mirrors the detail screen).
            dimensionsDisplay(state.sizeHeightCm, state.sizeWidthCm, state.sizeUnit)?.let { dimensions ->
                item(key = "dimensions") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    ) {
                        Text(
                            text       = "Dimensions",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text  = dimensions,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }

            // Description
            item(key = "desc") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Description",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier   = Modifier.weight(1f))
                        Text(if (descExpanded) "less" else "more",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { descExpanded = !descExpanded })
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text     = state.description.ifEmpty { "No description added." },
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.onBackground,
                        maxLines = if (descExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.animateContentSize(),
                    )
                }
            }

            item(key = "divider3") {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Spacing.md),
                    color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }

            // Uploader card — this is the posting user's profile (NOT the credited artist).
            item(key = "uploader-card") {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RinxAvatar(
                        url                = state.selfArtist?.avatarUrl,
                        contentDescription = state.selfArtist?.displayName,
                        size               = d.avatarSizeLg,
                        name               = state.selfArtist?.displayName,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Column {
                        Text(
                            text       = state.selfArtist?.displayName ?: "You",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onBackground,
                        )
                        Text("Artist",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

        state.creationStatus?.let { status ->
            CreationStatusOverlay(
                status = status,
                label = "Artwork",
                error = state.creationError,
                onDone = { viewModel.onCreationDone(); onUploadStarted(false) },
                onRetry = { viewModel.onRetryCreation() },
                onDismiss = { viewModel.onCreationDone() },
            )
        }
    }
}

/**
 * Human-readable size string from the raw form input (e.g. "60 × 90 cm"), or null when neither
 * dimension is set. Matches the detail screen's `toDimensionsDisplay` formatting.
 */
private fun dimensionsDisplay(height: String, width: String, unit: String): String? {
    val h = height.trim().toDoubleOrNull()
    val w = width.trim().toDoubleOrNull()
    if (h == null && w == null) return null
    val u = unit.trim().ifBlank { "cm" }
    fun fmt(n: Double) = if (n % 1.0 == 0.0) n.toLong().toString() else n.toString()
    return when {
        h != null && w != null -> "${fmt(h)} × ${fmt(w)} $u"
        h != null -> "Height: ${fmt(h)} $u"
        else -> "Width: ${fmt(w!!)} $u"
    }
}
