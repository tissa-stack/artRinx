package com.rinx.artRINXapp.feature.auth.presentation.waitlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.components.PhoneNumberField
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.components.ProfileTypeDropdown
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.components.WaitlistCheckbox
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.components.WaitlistTextField

@Composable
fun WaitlistScreen(
    onBack: () -> Unit,
    viewModel: WaitlistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.clearError()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (uiState.isSuccess) {
        WaitlistSuccessContent(onBack = onBack)
    } else {
        WaitlistFormContent(uiState = uiState, viewModel = viewModel, onBack = onBack)
    }
}

@Composable
private fun WaitlistFormContent(
    uiState: WaitlistUiState,
    viewModel: WaitlistViewModel,
    onBack: () -> Unit,
) {
    val dimens = LocalDimens.current
    val isDark = isSystemInDarkTheme()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dimens.logoPaddingVertical),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Image(
                painter = painterResource(
                    if (isDark) R.drawable.ic_white_logo else R.drawable.ic_black_logo,
                ),
                contentDescription = "RiNX logo",
                modifier = Modifier
                    .height(dimens.logoHeight)
                    .aspectRatio(4f)
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenPaddingHorizontal),
        ) {
            Text(
                text = "Join Our Waitlist",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Be among the first to experience RINX.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            WaitlistTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                modifier = Modifier.fillMaxWidth(),
                maxChars = 100,
                keyboardType = KeyboardType.Email,
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            PhoneNumberField(
                rawPhone = uiState.rawPhone,
                onPhoneChange = viewModel::onPhoneChange,
                selectedCountry = uiState.selectedCountry,
                onCountryChange = viewModel::onCountryChange,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            WaitlistTextField(
                value = uiState.firstName,
                onValueChange = viewModel::onFirstNameChange,
                label = "First Name",
                modifier = Modifier.fillMaxWidth(),
                maxChars = 50,
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            ProfileTypeDropdown(
                selected = uiState.profileType,
                onSelect = viewModel::onProfileTypeChange,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            WaitlistTextField(
                value = uiState.instagramHandle,
                onValueChange = viewModel::onInstagramHandleChange,
                label = "IG Handle (optional)",
                modifier = Modifier.fillMaxWidth(),
                maxChars = 30,
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = "If you were given a code, input it below:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            WaitlistTextField(
                value = uiState.referralCode,
                onValueChange = viewModel::onReferralCodeChange,
                label = "Enter Code",
                modifier = Modifier.fillMaxWidth(),
                maxChars = 20,
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            WaitlistCheckbox(
                checked = uiState.acceptedTerms,
                onCheckedChange = viewModel::onAcceptedTermsChange,
                text = buildAnnotatedString {
                    append("I accept the ")
                    withStyle(style = SpanStyle(color = BrandPrimary)) {
                        append("Terms and Conditions")
                    }
                    append(" & ")
                    withStyle(style = SpanStyle(color = BrandPrimary)) {
                        append("Privacy Policy")
                    }
                },
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            WaitlistCheckbox(
                checked = uiState.smsOptIn,
                onCheckedChange = viewModel::onSmsOptInChange,
                text = buildAnnotatedString {
                    append("Receive SMS notifications about RINX updates (optional)")
                },
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            AnimatedVisibility(visible = uiState.errorMessage != null) {
                Column {
                    Text(
                        text = uiState.errorMessage ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
            }

            val joinColor by animateColorAsState(
                targetValue = if (uiState.isJoinEnabled) BrandPrimary else InactiveButton,
                animationSpec = tween(durationMillis = 200),
                label = "joinButtonColor",
            )

            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.onJoin()
                },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.authButtonHeight),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = joinColor,
                    disabledContainerColor = InactiveButton,
                    contentColor = Color.White,
                    disabledContentColor = Color.White.copy(alpha = 0.7f),
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Spacing.xl),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "Join",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AvatarRow()
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Join 2,000+ others who signed up",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(dimens.screenPaddingBottom))
        }
    }
}

@Composable
private fun AvatarRow(modifier: Modifier = Modifier) {
    val avatarColors = listOf(
        Color(0xFF45B1E8),
        Color(0xFF8B5CF6),
        Color(0xFFEC4899),
        Color(0xFFF59E0B),
        Color(0xFF10B981),
    )
    val avatarSize = Spacing.xxl
    val step = Spacing.md
    val totalWidth = avatarSize + step * (avatarColors.size - 1)

    Box(modifier = modifier.size(width = totalWidth, height = avatarSize)) {
        avatarColors.forEachIndexed { index, color ->
            Box(
                modifier = Modifier
                    .offset(x = step * index)
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(color)
                    .border(width = 2.dp, color = MaterialTheme.colorScheme.background, shape = CircleShape),
            )
        }
    }
}

@Composable
private fun WaitlistSuccessContent(onBack: () -> Unit) {
    val dimens = LocalDimens.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dimens.logoPaddingVertical),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
            Image(
                painter = painterResource(R.drawable.ic_white_logo),
                contentDescription = "RiNX logo",
                modifier = Modifier
                    .height(dimens.logoHeight)
                    .aspectRatio(4f)
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = dimens.screenPaddingHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "You've joined the waitlist!",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = "What's next? Follow us on our socials to stay up to date.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Spacing.xxxl))

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SocialIconButton(iconRes = null, label = "𝕏")
                SocialIconButton(iconRes = R.drawable.ic_instagram, contentDescription = "Instagram")
                SocialIconButton(iconRes = R.drawable.ic_linkedin, contentDescription = "LinkedIn")
            }
        }
    }
}

@Composable
private fun SocialIconButton(
    iconRes: Int?,
    label: String = "",
    contentDescription: String = label,
) {
    Box(
        modifier = Modifier
            .size(Spacing.giant)
            .clip(RoundedCornerShape(Spacing.md))
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center,
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(Spacing.xl),
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }
    }
}
