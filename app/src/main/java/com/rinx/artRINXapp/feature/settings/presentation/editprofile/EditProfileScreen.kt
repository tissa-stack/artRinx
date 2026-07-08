package com.rinx.artRINXapp.feature.settings.presentation.editprofile

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.core.ui.DobPickerField
import com.rinx.artRINXapp.core.ui.ErrorSnackbarHost
import com.rinx.artRINXapp.core.ui.SearchableTextDropdownField
import com.rinx.artRINXapp.core.ui.TextLimits
import com.rinx.artRINXapp.feature.home.presentation.components.shimmer.rememberShimmerBrush
import com.rinx.artRINXapp.feature.profile.presentation.steps.InfoTooltip

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onChangeMedium: () -> Unit = {},
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val dimens = LocalDimens.current

    // Profile-picture picker: tapping the avatar opens the system photo picker directly. Gallery is
    // the only supported source; the chosen Uri flows to onPictureSelected.
    // Guard against rapid taps stacking multiple picker sheets — only launch one at a time.
    var pickerInFlight by remember { mutableStateOf(false) }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> pickerInFlight = false; viewModel.onPictureSelected(uri) }

    // Fallback picker for devices with no ACTION_PICK_IMAGES handler (PickVisualMedia would crash
    // with ActivityNotFoundException); GetContent (ACTION_GET_CONTENT) is universally handled.
    val getContentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> pickerInFlight = false; viewModel.onPictureSelected(uri) }

    val openPhotoPicker = {
        if (!pickerInFlight) {
            pickerInFlight = true
            try {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            } catch (e: android.content.ActivityNotFoundException) {
                runCatching { getContentLauncher.launch("image/*") }
                    .onFailure { pickerInFlight = false }
            }
        }
    }

    // Navigate back once a save succeeds (or was a no-op).
    LaunchedEffect(state.saveStatus) {
        if (state.saveStatus == SaveStatus.SAVED) {
            viewModel.onSaveHandled()
            onSaved()
        }
    }

    // Guard against losing unsaved edits: confirm before leaving when the form is dirty.
    var showDiscard by remember { mutableStateOf(false) }
    val tryBack = { if (viewModel.isDirty) showDiscard = true else onBack() }
    BackHandler(enabled = !state.isSaving) { tryBack() }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Leaving now will discard them.") },
            confirmButton = {
                TextButton(onClick = { showDiscard = false; onBack() }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscard = false }) { Text("Keep editing") }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
      Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding(),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xs),
        ) {
            IconButton(onClick = tryBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
        }
            Text(
                text = "Edit profile",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        when {
            // ── Loading the profile ────────────────────────────────────────────
            state.isLoading -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = BrandPrimary) }

            // ── Failed to load ─────────────────────────────────────────────────
            state.loadError != null -> Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = dimens.screenPaddingHorizontal),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = state.loadError!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.md))
                TextButton(onClick = viewModel::onRetryLoad) {
                    Text("Retry", color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                }
            }

            // ── Loaded → editable form ─────────────────────────────────────────
            else -> EditProfileContent(
                state = state,
                viewModel = viewModel,
                onAvatarTapped = openPhotoPicker,
                onChangeMedium = onChangeMedium,
            )
        }
      }
      // Red banner (e.g. the premium profile-link upgrade prompt) — matches the report toast.
      ErrorSnackbarHost(
          message = state.upgradeMessage,
          onShown = viewModel::onUpgradeMessageShown,
          modifier = Modifier
              .align(Alignment.BottomCenter)
              .navigationBarsPadding()
              .imePadding(),
      )
    }
}

@Composable
private fun ColumnScope.EditProfileContent(
    state: EditProfileUiState,
    viewModel: EditProfileViewModel,
    onAvatarTapped: () -> Unit,
    onChangeMedium: () -> Unit,
) {
    val dimens = LocalDimens.current
    // ── Scroll body ──────────────────────────────────────────────────────────
    Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenPaddingHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Spacing.lg))

            // Avatar
            val avatarSize = dimens.authButtonHeight * 1.6f
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable(onClick = onAvatarTapped),
                    contentAlignment = Alignment.Center,
                ) {
                    val avatarModel = state.pictureUri ?: state.pictureUrl
                    if (avatarModel != null) {
                        SubcomposeAsyncImage(
                            model = avatarModel,
                            contentDescription = "Profile picture",
                            modifier = Modifier.size(avatarSize).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            loading = {
                                // The sweep band must read as a LIGHTER glint than the avatar's
                                // surfaceVariant circle in BOTH themes. onSurfaceVariant is a
                                // foreground colour: light-grey on dark (fine) but dark-grey on
                                // light (a harsh dark band). In light theme sweep toward the bright
                                // background instead; the default brush's `surface` highlight is too
                                // close to surfaceVariant here to be visible.
                                val band = if (isSystemInDarkTheme()) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.background
                                }
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(
                                            rememberShimmerBrush(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    band,
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                ),
                                            ),
                                        ),
                                )
                            },
                            error = {
                                Image(
                                    painter = painterResource(R.drawable.ic_profile_empty),
                                    contentDescription = null,
                                    modifier = Modifier.size(avatarSize).clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                )
                            },
                        )
                    } else {
                        // No image → the empty-profile illustration fills the avatar (nothing else).
                        Image(
                            painter = painterResource(R.drawable.ic_profile_empty),
                            contentDescription = null,
                            modifier = Modifier.size(avatarSize).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(Spacing.xxxl)
                        .clip(CircleShape)
                        .background(BrandPrimary)
                        .clickable(onClick = onAvatarTapped),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit_photo),
                        contentDescription = "Change photo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(Spacing.lg),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            // Username (read-only) — usernames can't be changed from edit profile.
            LabeledTextField(
                label = "Username",
                value = state.username,
                onValueChange = viewModel::onUsernameChange,
                maxChars = TextLimits.USERNAME,
                displayOnly = true,
                capitalization = KeyboardCapitalization.Words,
            )
            if (state.usernameError != null) {
                Text(
                    text = state.usernameError!!,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = Spacing.md, top = Spacing.xs),
                )
            }
            Spacer(Modifier.height(Spacing.md))

            // Full name (+ tooltip) — once the 2-edit cap is reached (handout §9) it becomes
            // display-only (not editable) but keeps normal colors, like the username field.
            LabeledTextField(
                label = "Full name",
                value = state.fullName,
                onValueChange = viewModel::onFullNameChange,
                maxChars = TextLimits.FULL_NAME,
                displayOnly = !state.canEditFullName,
                capitalization = KeyboardCapitalization.Words,
                trailingIcon = { HelpIcon(state.showFullNameTooltip, viewModel::onFullNameTooltipToggle) },
            )
            Tooltip(
                visible = state.showFullNameTooltip,
                text = if (state.canEditFullName) {
                    "Ensure that the name you enter is your verifiable legal name. This can only be changed 2 times."
                } else {
                    "You've reached the limit for changing your full name."
                },
                onClose = viewModel::onFullNameTooltipToggle,
            )
            Spacer(Modifier.height(Spacing.md))

            // Bio
            LabeledTextField(
                label = "Bio",
                value = state.bio,
                onValueChange = viewModel::onBioChange,
                maxChars = TextLimits.BIO,
            )
            Spacer(Modifier.height(Spacing.md))

            // Profile link (premium-locked) — shown as a normal field; entering one and saving
            // surfaces the upgrade banner instead of persisting it. The info tooltip explains it.
            LabeledTextField(
                label = "Profile link",
                value = state.profileLink,
                onValueChange = viewModel::onProfileLinkChange,
                trailingIcon = { HelpIcon(state.showProfileLinkTooltip, viewModel::onProfileLinkTooltipToggle) },
            )
            Tooltip(
                visible = state.showProfileLinkTooltip,
                text = "Upgrade to add profile link.",
                onClose = viewModel::onProfileLinkTooltipToggle,
            )
            Spacer(Modifier.height(Spacing.md))

            // Display name (+ tooltip)
            LabeledTextField(
                label = "Display name",
                value = state.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                maxChars = TextLimits.DISPLAY_NAME,
                capitalization = KeyboardCapitalization.Words,
                trailingIcon = { HelpIcon(state.showDisplayNameTooltip, viewModel::onDisplayNameTooltipToggle) },
            )
            Tooltip(
                visible = state.showDisplayNameTooltip,
                text = "You're welcome to use your legal name or an alias. This can only be changed every 90 days.",
                onClose = viewModel::onDisplayNameTooltipToggle,
            )
            Spacer(Modifier.height(Spacing.md))

            // Date of birth
            DobPickerField(
                label = "Date of birth",
                value = state.dob,
                onDobSelected = viewModel::onDobChange,
            )
            Text(
                text = "You must be at least 18 to use artRINX.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.md, top = Spacing.xs),
            )
            Spacer(Modifier.height(Spacing.md))

            // Country → State → City type-to-search cascade (master catalog APIs). Dropdown-selection
            // only (no clear-X, no free text). Country is mandatory; State/City are required only when
            // the selected country/state has them.
            LocationDropdownField(
                label = "Country",
                value = state.country,
                options = state.countryOptions,
                hasError = state.countryError,
                onQueryChange = viewModel::onCountryQuery,
                onOptionSelected = viewModel::onCountrySelected,
            )
            Spacer(Modifier.height(Spacing.md))
            LocationDropdownField(
                label = "State",
                value = state.state,
                options = state.stateOptions,
                hasError = state.stateError,
                onQueryChange = viewModel::onStateQuery,
                onOptionSelected = viewModel::onStateSelected,
            )
            Spacer(Modifier.height(Spacing.md))
            LocationDropdownField(
                label = "City",
                value = state.city,
                options = state.cityOptions,
                hasError = state.cityError,
                onQueryChange = viewModel::onCityQuery,
                onOptionSelected = viewModel::onCitySelected,
            )
            Spacer(Modifier.height(Spacing.md))

            // Change medium → opens the medium-selection screen pre-filled with current choices.
            NavFieldRow(label = "Change medium", onClick = onChangeMedium)

            Spacer(Modifier.height(Spacing.xxxl))
        }

    // ── Save button ──────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = dimens.screenPaddingHorizontal, vertical = Spacing.lg),
    ) {
        if (state.saveError != null) {
            Text(
                text = state.saveError!!,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.sm),
            )
        }
        Button(
            onClick = viewModel::onSave,
            enabled = state.canSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimens.authButtonHeight),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPrimary,
                disabledContainerColor = InactiveButton,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
            ),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Spacing.xl),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Save Changes", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun HelpIcon(active: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(Spacing.xxxl),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_help),
            contentDescription = "Info",
            tint = if (active) BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Spacing.lg),
        )
    }
}

/** Dropdown-only location picker (no clear-X) with an inline "must pick from the list" error. */
@Composable
private fun LocationDropdownField(
    label: String,
    value: String,
    options: List<String>,
    hasError: Boolean,
    onQueryChange: (String) -> Unit,
    onOptionSelected: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SearchableTextDropdownField(
            label = label,
            value = value,
            options = options,
            onQueryChange = onQueryChange,
            onOptionSelected = onOptionSelected,
            showClearIcon = false,
            dropdownOnly = true,
        )
        if (hasError) {
            Text(
                text = "Please select from the list",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = Spacing.md, top = Spacing.xs),
            )
        }
    }
}

@Composable
private fun Tooltip(visible: Boolean, text: String, onClose: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(220)) + fadeIn(tween(220)),
        exit = shrinkVertically(tween(180)) + fadeOut(tween(180)),
    ) {
        InfoTooltip(text = text, onClose = onClose)
    }
}

/** A field-styled row that navigates elsewhere on tap (label + chevron). */
@Composable
private fun NavFieldRow(label: String, onClick: () -> Unit) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimens.textFieldHeight)
            .clip(RoundedCornerShape(dimens.authButtonHeight / 4))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            // Match the other field VALUE text (LabeledTextField uses bodyMedium).
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Spacing.xl),
        )
    }
}
