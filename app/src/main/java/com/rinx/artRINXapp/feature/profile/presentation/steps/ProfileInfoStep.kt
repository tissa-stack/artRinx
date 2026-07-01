package com.rinx.artRINXapp.feature.profile.presentation.steps

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.core.ui.TextLimits
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar
import com.rinx.artRINXapp.feature.profile.presentation.UsernameCheckState
import com.rinx.artRINXapp.feature.profile.presentation.components.ProfileTextField

@Composable
fun ProfileInfoStep(
    pictureUri: Uri?,
    googlePhotoUrl: String?,
    avatarPrefilling: Boolean,
    fullName: String,
    username: String,
    displayName: String,
    bio: String,
    usernameCheckState: UsernameCheckState,
    fullNameError: Boolean,
    usernameError: Boolean,
    displayNameError: Boolean,
    showFullNameTooltip: Boolean,
    showDisplayNameTooltip: Boolean,
    onPictureSelected: (Uri?) -> Unit,
    onFullNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onFullNameTooltipToggle: () -> Unit,
    onDisplayNameTooltipToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    // Guard against rapid taps stacking multiple picker sheets — only launch one at a time.
    var pickerInFlight by remember { mutableStateOf(false) }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> pickerInFlight = false; onPictureSelected(uri) }

    // Tapping the avatar (or the edit badge) opens the system photo picker directly — gallery is the
    // only supported source for a profile picture.
    val openPhotoPicker = {
        if (!pickerInFlight) {
            pickerInFlight = true
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }

    val avatarSize = dimens.authButtonHeight * 1.6f

    Column(
        // No internal scroll — the parent ProfileCreationScreen owns one scroll for the whole flow.
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.xxl))

        // ── Heading ───────────────────────────────────────────────────────────
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "This information will be public on artRINX.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Spacing.xxl))

        // ── Avatar ────────────────────────────────────────────────────────────
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable(onClick = openPhotoPicker),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    // Picked / downloaded local photo wins.
                    pictureUri != null -> AsyncImage(
                        model = pictureUri,
                        contentDescription = "Profile picture",
                        modifier = Modifier.size(avatarSize).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                    // Google sign-in: show the remote photo (initials fallback) while it downloads.
                    googlePhotoUrl != null -> RinxAvatar(
                        url = googlePhotoUrl,
                        contentDescription = "Profile picture",
                        size = avatarSize,
                        name = fullName,
                    )
                    // No image → the empty-profile illustration fills the avatar (nothing else).
                    else -> Image(
                        painter = painterResource(R.drawable.ic_profile_empty),
                        contentDescription = null,
                        modifier = Modifier.size(avatarSize).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }
                if (avatarPrefilling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(avatarSize * 0.3f),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(Spacing.xxxl)
                    .clip(CircleShape)
                    .background(BrandPrimary)
                    .clickable(onClick = openPhotoPicker),
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

        Spacer(Modifier.height(Spacing.xxl))

        // ── Full Name ─────────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {
            ProfileTextField(
                value = fullName,
                onValueChange = onFullNameChange,
                placeholder = "Enter your full name",
                modifier = Modifier.fillMaxWidth(),
                maxChars = TextLimits.FULL_NAME,
                hasError = fullNameError,
                capitalization = KeyboardCapitalization.Words,
                trailingIcon = {
                    InfoIconButton(
                        active = showFullNameTooltip,
                        onClick = onFullNameTooltipToggle,
                    )
                },
            )
            if (fullNameError) {
                ErrorLabel("Enter a valid name")
            }
            AnimatedVisibility(
                visible = showFullNameTooltip,
                enter = expandVertically(tween(220)) + fadeIn(tween(220)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(180)),
            ) {
                InfoTooltip(
                    text = "Ensure that the name you enter is your verifiable legal name. This can only be changed 3 times.",
                    onClose = onFullNameTooltipToggle,
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))

        // ── Username ──────────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {
            val usernameTooShort = username.isNotEmpty() && username.length < 5
            ProfileTextField(
                value = username,
                onValueChange = onUsernameChange,
                placeholder = "Username",
                modifier = Modifier.fillMaxWidth(),
                maxChars = TextLimits.USERNAME,
                capitalization = KeyboardCapitalization.Words,
                hasError = usernameError || usernameTooShort || usernameCheckState is UsernameCheckState.Taken,
            )
            // Status/hint on the left, character counter on the right.
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    // Immediate min-length feedback before the availability check (which only runs at >= 5).
                    if (usernameTooShort) {
                        Text(
                            text = "Username must be at least 5 characters",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = Spacing.xs, start = Spacing.sm),
                        )
                    } else {
                        UsernameStatusRow(state = usernameCheckState, hasError = usernameError)
                    }
                }
                Text(
                    text = "${username.length}/${TextLimits.USERNAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs, end = Spacing.sm),
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))

        // ── Display Name ──────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {
            ProfileTextField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                placeholder = "Display name",
                modifier = Modifier.fillMaxWidth(),
                maxChars = TextLimits.DISPLAY_NAME,
                hasError = displayNameError,
                capitalization = KeyboardCapitalization.Words,
                trailingIcon = {
                    InfoIconButton(
                        active = showDisplayNameTooltip,
                        onClick = onDisplayNameTooltipToggle,
                    )
                },
            )
            if (displayNameError) {
                ErrorLabel("Enter a display name")
            }
            AnimatedVisibility(
                visible = showDisplayNameTooltip,
                enter = expandVertically(tween(220)) + fadeIn(tween(220)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(180)),
            ) {
                InfoTooltip(
                    text = "You can use any name or alias as your display name.",
                    onClose = onDisplayNameTooltipToggle,
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))

        // ── Bio ───────────────────────────────────────────────────────────────
        ProfileTextField(
            value = bio,
            onValueChange = onBioChange,
            placeholder = "Bio",
            modifier = Modifier.fillMaxWidth(),
            maxChars = TextLimits.BIO,
        )

        Spacer(Modifier.height(Spacing.xxxl))
    }
}

// ── Info icon button (trailing icon inside the field) ─────────────────────────
@Composable
private fun InfoIconButton(active: Boolean, onClick: () -> Unit) {
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

// ── Tooltip popup — compact speech-bubble card, right-aligned, ~70% width ─────
@Composable
internal fun InfoTooltip(
    text: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    // When true the tail sits below the card pointing DOWN — use when the tooltip is shown ABOVE its
    // anchor (e.g. an anchor near the bottom of the screen).
    tailAtBottom: Boolean = false,
) {
    val cardColor = MaterialTheme.colorScheme.inverseSurface
    val textColor = MaterialTheme.colorScheme.inverseOnSurface
    val triW = 14.dp
    val triH = 10.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
    ) {
        // Card is ~70% of available width, right-aligned
        val cardWidth = maxWidth * 0.70f

        @Composable
        fun Tail(pointDown: Boolean) {
            Row(
                modifier = Modifier.width(cardWidth),
                horizontalArrangement = Arrangement.End,
            ) {
                Canvas(
                    modifier = Modifier
                        .padding(end = Spacing.xl)
                        .size(triW, triH),
                ) {
                    val path = Path().apply {
                        if (pointDown) {
                            moveTo(size.width / 2f, size.height)
                            lineTo(size.width, 0f)
                            lineTo(0f, 0f)
                        } else {
                            moveTo(size.width / 2f, 0f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                        }
                        close()
                    }
                    drawPath(path, cardColor)
                }
            }
        }

        Column(
            modifier = Modifier
                .width(cardWidth)
                .align(Alignment.TopEnd),
        ) {
            if (!tailAtBottom) Tail(pointDown = false)

            // Card body — same color as triangle so they merge seamlessly
            Box(
                modifier = Modifier
                    .width(cardWidth)
                    .clip(RoundedCornerShape(Spacing.md))
                    .background(cardColor),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = textColor,
                            modifier = Modifier.clickable(onClick = onClose),
                        )
                    }
                }
            }

            if (tailAtBottom) Tail(pointDown = true)
        }
    }
}

// Alias for PersonalInfoStep
@Composable
internal fun InlineTooltip(text: String, onClose: () -> Unit, modifier: Modifier = Modifier) =
    InfoTooltip(text = text, onClose = onClose, modifier = modifier)

// Alias kept for backward compat
@Composable
internal fun TooltipBox(text: String, onClose: () -> Unit, modifier: Modifier = Modifier) =
    InfoTooltip(text = text, onClose = onClose, modifier = modifier)

// ── Error text ────────────────────────────────────────────────────────────────
@Composable
private fun ErrorLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(start = Spacing.md, top = Spacing.xs),
    )
}

// ── Username availability row ─────────────────────────────────────────────────
@Composable
private fun UsernameStatusRow(state: UsernameCheckState, hasError: Boolean) {
    when {
        state is UsernameCheckState.Checking -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.padding(start = Spacing.md, top = Spacing.xs),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(Spacing.md),
                strokeWidth = 1.5.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Checking…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        state is UsernameCheckState.Available -> Text(
            text = "${state.username} is available",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Spacing.md, top = Spacing.xs),
        )

        state is UsernameCheckState.Taken -> Text(
            text = "${state.username} is taken",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = Spacing.md, top = Spacing.xs),
        )

        hasError -> Text(
            text = "Enter a valid username",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = Spacing.md, top = Spacing.xs),
        )
    }
}

