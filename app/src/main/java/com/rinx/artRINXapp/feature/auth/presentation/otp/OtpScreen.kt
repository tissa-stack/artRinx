package com.rinx.artRINXapp.feature.auth.presentation.otp

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.ArtRinxTheme
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.auth.presentation.otp.components.OtpBoxRow

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

    val context = LocalContext.current
    LaunchedEffect(uiState.codeResent) {
        if (uiState.codeResent) {
            Toast.makeText(context, "Code sent", Toast.LENGTH_SHORT).show()
            viewModel.onCodeResentShown()
        }
    }

    ArtRinxTheme {
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
    val isDark = isSystemInDarkTheme()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Top bar: back arrow + logo ─────────────────────────────────
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
                    .padding(horizontal = dimens.screenPaddingHorizontal),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(Spacing.xxl))

                Text(
                    text = "Enter Code sent to $contactValue",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(Spacing.xxxl))

                OtpBoxRow(
                    otp = uiState.otp,
                    onOtpChange = onOtpChange,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                // ── Expires-in countdown ───────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Expires in: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatCountdown(uiState.resendCooldownSeconds),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

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

                Spacer(modifier = Modifier.height(Spacing.md))

                // ── Resend Code | Verify ───────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    OutlinedButton(
                        onClick = onResend,
                        enabled = uiState.canResend,
                        modifier = Modifier
                            .weight(1f)
                            .height(dimens.authButtonHeight),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BrandPrimary,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
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
                            Text(text = "Resend Code", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Button(
                        onClick = onVerify,
                        enabled = uiState.isContinueEnabled,
                        modifier = Modifier
                            .weight(1f)
                            .height(dimens.authButtonHeight),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimary,
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
                            Text(text = "Verify", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(dimens.screenPaddingBottom))
            }
        }

        if (uiState.isBlocked) {
            BlockedOverlay()
        }
    }
}

/** Formats remaining seconds as MM:SS. */
private fun formatCountdown(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}

@Composable
private fun BlockedOverlay() {
    val dimens = LocalDimens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = dimens.screenPaddingHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Access Restricted",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "Your account has been disabled. Please contact support for assistance.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
