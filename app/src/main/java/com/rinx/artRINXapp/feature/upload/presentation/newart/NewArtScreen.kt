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
import androidx.compose.foundation.text.KeyboardActions
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
import com.rinx.artRINXapp.feature.upload.domain.model.MAX_ARTWORK_DIMENSION
import com.rinx.artRINXapp.feature.upload.domain.model.MAX_ARTWORK_TAGS
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

    // Edit prefill failed (e.g. the artwork was deleted) → don't leave the user on a blank form.
    LaunchedEffect(state.editLoadFailed) {
        if (state.editLoadFailed) {
            Toast.makeText(context, "This artwork is no longer available.", Toast.LENGTH_SHORT).show()
            onBack()
        }
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

                // Description (required)
                item(key = "desc") {
                    Spacer(Modifier.height(Spacing.md))
                    DescriptionField(
                        description = state.description,
                        hasError    = state.isDescriptionError,
                        onChange    = viewModel::onDescriptionChange,
                    )
                }

                // Spacer between text-entry section and navigation rows
                item(key = "gap") { Spacer(Modifier.height(Spacing.md)) }

                // Artist (required — name mandatory, with or without a RINX profile)
                item(key = "artist") {
                    NavRow(
                        label   = "Artist",
                        value   = state.selectedArtist?.displayName,
                        onClick = onNavigateToArtist,
                        isError = state.isArtistError,
                        required = true,
                    )
                    if (state.isArtistError) {
                        Spacer(Modifier.height(Spacing.xs))
                        FieldErrorText("Artist name is required")
                    }
                    Spacer(Modifier.height(Spacing.md))
                }

                // Medium (required)
                item(key = "medium") {
                    NavRow(
                        label   = "Medium",
                        value   = state.selectedMedium,
                        onClick = viewModel::onShowMediumPicker,
                        isError = state.isMediumError,
                        required = true,
                    )
                    if (state.isMediumError) {
                        Spacer(Modifier.height(Spacing.xs))
                        FieldErrorText("Please select a medium")
                    }
                    Spacer(Modifier.height(Spacing.md))
                }

                // Tags — inline editor (type a tag, tap Add). At least one tag is required.
                item(key = "tags") {
                    TagsEditor(
                        tags          = state.tags,
                        input         = state.currentTagInput,
                        onInputChange = viewModel::onTagInputChange,
                        onAdd         = viewModel::onAddTag,
                        onRemove      = viewModel::onRemoveTag,
                    )
                    if (state.isTagsError) {
                        Spacer(Modifier.height(Spacing.xs))
                        FieldErrorText("Add at least one tag")
                    }
                    Spacer(Modifier.height(Spacing.md))
                }

                // Shop link — premium-only. Editable for paid Artist/Gallery; locked (paywall) for
                // Artist Free; hidden for Collector / Art Curious / inactive Gallery (handout §Field gating).
                item(key = "shop") {
                    when (state.shopLinkVisibility) {
                        ShopLinkVisibility.VISIBLE -> {
                            ShopLinkPlainField(state.shopLink, viewModel::onShopLinkChange)
                            Spacer(Modifier.height(Spacing.md))
                        }
                        ShopLinkVisibility.LOCKED -> {
                            val shopCtx = LocalContext.current
                            LockedShopLinkField(onTap = {
                                Toast.makeText(context, "Adding a shop link requires Artist Pro.", Toast.LENGTH_SHORT).show()
                            })
                            Spacer(Modifier.height(Spacing.md))
                        }
                        ShopLinkVisibility.HIDDEN -> Unit
                    }
                }
                if (state.shopLink.isNotBlank()) item(key = "price") {
                    PriceField(state.price, state.isPriceError, viewModel::onPriceChange)
                    if (state.isPriceError) {
                        Spacer(Modifier.height(Spacing.xs))
                        FieldErrorText("Enter a price for your shop link")
                    }
                    Spacer(Modifier.height(Spacing.md))
                }

                // Dimensions (optional physical size; unit cm or in)
                item(key = "size") {
                    DimensionsRow(
                        height = state.sizeHeightCm,
                        width = state.sizeWidthCm,
                        unit = state.sizeUnit,
                        hasPartialDimensions = state.hasPartialDimensions,
                        hasInvalidDimensions = state.hasInvalidDimensions,
                        onHeightChange = viewModel::onSizeHeightChange,
                        onWidthChange = viewModel::onSizeWidthChange,
                        onUnitChange = viewModel::onSizeUnitChange,
                    )
                    Spacer(Modifier.height(Spacing.md))
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
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Text("Title",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            RequiredStar()
                        }
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
                            if (title.isEmpty()) FieldHint("Title", required = true)
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
private fun DescriptionField(description: String, hasError: Boolean, onChange: (String) -> Unit) {
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
                        if (description.isEmpty()) FieldHint("Description", required = true)
                        inner()
                    }
                },
            )
        }
      }
        if (hasError) {
            Spacer(Modifier.height(Spacing.xs))
            FieldErrorText("Description is required")
        }
    }
}

// ── Navigation row ────────────────────────────────────────────────────────────

@Composable
private fun NavRow(
    label: String,
    value: String?,
    onClick: () -> Unit,
    isError: Boolean = false,
    required: Boolean = false,
) {
    val d = LocalDimens.current
    Row(
        modifier          = Modifier
            .padding(horizontal = Spacing.md)
            .fillMaxWidth()
            .clip(RoundedCornerShape(d.cardCornerRadius))
            .background(if (isError) ErrorDark.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = Spacing.md, vertical = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text     = value ?: label,
                style    = MaterialTheme.typography.bodyMedium,
                color    = if (value != null) MaterialTheme.colorScheme.onBackground
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (required && value == null) RequiredStar()
        }
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

/**
 * Inline tags editor: type a tag and an "Add" button appears; tap it (or press Done) to add a chip.
 * No separate screen. Added tags render as removable chips below the input.
 */
@Composable
private fun TagsEditor(
    tags: List<String>,
    input: String,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
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
            // Tag count, above the field. Caps at MAX_ARTWORK_TAGS.
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Text(
                    "${tags.size}/$MAX_ARTWORK_TAGS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Spacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    cursorBrush = SolidColor(BrandPrimary),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAdd() }),
                    decorationBox = { inner ->
                        Box {
                            if (input.isEmpty()) FieldHint("Tags", required = true)
                            inner()
                        }
                    },
                )
                if (input.isNotBlank()) {
                    // Enabled only for a NEW tag under the cap; disabled (greyed) for a duplicate or
                    // once 10 tags are reached.
                    val normalized = input.trim().lowercase()
                    val canAdd = normalized.isNotEmpty() && normalized !in tags && tags.size < MAX_ARTWORK_TAGS
                    Spacer(Modifier.width(Spacing.sm))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (canAdd) BrandPrimary else InactiveButton)
                            .then(if (canAdd) Modifier.clickable { onAdd() } else Modifier)
                            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Add",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = if (canAdd) 1f else 0.6f),
                        )
                    }
                }
            }
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.sm))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items(tags, key = { it }) { tag ->
                        InlineTagChip(tag) { onRemove(tag) }
                    }
                }
            }
        }
    }
}

/** Plain editable shop-link field (no premium gating) — available to all users. */
@Composable
private fun ShopLinkPlainField(value: String, onChange: (String) -> Unit) {
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
            if (value.isNotEmpty()) {
                Text(
                    "Shop link",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.xs))
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                cursorBrush = SolidColor(BrandPrimary),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) FieldHint("Shop link")
                        inner()
                    }
                },
            )
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
private fun PriceField(value: String, isError: Boolean, onChange: (String) -> Unit) {
    val d = LocalDimens.current
    Row(
        modifier = Modifier
            .padding(horizontal = Spacing.md)
            .fillMaxWidth()
            .clip(RoundedCornerShape(d.cardCornerRadius))
            .background(if (isError) ErrorDark.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant)
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

/** Optional artwork dimensions — a cm/in unit selector plus two side-by-side numeric fields. */
@Composable
private fun DimensionsRow(
    height: String,
    width: String,
    unit: String,
    onHeightChange: (String) -> Unit,
    onWidthChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    hasPartialDimensions: Boolean,
    hasInvalidDimensions: Boolean,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = Spacing.md)
            .fillMaxWidth(),
    ) {
        // Section title — same style as the "Privacy" label.
        Text(
            "Dimensions",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.sm))
        // Unit selector (cm / in) — a segmented tab; entered values upload in the selected unit.
        UnitTabs(unit = unit, onUnitChange = onUnitChange)
        Spacer(Modifier.height(Spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DimensionField(
                placeholder = "Height",
                value = height,
                onChange = onHeightChange,
                modifier = Modifier.weight(1f),
            )
            DimensionField(
                placeholder = "Width",
                value = width,
                onChange = onWidthChange,
                modifier = Modifier.weight(1f),
            )
        }
        when {
            hasPartialDimensions -> {
                Text(
                    "Please enter both height and width",
                    style = MaterialTheme.typography.labelSmall,
                    color = ErrorDark,
                )
            }

            hasInvalidDimensions -> {
                Text(
                    "Enter a size between 1 and ${MAX_ARTWORK_DIMENSION.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ErrorDark,
                )
            }
        }
    }
}

/** Segmented tab control for the dimension unit (cm / in) — two equal-width tabs. */
@Composable
private fun UnitTabs(unit: String, onUnitChange: (String) -> Unit) {
    val d = LocalDimens.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(d.cardCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        UnitTab(label = "cm", selected = unit == "cm", modifier = Modifier.weight(1f)) { onUnitChange("cm") }
        UnitTab(label = "in", selected = unit == "in", modifier = Modifier.weight(1f)) { onUnitChange("in") }
    }
}

/** A single segment of [UnitTabs]. */
@Composable
private fun UnitTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val d = LocalDimens.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(d.cardCornerRadius))
            .background(if (selected) BrandPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A single optional numeric dimension field. The label shows as a placeholder that disappears
 * once typing begins, leaving the full field width for the value (dimensions are never required).
 */
@Composable
private fun DimensionField(
    placeholder: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = LocalDimens.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(d.cardCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = Spacing.md, vertical = Spacing.lg),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
            cursorBrush = SolidColor(BrandPrimary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                inner()
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
private fun FieldHint(text: String, required: Boolean = false) {
    if (required) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            RequiredStar()
        }
    } else {
        Text(text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Small red asterisk denoting a mandatory field. */
@Composable
private fun RequiredStar() {
    Text(" *",
        style = MaterialTheme.typography.bodyMedium,
        color = ErrorDark,
        fontWeight = FontWeight.SemiBold)
}

/** Inline validation error shown beneath a required field. */
@Composable
private fun FieldErrorText(text: String) {
    Text(text,
        style    = MaterialTheme.typography.labelSmall,
        color    = ErrorDark,
        modifier = Modifier.padding(horizontal = Spacing.md + Spacing.xs))
}
