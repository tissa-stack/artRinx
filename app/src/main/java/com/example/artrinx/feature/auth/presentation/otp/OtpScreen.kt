package com.example.artrinx.feature.auth.presentation.otp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.artrinx.R
import com.example.artrinx.core.theme.ArtRinxTheme
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.InactiveButton
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.auth.presentation.otp.components.OtpBoxRow

@Composable
fun OtpScreen(
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToProfileCompletion: () -> Unit,
    viewModel: OtpViewModel = hiltViewModel(),
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

    LaunchedEffect(uiState.navigateToHome) {
        if (uiState.navigateToHome) onNavigateToHome()
    }
    LaunchedEffect(uiState.navigateToProfileCompletion) {
        if (uiState.navigateToProfileCompletion) onNavigateToProfileCompletion()
    }

    ArtRinxTheme(darkTheme = true) {
        OtpContent(
            uiState = uiState,
            contactValue = viewModel.contactValue,
            onBack = onBack,
            onOtpChange = viewModel::onOtpChange,
            onVerify = viewModel::onVerify,
            onResend = viewModel::onResend,
        )
    }
}

@Composable
private fun OtpContent(
    uiState: OtpUiState,
    contactValue: String,
    onBack: () -> Unit,
    onOtpChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
) {
    val dimens = LocalDimens.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
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
            ) {
                Spacer(modifier = Modifier.height(Spacing.xxl))

                Text(
                    text = "Enter code",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Please enter the 6-digit code sent to",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = contactValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(Spacing.xxxl))

                OtpBoxRow(
                    otp = uiState.otp,
                    onOtpChange = onOtpChange,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                AnimatedVisibility(visible = uiState.errorMessage != null) {
                    Column {
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                val continueColor by animateColorAsState(
                    targetValue = if (uiState.isContinueEnabled) BrandPrimary else InactiveButton,
                    animationSpec = tween(durationMillis = 200),
                    label = "otpContinueColor",
                )

                Button(
                    onClick = onVerify,
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

                val resendLabel = if (uiState.resendCooldownSeconds > 0) {
                    "Resend Code (${uiState.resendCooldownSeconds}s)"
                } else {
                    "Resend Code"
                }

                OutlinedButton(
                    onClick = onResend,
                    enabled = uiState.canResend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimens.authButtonHeight),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BrandPrimary,
                        disabledContentColor = Color.White.copy(alpha = 0.3f),
                    ),
                    border = BorderStroke(
                        Spacing.xs / 4,
                        if (uiState.canResend) BrandPrimary else MaterialTheme.colorScheme.outline,
                    ),
                ) {
                    if (uiState.isResending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Spacing.lg),
                            color = BrandPrimary,
                            strokeWidth = Spacing.xs / 2,
                        )
                    } else {
                        Text(text = resendLabel, style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                Text(
                    text = "Change email/phone?",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(vertical = Spacing.xs),
                )

                Spacer(modifier = Modifier.height(dimens.screenPaddingBottom))
            }
        }

        if (uiState.isBlocked) {
            BlockedOverlay()
        }
    }
}

@Composable
private fun BlockedOverlay() {
    val dimens = LocalDimens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = dimens.screenPaddingHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Access Restricted",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "Your account has been disabled. Please contact support for assistance.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
        )
    }
}
