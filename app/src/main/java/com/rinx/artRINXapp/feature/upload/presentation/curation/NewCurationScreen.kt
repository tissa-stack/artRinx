package com.rinx.artRINXapp.feature.upload.presentation.curation

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.DangerRed
import com.rinx.artRINXapp.core.theme.ErrorDark
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.home.presentation.components.CurationCardStack
import com.rinx.artRINXapp.feature.profile.presentation.other.components.ConfirmActionDialog
import com.rinx.artRINXapp.feature.upload.domain.model.PrivacyOption
import com.rinx.artRINXapp.feature.upload.presentation.components.CreationStatusOverlay
import com.rinx.artRINXapp.feature.upload.presentation.newart.components.PrivacyPickerSheet

private val CardFront  = Color(0xFF6B6B6B)
private val CardMiddle = Color(0xFF4D4D4D)
private val CardBack   = Color(0xFF3A3A3A)

@Composable
fun NewCurationScreen(
    onBack: () -> Unit,
    onNavigateToAddArt: () -> Unit,
    onCreateStarted: (isPrivate: Boolean) -> Unit = {},
    onEditDone: () -> Unit = onBack,
    viewModel: NewCurationViewModel = hiltViewModel(),
) {
    val state        by viewModel.state.collectAsState()
    val d            = LocalDimens.current
    val focusManager = LocalFocusManager.current
    val context      = LocalContext.current

    // Edit mode: index (into the preview deck) pending a "remove from curation" confirmation.
    var pendingDeleteIndex by remember { mutableStateOf<Int?>(null) }
    pendingDeleteIndex?.let { idx ->
        ConfirmActionDialog(
            title = "Remove from collection?",
            confirmLabel = "Remove",
            confirmColor = DangerRed,
            iconRes = R.drawable.ic_delete,
            onConfirm = { viewModel.onRemoveArtAt(idx); pendingDeleteIndex = null },
            onDismiss = { pendingDeleteIndex = null },
        )
    }

    // Edit prefill failed (e.g. the curation was deleted) → don't leave the user on a blank form.
    LaunchedEffect(state.editLoadFailed) {
        if (state.editLoadFailed) {
            Toast.makeText(context, "This collection is no longer available.", Toast.LENGTH_SHORT).show()
            onBack()
        }
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
                // Keep content (incl. the bottom Create/Save button) clear of the system 3-button
                // nav bar. Consumed here so the inner imePadding doesn't double-count it.
                .navigationBarsPadding()
                // Dismiss keyboard on tap outside interactive elements
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
        ) {
            // ── FIXED header — never scrolls ──────────────────────────────
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
                    text       = if (state.isEditing) "Edit Collection" else "New Collection",
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onBackground,
                    modifier   = Modifier.weight(1f),
                    textAlign  = TextAlign.Center,
                )
                Spacer(Modifier.width(Spacing.huge))
            }

            // ── SCROLLABLE content — imePadding lifts above keyboard ──────
            LazyColumn(
                modifier       = Modifier
                    .weight(1f)
                    .imePadding(),
                contentPadding = PaddingValues(bottom = Spacing.xxl),
            ) {

                // Card stack preview — tapping empty state navigates to add art
                item(key = "preview") {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .height(d.uploadImageHeight)
                            .padding(horizontal = Spacing.lg)
                            .then(
                                if (state.selectedArts.isEmpty())
                                    Modifier.clickable { onNavigateToAddArt() }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.selectedArts.isEmpty()) {
                            EmptyStackedCards()
                        } else {
                            CurationCardStack(
                                artworks = state.selectedArts.mapNotNull { it.imageUrl ?: it.imageRes },
                                modifier = Modifier.fillMaxSize(),
                                // Edit mode only: each card gets a delete badge → confirm → remove (saved on Save).
                                onDeleteArt = if (state.isEditing) {
                                    { index ->
                                        if (state.selectedArts.size <= 1) {
                                            Toast.makeText(context, "A collection needs at least one artwork.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            pendingDeleteIndex = index
                                        }
                                    }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }

                // Title
                item(key = "title") {
                    Spacer(Modifier.height(Spacing.md))
                    FormTextField(
                        value       = state.title,
                        onChange    = viewModel::onTitleChange,
                        placeholder = "Title",
                        singleLine  = true,
                        charLimit   = 40,
                        showCounter = state.title.isNotEmpty(),
                        required    = true,
                    )
                }

                // Description
                item(key = "desc") {
                    Spacer(Modifier.height(Spacing.md))
                    FormTextField(
                        value         = state.description,
                        onChange      = viewModel::onDescriptionChange,
                        placeholder   = "Description",
                        singleLine    = false,
                        charLimit     = 255,
                        showCounter   = state.description.isNotEmpty(),
                        contentHeight = Spacing.giant * 2 + Spacing.lg,
                        required      = true,
                    )
                }

                item(key = "gap") { Spacer(Modifier.height(Spacing.md)) }

                // Add art
                item(key = "addart") {
                    NavRow(
                        label   = "Add art",
                        value   = if (state.selectedArts.isNotEmpty())
                                      "${state.selectedArts.size} selected" else null,
                        onClick = onNavigateToAddArt,
                        required = true,
                    )
                    Spacer(Modifier.height(Spacing.md))
                }

                // Privacy
                item(key = "privacy") {
                    PrivacyRow(state.privacy, viewModel::onShowPrivacyPicker)
                    Spacer(Modifier.height(Spacing.md))
                }

                // Create button
                item(key = "create") {
                    CreateButton(
                        enabled    = state.isValid,
                        isCreating = state.isCreating,
                        label      = if (state.isEditing) "Save" else "Create",
                        onClick    = {
                            if (state.isEditing) {
                                // Edit → stay; overlay shows save progress, pops back on done.
                                viewModel.onSaveEdit()
                            } else if (viewModel.onCreate() && state.privacy != PrivacyOption.PRIVATE) {
                                // Public → Home (progress row). Private → stay; overlay shows.
                                onCreateStarted(false)
                            }
                        },
                    )
                    Spacer(Modifier.height(Spacing.lg))
                }
            }
        }

        state.creationStatus?.let { status ->
            CreationStatusOverlay(
                status = status,
                label = "Collection",
                error = state.creationError,
                createdTitle = if (state.isEditing) "Changes saved" else null,
                createdSubtitle = if (state.isEditing) "Your collection has been updated." else null,
                onDone = {
                    viewModel.onCreationDone()
                    if (state.isEditing) onEditDone() else onCreateStarted(false)
                },
                onRetry = { if (state.isEditing) viewModel.onRetryEdit() else viewModel.onRetryCreation() },
                onDismiss = { viewModel.onCreationDone() },
            )
        }

        // Loader while the existing curation is being fetched for editing.
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

// ── Empty stacked card placeholder ────────────────────────────────────────────

@Composable
private fun EmptyStackedCards() {
    val d            = LocalDimens.current
    val cardHeight   = d.uploadImageHeight * 0.80f
    val cardWidth    = d.uploadImageHeight * 0.68f
    val cornerRadius = d.cardCornerRadius

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Back card — rightmost, darkest
        Box(
            modifier = Modifier
                .width(cardWidth * 0.88f)
                .height(cardHeight * 0.88f)
                .offset(x = 22.dp, y = 8.dp)
                .zIndex(0f)
                .shadow(4.dp, RoundedCornerShape(cornerRadius))
                .clip(RoundedCornerShape(cornerRadius))
                .background(CardBack),
        )
        // Middle card
        Box(
            modifier = Modifier
                .width(cardWidth * 0.94f)
                .height(cardHeight * 0.94f)
                .offset(x = 11.dp, y = 4.dp)
                .zIndex(1f)
                .shadow(4.dp, RoundedCornerShape(cornerRadius))
                .clip(RoundedCornerShape(cornerRadius))
                .background(CardMiddle),
        )
        // Front card — has add photo icon
        Box(
            modifier         = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .zIndex(2f)
                .shadow(6.dp, RoundedCornerShape(cornerRadius))
                .clip(RoundedCornerShape(cornerRadius))
                .background(CardFront),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter            = painterResource(R.drawable.ic_add_photo),
                contentDescription = "Add art",
                tint               = Color.White.copy(alpha = 0.6f),
                modifier           = Modifier.size(Spacing.giant),
            )
        }
    }
}

// ── Form text field ───────────────────────────────────────────────────────────

@Composable
private fun FormTextField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    charLimit: Int,
    showCounter: Boolean,
    contentHeight: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Unspecified,
    required: Boolean = false,
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
            if (showCounter) {
                Row(Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(placeholder,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (required) RequiredStar()
                    }
                    Text("${value.length}/$charLimit characters",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(Spacing.xs))
            }
            val fieldMod = if (contentHeight != androidx.compose.ui.unit.Dp.Unspecified)
                Modifier.fillMaxWidth().height(contentHeight)
            else Modifier.fillMaxWidth()

            BasicTextField(
                value           = value,
                onValueChange   = { onChange(it.take(charLimit)) },
                textStyle       = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground),
                cursorBrush     = SolidColor(BrandPrimary),
                singleLine      = singleLine,
                modifier        = fieldMod,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction      = if (singleLine) ImeAction.Next else ImeAction.Default,
                ),
                decorationBox   = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(placeholder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (required) RequiredStar()
                            }
                        }
                        inner()
                    }
                },
            )
        }
    }
}

// ── Navigation row ────────────────────────────────────────────────────────────

@Composable
private fun NavRow(label: String, value: String?, onClick: () -> Unit, required: Boolean = false) {
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
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text     = if (value != null) "$label  ·  $value" else label,
                style    = MaterialTheme.typography.bodyMedium,
                color    = if (value != null) MaterialTheme.colorScheme.onBackground
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (required && value == null) RequiredStar()
        }
        Icon(painterResource(R.drawable.ic_arrow_right), null,
            tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.size(Spacing.xl))
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

// ── Privacy row ───────────────────────────────────────────────────────────────

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

// ── Create button ─────────────────────────────────────────────────────────────

@Composable
private fun CreateButton(enabled: Boolean, isCreating: Boolean, label: String = "Create", onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (enabled) BrandPrimary else MaterialTheme.colorScheme.surfaceVariant,
        label       = "createBtn",
    )
    Box(
        modifier         = Modifier
            .padding(horizontal = Spacing.md)
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .then(if (enabled && !isCreating) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = Spacing.md + Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        if (isCreating) {
            CircularProgressIndicator(Modifier.size(Spacing.lg), strokeWidth = 2.dp, color = Color.White)
        } else {
            Text(label,
                style      = MaterialTheme.typography.labelLarge,
                color      = if (enabled) Color.White
                             else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontWeight = FontWeight.SemiBold)
        }
    }
}
