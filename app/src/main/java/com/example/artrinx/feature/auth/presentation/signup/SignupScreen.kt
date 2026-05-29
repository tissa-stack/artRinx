package com.example.artrinx.feature.auth.presentation.signup

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.artrinx.R
import com.example.artrinx.core.navigation.OtpArgs
import com.example.artrinx.core.theme.ArtRinxTheme
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.InactiveButton
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.auth.domain.model.ContactType
import com.example.artrinx.feature.auth.presentation.waitlist.components.PhoneNumberField
import com.example.artrinx.feature.auth.presentation.waitlist.components.WaitlistCheckbox
import com.example.artrinx.feature.auth.presentation.waitlist.components.WaitlistTextField

@Composable
fun SignupScreen(
    onBack: () -> Unit,
    onNavigateToOtp: (OtpArgs) -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SignupViewModel = hiltViewModel(),
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

    LaunchedEffect(uiState.navigateToOtp) {
        uiState.navigateToOtp?.let { args ->
            onNavigateToOtp(args)
            viewModel.onOtpNavigated()
        }
    }

    ArtRinxTheme(darkTheme = true) {
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
    onCountryChange: (com.example.artrinx.feature.auth.presentation.waitlist.CountryCode) -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
) {
    val dimens = LocalDimens.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenPaddingHorizontal),
        ) {
            Spacer(modifier = Modifier.height(Spacing.xxl))

            Text(
                text = "Let's get started",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Let's create your account and start exploring the art world together.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.55f),
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
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = if (uiState.contactType == ContactType.EMAIL) "Sign up with phone" else "Sign up with email",
                style = MaterialTheme.typography.bodySmall,
                color = BrandPrimary,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { onContactTypeToggle() }
                    .padding(vertical = Spacing.xs),
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            WaitlistCheckbox(
                checked = uiState.acceptedTerms,
                onCheckedChange = onTermsChange,
                text = buildAnnotatedString {
                    append("By continuing, I agree to the Opt-in, ")
                    withStyle(style = SpanStyle(color = BrandPrimary)) { append("Terms and Conditions") }
                    append(", ")
                    withStyle(style = SpanStyle(color = BrandPrimary)) { append("Privacy Policy") }
                    append(", and ")
                    withStyle(style = SpanStyle(color = BrandPrimary)) { append("Community Guidelines") }
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
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.authButtonHeight),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = continueColor,
                    disabledContainerColor = InactiveButton,
                    contentColor = Color.White,
                    disabledContentColor = Color.White.copy(alpha = 0.7f),
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = Spacing.xs * 0),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Spacing.xl),
                        color = Color.White,
                        strokeWidth = Spacing.xs / 2,
                    )
                } else {
                    Text(text = "Continue", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "Google Sign In coming soon", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.authButtonHeight),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(
                    Spacing.xs / 4,
                    MaterialTheme.colorScheme.outline,
                ),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "G",
                        style = MaterialTheme.typography.labelLarge.copy(color = BrandPrimary),
                    )
                    Text(
                        text = "Continue with Google",
                        style = MaterialTheme.typography.labelLarge,
                    )
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
                color = Color.White.copy(alpha = 0.55f),
            )
            Text(
                text = "Sign in",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .clickable(onClick = onNavigateToLogin)
                    .padding(horizontal = Spacing.xs),
            )
        }
    }
}
