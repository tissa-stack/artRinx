package com.rinx.artRINXapp.feature.settings.presentation.editprofile

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.ShopLinkGradientEnd
import com.rinx.artRINXapp.core.theme.ShopLinkGradientStart
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.home.presentation.components.shimmer.rememberShimmerBrush
import com.rinx.artRINXapp.feature.profile.presentation.steps.InfoTooltip

private val AGE_RANGES = listOf("Under 18", "18-25", "26-35", "36-45", "46-55", "56-65", "65+")

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val dimens = LocalDimens.current

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.onPictureSelected(uri) }

    // Navigate back once a save succeeds (or was a no-op).
    LaunchedEffect(state.saveStatus) {
        if (state.saveStatus == SaveStatus.SAVED) {
            viewModel.onSaveHandled()
            onSaved()
        }
    }

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
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
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
                galleryLauncher = galleryLauncher,
            )
        }
    }
}

@Composable
private fun ColumnScope.EditProfileContent(
    state: EditProfileUiState,
    viewModel: EditProfileViewModel,
    galleryLauncher: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>,
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
                        .clickable {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
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
                                // Band tinted with onSurfaceVariant so it stays visible against the
                                // avatar's surfaceVariant circle (surface ≈ surfaceVariant here).
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(
                                            rememberShimmerBrush(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f),
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                ),
                                            ),
                                        ),
                                )
                            },
                            error = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_edit_photo),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(avatarSize * 0.38f),
                                )
                            },
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit_photo),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(avatarSize * 0.38f),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(Spacing.xxxl)
                        .clip(CircleShape)
                        .background(BrandPrimary)
                        .clickable {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
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

            // Username (+ tooltip)
            LabeledTextField(
                label = "Username",
                value = state.username,
                onValueChange = viewModel::onUsernameChange,
                trailingIcon = { HelpIcon(state.showUsernameTooltip, viewModel::onUsernameTooltipToggle) },
            )
            Tooltip(
                visible = state.showUsernameTooltip,
                text = "Username can only be changed every 90 days.",
                onClose = viewModel::onUsernameTooltipToggle,
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

            // Full name (+ tooltip)
            LabeledTextField(
                label = "Full name",
                value = state.fullName,
                onValueChange = viewModel::onFullNameChange,
                trailingIcon = { HelpIcon(state.showFullNameTooltip, viewModel::onFullNameTooltipToggle) },
            )
            Tooltip(
                visible = state.showFullNameTooltip,
                text = "Ensure that the name you enter is your verifiable legal name. This can only be changed 3 times.",
                onClose = viewModel::onFullNameTooltipToggle,
            )
            Spacer(Modifier.height(Spacing.md))

            // Bio
            LabeledTextField(
                label = "Bio",
                value = state.bio,
                onValueChange = viewModel::onBioChange,
                maxChars = 200,
            )
            Spacer(Modifier.height(Spacing.md))

            // Shop link — premium-locked gradient card
            ShopLinkCard()
            Spacer(Modifier.height(Spacing.md))

            // Display name (+ tooltip)
            LabeledTextField(
                label = "Display name",
                value = state.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                trailingIcon = { HelpIcon(state.showDisplayNameTooltip, viewModel::onDisplayNameTooltipToggle) },
            )
            Tooltip(
                visible = state.showDisplayNameTooltip,
                text = "You're welcome to use your legal name or an alias. This can only be changed every 90 days.",
                onClose = viewModel::onDisplayNameTooltipToggle,
            )
            Spacer(Modifier.height(Spacing.md))

            // Age
            LabeledDropdownField(
                label = "Age",
                value = state.age,
                options = AGE_RANGES,
                onValueChange = viewModel::onAgeChange,
            )
            Spacer(Modifier.height(Spacing.md))

            LabeledTextField(label = "Country", value = state.country, onValueChange = viewModel::onCountryChange)
            Spacer(Modifier.height(Spacing.md))
            LabeledTextField(label = "State", value = state.state, onValueChange = viewModel::onStateChange)
            Spacer(Modifier.height(Spacing.md))
            LabeledTextField(label = "City", value = state.city, onValueChange = viewModel::onCityChange)

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
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_help),
            contentDescription = "Info",
            tint = if (active) BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Spacing.xl),
        )
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

@Composable
private fun ShopLinkCard() {
    val dimens = LocalDimens.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimens.textFieldHeight)
            .clip(RoundedCornerShape(dimens.authButtonHeight / 4))
            .background(Brush.horizontalGradient(listOf(ShopLinkGradientStart, ShopLinkGradientEnd)))
            .padding(horizontal = Spacing.lg),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Shop link",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            // "Premium" badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            ) {
                Text(
                    text = "Premium",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}