package com.rinx.artRINXapp.feature.auth.presentation.signup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.util.LegalLinks
import com.rinx.artRINXapp.core.util.appendLegalLink
import com.rinx.artRINXapp.core.navigation.OtpArgs
import com.rinx.artRINXapp.core.theme.ArtRinxTheme
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.auth.domain.model.ContactType
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.components.PhoneNumberField
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.components.WaitlistCheckbox
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.components.WaitlistTextField

@Composable
fun SignupScreen(
    onBack: () -> Unit,
    onNavigateToOtp: (OtpArgs) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToProfileCompletion: () -> Unit,
    viewModel: SignupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.clearError()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.navigateToOtp) {
        uiState.navigateToOtp?.let { args ->
            onNavigateToOtp(args)
            viewModel.onOtpNavigated()
        }
    }

    LaunchedEffect(uiState.navigateToHome) {
        if (uiState.navigateToHome) onNavigateToHome()
    }
    LaunchedEffect(uiState.navigateToProfileCompletion) {
        if (uiState.navigateToProfileCompletion) onNavigateToProfileCompletion()
    }

    ArtRinxTheme {
        SignupContent(
            uiState = uiState,
            onBack = onBack,
            onNavigateToLogin = onNavigateToLogin,
            onContactTypeToggle = viewModel::onContactTypeToggle,
            onEmailChange = viewModel::onEmailChange,
            onPhoneChange = viewModel::onPhoneChange,
            onCountryChange = viewModel::onCountryChange,
            onTermsChange = viewModel::onTermsChange,
            onContinue = viewModel::onContinue,
            onGoogleSignIn = { viewModel.onGoogleSignIn(context) },
        )
    }
}

@Composable
private fun SignupContent(
    uiState: SignupUiState,
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onContactTypeToggle: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onCountryChange: (com.rinx.artRINXapp.feature.auth.presentation.waitlist.CountryCode) -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
    onGoogleSignIn: () -> Unit,
) {
    val dimens = LocalDimens.current
    val isDark = isSystemInDarkTheme()

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
                    if (isDark) R.drawable.artrinx_logo_dark_theme else R.drawable.artrinx_logo_light_theme,
                ),
                contentDescription = "artRINX logo",
                modifier = Modifier
                    .height(dimens.logoHeight)
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
            Spacer(modifier = Modifier.height(Spacing.xxl))

            Text(
                text = "Let's get started",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Let's create your account and start exploring the art world together.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(Spacing.xxxl))

            when (uiState.contactType) {
                ContactType.EMAIL -> WaitlistTextField(
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    label = "Enter your email",
                    modifier = Modifier.fillMaxWidth(),
                    maxChars = 100,
                    keyboardType = KeyboardType.Email,
                )
                ContactType.PHONE -> PhoneNumberField(
                    rawPhone = uiState.rawPhone,
                    onPhoneChange = onPhoneChange,
                    selectedCountry = uiState.selectedCountry,
                    onCountryChange = onCountryChange,
                    countries = uiState.availableCountries,
                    searchable = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Phone signup is temporarily disabled — only email signup is offered for now, so the
            // "Sign up with phone" toggle is hidden. contactType stays EMAIL (the default).

            Spacer(modifier = Modifier.height(Spacing.lg))

            WaitlistCheckbox(
                checked = uiState.acceptedTerms,
                onCheckedChange = onTermsChange,
                text = buildAnnotatedString {
                    val linkStyle = SpanStyle(color = BrandPrimary)
                    append("I accept the ")
                    appendLegalLink("Community Guidelines", LegalLinks.COMMUNITY_GUIDELINES, linkStyle)
                    append(", ")
                    appendLegalLink("Terms and Conditions", LegalLinks.TERMS_OF_USE, linkStyle)
                    append(" & ")
                    appendLegalLink("Privacy Policy", LegalLinks.PRIVACY_POLICY, linkStyle)
                    append(".")
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

            val continueColor by animateColorAsState(
                targetValue = if (uiState.isContinueEnabled) BrandPrimary else InactiveButton,
                animationSpec = tween(durationMillis = 200),
                label = "signupContinueColor",
            )

            Button(
                onClick = onContinue,
                enabled = !uiState.isLoading && !uiState.isGoogleLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.authButtonHeight),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = continueColor,
                    disabledContainerColor = InactiveButton,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = Spacing.xs * 0),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Spacing.xl),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = Spacing.xs / 2,
                    )
                } else {
                    Text(text = "Continue", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // "or" divider before the Google option.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = "or",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedButton(
                onClick = onGoogleSignIn,
                // Sign-up requires accepting the terms — gate Google sign-up on the checkbox too.
                enabled = !uiState.isLoading && !uiState.isGoogleLoading && uiState.acceptedTerms,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.authButtonHeight),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                border = BorderStroke(
                    Spacing.xs / 4,
                    MaterialTheme.colorScheme.outline,
                ),
            ) {
                if (uiState.isGoogleLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Spacing.xl),
                        color = MaterialTheme.colorScheme.onSurface,
                        strokeWidth = Spacing.xs / 2,
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_google_logo),
                            contentDescription = null,
                            modifier = Modifier.size(Spacing.xl),
                        )
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimens.screenPaddingBottom))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimens.screenPaddingBottom),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Already have an account? ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Sign in",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clickable(onClick = onNavigateToLogin)
                    .padding(horizontal = Spacing.xs),
            )
        }
    }
}
