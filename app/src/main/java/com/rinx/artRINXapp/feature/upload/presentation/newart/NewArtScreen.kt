package com.rinx.artRINXapp.feature.upload.presentation.newart

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.ErrorDark
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.upload.domain.model.PrivacyOption
import com.rinx.artRINXapp.feature.upload.domain.model.ShopLinkVisibility
import com.rinx.artRINXapp.feature.upload.presentation.components.CreationStatusOverlay
import com.rinx.artRINXapp.feature.upload.presentation.newart.components.MediumPickerSheet
import com.rinx.artRINXapp.feature.upload.presentation.newart.components.PrivacyPickerSheet

// Shop link gradient — dark teal on left → near-black on right (always, both themes)
private val ShopLinkGradient = Brush.horizontalGradient(
    listOf(Color(0xFF1D4F68), Color(0xFF060E12))
)

@Composable
fun NewArtScreen(
    imageUri: Uri?,
    onBack: () -> Unit,
    onNavigateToArtist: () -> Unit,
    onNavigateToTags: () -> Unit,
    onUploadStarted: (isPrivate: Boolean) -> Unit = {},
    onNavigateToPreview: () -> Unit = {},
    onEditDone: () -> Unit = onBack,
    viewModel: NewArtViewModel = hiltViewModel(),
) {
    val state        by viewModel.state.collectAsState()
    val d            = LocalDimens.current
    val focusManager = LocalFocusManager.current
    val context      = LocalContext.current

    LaunchedEffect(imageUri) {
        if (imageUri != null) viewModel.onImageSet(imageUri)
    }

    if (state.showMediumPicker) {
        MediumPickerSheet(
            mediums          = state.mediums,
            selectedMediumId = state.selectedMediumId,
            onMediumSelected = viewModel::onMediumSelected,
            onDismiss        = viewModel::onDismissMediumPicker,
        )
    }
    if (state.showPrivacyPicker) {
        PrivacyPickerSheet(
            selected          = state.privacy,
            onPrivacySelected = viewModel::onPrivacySelected,
            onDismiss         = viewModel::onDismissPrivacyPicker,
        )
    }

    Scaffold(contentWindowInsets = WindowInsets(0)) { _ ->
      Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                // Keep content (incl. the bottom Preview/Upload button) clear of the system
                // 3-button nav bar. Consumed here so the inner imePadding doesn't double-count it.
                .navigationBarsPadding()
                // Dismiss keyboard when tapping anywhere not interactive
                .pointerInput(Unit) {
                    detectTapGestures { focusManager.clearFocus() }
                },
        ) {
            // ── FIXED header ──────────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.xs, end = Spacing.md,
                             top = Spacing.xs, bottom = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), "Back",
                        tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    text       = if (state.isEditing) "Edit Art" else "New Art",
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onBackground,
                    modifier   = Modifier.weight(1f),
                    textAlign  = TextAlign.Center,
                )
                UploadButton(
                    enabled     = state.isValid,
                    isUploading = state.isUploading,
                    label       = if (state.isEditing) "Save" else "Upload",
                    onClick     = {
                        if (state.isEditing) {
                            // Edit → stay; overlay shows save progress, pops back on done.
                            viewModel.onSaveEdit()
                        } else if (viewModel.onUpload() && state.privacy != PrivacyOption.PRIVATE) {
                            // Public → navigate to Home (progress row). Private → stay; overlay shows.
                            onUploadStarted(false)
                        }
                    },
                )
            }

            // ── SCROLLABLE content — imePadding keeps it above keyboard ───
            LazyColumn(
                modifier       = Modifier
                    .weight(1f)
                    .imePadding(),        // content shifts up when keyboard opens
                contentPadding = PaddingValues(bottom = Spacing.xxl),
            ) {
                // Photo — full width, no horizontal margins
                item(key = "photo") {
                    AsyncImage(
                        model              = state.imageUri ?: state.imageUrl,
                        contentDescription = "Selected photo",
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier
                            .fillMaxWidth()
                            .height(d.uploadImageHeight)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }

                // Title
                item(key = "title") {
                    Spacer(Modifier.height(Spacing.md))
                    TitleField(
                        title        = state.title,
                        hasError     = state.isTitleError,
                        onChange     = viewModel::onTitleChange,
                    )
                }

                // Description
                item(key = "desc") {
                    Spacer(Modifier.height(Spacing.md))
                    DescriptionField(
                        description = state.description,
                        onChange    = viewModel::onDescriptionChange,
                    )
                }

                // Spacer between text-entry section and navigation rows
                item(key = "gap") { Spacer(Modifier.height(Spacing.md)) }

                // Artist
                item(key = "artist") {
                    NavRow("Artist", state.selectedArtist?.displayName, onNavigateToArtist)
                    Spacer(Modifier.height(Spacing.md))
                }

                // Medium
                item(key = "medium") {
                    NavRow("Medium", state.selectedMedium, viewModel::onShowMediumPicker)
                    Spacer(Modifier.height(Spacing.md))
                }

                // Tags
                item(key = "tags") {
                    TagsRow(state.tags, viewModel::onRemoveTag, onNavigateToTags)
                    Spacer(Modifier.height(Spacing.md))
                }

                // Shop link (gated by role×plan) + price (only when a shop link is entered)
                when (state.shopLinkVisibility) {
                    ShopLinkVisibility.HIDDEN -> Unit
                    ShopLinkVisibility.LOCKED -> item(key = "shop") {
                        LockedShopLinkField(onTap = {
                            Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
                        })
                        Spacer(Modifier.height(Spacing.md))
                    }
                    ShopLinkVisibility.VISIBLE -> {
                        item(key = "shop") {
                            ShopLinkField(state.shopLink, viewModel::onShopLinkChange)
                            Spacer(Modifier.height(Spacing.md))
                        }
                        if (state.shopLink.isNotBlank()) item(key = "price") {
                            PriceField(state.price, viewModel::onPriceChange)
                            Spacer(Modifier.height(Spacing.md))
                        }
                    }
                }

                // Privacy
                item(key = "privacy") {
                    PrivacyRow(state.privacy, viewModel::onShowPrivacyPicker)
                    Spacer(Modifier.height(Spacing.md))
                }

                // Preview (upload flow only — not when editing existing art)
                if (!state.isEditing) {
                    item(key = "preview") {
                        PreviewButton(enabled = state.isValid, onClick = onNavigateToPreview)
                        Spacer(Modifier.height(Spacing.lg))
                    }
                }
            }
        }

        state.creationStatus?.let { status ->
            CreationStatusOverlay(
                status = status,
                label = "Artwork",
                error = state.creationError,
                createdTitle = if (state.isEditing) "Changes saved" else null,
                createdSubtitle = if (state.isEditing) "Your art has been updated." else null,
                onDone = {
                    viewModel.onCreationDone()
                    if (state.isEditing) onEditDone() else onUploadStarted(false)
                },
                onRetry = { if (state.isEditing) viewModel.onRetryEdit() else viewModel.onRetryCreation() },
                onDismiss = { viewModel.onCreationDone() },
            )
        }

        // Loader while the existing artwork is being fetched for editing.
        if (state.isLoadingEdit) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = BrandPrimary) }
        }
      }
    }
}

// ── Upload button ─────────────────────────────────────────────────────────────

@Composable
private fun UploadButton(enabled: Boolean, isUploading: Boolean, label: String = "Upload", onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (enabled) BrandPrimary else InactiveButton,
        label       = "uploadBtn",
    )
    Box(
        modifier         = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable(enabled = enabled && !isUploading) { onClick() }
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        if (isUploading) {
            CircularProgressIndicator(Modifier.size(Spacing.lg), strokeWidth = 2.dp, color = Color.White)
        } else {
            Text(label,
                style      = MaterialTheme.typography.labelLarge,
                color      = Color.White.copy(alpha = if (enabled) 1f else 0.7f),
                fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Title field ───────────────────────────────────────────────────────────────

@Composable
private fun TitleField(title: String, hasError: Boolean, onChange: (String) -> Unit) {
    val d = LocalDimens.current
    Column(Modifier.padding(horizontal = Spacing.md)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(d.cardCornerRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = Spacing.md, vertical = Spacing.lg),
        ) {
            Column(Modifier.fillMaxWidth()) {
                if (title.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth()) {
                        Text("Title",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f))
                        Text("${title.length}/40 characters",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(Spacing.xs))
                }
                BasicTextField(
                    value           = title,
                    onValueChange   = onChange,
                    textStyle       = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush     = SolidColor(BrandPrimary),
                    singleLine      = true,
                    modifier        = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction      = ImeAction.Next,
                    ),
                    decorationBox   = { inner ->
                        Box {
                            if (title.isEmpty()) FieldHint("Title")
                            inner()
                        }
                    },
                )
            }
        }
        if (hasError) {
            Spacer(Modifier.height(Spacing.xs))
            Text("Explore prohibited content guidelines",
                style    = MaterialTheme.typography.labelSmall,
                color    = ErrorDark,
                modifier = Modifier.padding(horizontal = Spacing.xs))
        }
    }
}

// ── Description field ─────────────────────────────────────────────────────────

@Composable
private fun DescriptionField(description: String, onChange: (String) -> Unit) {
    val d = LocalDimens.current
    Box(
        modifier = Modifier
            .padding(horizontal = Spacing.md)
            .fillMaxWidth()
            .clip(RoundedCornerShape(d.cardCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = Spacing.md, vertical = Spacing.lg),
    ) {
        Column(Modifier.fillMaxWidth()) {
            if (description.isNotEmpty()) {
                Row(Modifier.fillMaxWidth()) {
                    Text("Description",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f))
                    Text("${description.length}/255 characters",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(Spacing.xs))
            }
            BasicTextField(
                value           = description,
                onValueChange   = onChange,
                textStyle       = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground),
                cursorBrush     = SolidColor(BrandPrimary),
                // Fixed height for ~4 lines of text
                modifier        = Modifier.fillMaxWidth().height(Spacing.giant * 2 + Spacing.lg),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction      = ImeAction.Next,
                ),
                decorationBox   = { inner ->
                    Box {
                        if (description.isEmpty()) FieldHint("Description")
                        inner()
                    }
                },
            )
        }
    }
}

// ── Navigation row ────────────────────────────────────────────────────────────

@Composable
private fun NavRow(label: String, value: String?, onClick: () -> Unit) {
    val d = LocalDimens.current
    Row(
        modifier          = Modifier
            .padding(horizontal = Spacing.md)
            .fillMaxWidth()
            .clip(RoundedCornerShape(d.cardCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = Spacing.md, vertical = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text     = value ?: label,
            style    = MaterialTheme.typography.bodyMedium,
            color    = if (value != null) MaterialTheme.colorScheme.onBackground
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            painter            = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier           = Modifier.size(Spacing.xl),
        )
    }
}

// ── Tags row ──────────────────────────────────────────────────────────────────

@Composable
private fun TagsRow(tags: List<String>, onRemoveTag: (String) -> Unit, onClick: () -> Unit) {
    val d = LocalDimens.current
    Box(
        modifier = Modifier
            .padding(horizontal = Spacing.md)
            .fillMaxWidth()
            .clip(RoundedCornerShape(d.cardCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = Spacing.md, vertical = Spacing.lg),
    ) {
        if (tags.isEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Tags",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f))
                Icon(painterResource(R.drawable.ic_arrow_right), null,
                    tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(Spacing.xl))
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                items(tags, key = { it }) { tag ->
                    InlineTagChip(tag) { onRemoveTag(tag) }
                }
            }
        }
    }
}

@Composable
private fun InlineTagChip(label: String, onRemove: () -> Unit) {
    Row(
        modifier          = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            .padding(start = Spacing.sm, end = Spacing.xs,
                     top = Spacing.xs, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.width(Spacing.xs))
        Icon(Icons.Default.Close, null,
            tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.size(Spacing.md).clickable { onRemove() })
    }
}

// ── Shop link — dark gradient + Premium badge at top-right corner ─────────────

/** Artist-Free shop-link field: shown but locked behind the Artist Pro paywall (handout §Field gating). */
@Composable
private fun LockedShopLinkField(onTap: () -> Unit) {
    val d = LocalDimens.current
    Box(
        modifier = Modifier
            .padding(horizontal = Spacing.md)
            .fillMaxWidth()
            .clip(RoundedCornerShape(d.cardCornerRadius))
            .background(ShopLinkGradient)
            .clickable { onTap() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.md, end = Spacing.md, top = Spacing.xl, bottom = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(Spacing.lg),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                "Shop link",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = Spacing.xs, end = 0.dp)
                .clip(RoundedCornerShape(
                    topStart = 0.dp, topEnd = d.cardCornerRadius,
                    bottomStart = d.cardCornerRadius, bottomEnd = 0.dp,
                ))
                .background(BrandPrimary)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Text("Premium", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Price input — only rendered when a shop link is present (handout §Field gating). */
@Composable
private fun PriceField(value: String, onChange: (String) -> Unit) {
    val d = LocalDimens.current
    Row(
        modifier = Modifier
            .padding(horizontal = Spacing.md)
            .fillMaxWidth()
            .clip(RoundedCornerShape(d.cardCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = Spacing.md, vertical = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Price",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(Spacing.md))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
            cursorBrush = SolidColor(BrandPrimary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            "e.g. 250",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun ShopLinkField(value: String, onChange: (String) -> Unit) {
    val d = LocalDimens.current
    Box(
        modifier = Modifier
            .padding(horizontal = Spacing.md)
            .fillMaxWidth()
            .clip(RoundedCornerShape(d.cardCornerRadius))
            .background(ShopLinkGradient),
    ) {
        // Main content — horizontally padded, vertically generous
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.md, end = Spacing.md,
                         top = Spacing.xl, bottom = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value           = value,
                onValueChange   = onChange,
                textStyle       = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                cursorBrush     = SolidColor(Color.White),
                singleLine      = true,
                modifier        = Modifier.weight(1f),
                decorationBox   = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text("Shop link",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f))
                        }
                        inner()
                    }
                },
            )
        }

        // "Premium" badge — pinned to top-right corner
        Box(
            modifier         = Modifier
                .align(Alignment.TopEnd)
                .padding(top = Spacing.xs, end = 0.dp)
                .clip(RoundedCornerShape(
                    topStart = 0.dp, topEnd = d.cardCornerRadius,
                    bottomStart = d.cardCornerRadius, bottomEnd = 0.dp,
                ))
                .background(BrandPrimary)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Text("Premium",
                style      = MaterialTheme.typography.labelSmall,
                color      = Color.White,
                fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Privacy row — plain (no card) ─────────────────────────────────────────────

@Composable
private fun PrivacyRow(privacy: PrivacyOption, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Privacy",
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onBackground,
            modifier   = Modifier.weight(1f))
        Row(
            modifier          = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_globe), null,
                tint     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                modifier = Modifier.size(Spacing.lg))
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text  = if (privacy == PrivacyOption.PUBLIC) "Public" else "Private",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

// ── Preview button ────────────────────────────────────────────────────────────

@Composable
private fun PreviewButton(enabled: Boolean, onClick: () -> Unit) {
    val d = LocalDimens.current
    val bgColor by animateColorAsState(
        targetValue = if (enabled) BrandPrimary else MaterialTheme.colorScheme.surfaceVariant,
        label       = "previewBtn",
    )
    Box(
        modifier         = Modifier
            .padding(horizontal = Spacing.md)
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = Spacing.md + Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text("Preview",
            style      = MaterialTheme.typography.labelLarge,
            color      = if (enabled) Color.White
                         else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontWeight = FontWeight.SemiBold)
    }
}

// ── Placeholder hint ──────────────────────────────────────────────────────────

@Composable
private fun FieldHint(text: String) {
    Text(text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}
