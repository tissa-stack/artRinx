package com.rinx.artRINXapp.feature.auth.presentation.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.ArtRinxTheme
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * Login method chooser shown when the user taps "Continue to login". Offers Continue with Email,
 * Continue with Google, and a "Signed up with phone?" link. Email/phone navigate to the single-mode
 * [LoginScreen]; Google sign-in is handled here via [LoginViewModel] and routes to Home (existing
 * user) or profile completion (new account).
 */
@Composable
fun LoginOptionsScreen(
    onBack: () -> Unit,
    onContinueWithEmail: () -> Unit,
    onContinueWithPhone: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToProfileCompletion: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
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

    LaunchedEffect(uiState.navigateToHome) {
        if (uiState.navigateToHome) onNavigateToHome()
    }
    LaunchedEffect(uiState.navigateToProfileCompletion) {
        if (uiState.navigateToProfileCompletion) onNavigateToProfileCompletion()
    }

    ArtRinxTheme {
        LoginOptionsContent(
            uiState = uiState,
            onBack = onBack,
            onContinueWithEmail = onContinueWithEmail,
            onContinueWithPhone = onContinueWithPhone,
            onGoogleSignIn = { viewModel.onGoogleSignIn(context) },
        )
    }
}

@Composable
private fun LoginOptionsContent(
    uiState: LoginUiState,
    onBack: () -> Unit,
    onContinueWithEmail: () -> Unit,
    onContinueWithPhone: () -> Unit,
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
                contentDescription = "artRinx logo",
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
                text = "Welcome back",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "We missed you! Pick how you'd like to sign in.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(Spacing.xxxl))

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

            // Continue with Email — primary filled action.
            Button(
                onClick = onContinueWithEmail,
                enabled = !uiState.isGoogleLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.authButtonHeight),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = BrandPrimary.copy(alpha = 0.5f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = Spacing.xs * 0),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MailOutline,
                        contentDescription = null,
                        modifier = Modifier.size(Spacing.xl),
                    )
                    Text(text = "Continue with Email", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // "or" divider.
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

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Continue with Google.
            OutlinedButton(
                onClick = onGoogleSignIn,
                enabled = !uiState.isGoogleLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.authButtonHeight),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ),
                border = BorderStroke(
                    Spacing.xs / 4,
                    MaterialTheme.colorScheme.outline,
                ),
            ) {
                if (uiState.isGoogleLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Spacing.xl),
                        color = MaterialTheme.colorScheme.onBackground,
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

            Spacer(modifier = Modifier.height(Spacing.xxxl))

            Text(
                text = "Signed up with phone?",
                style = MaterialTheme.typography.labelMedium,
                color = BrandPrimary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable(enabled = !uiState.isGoogleLoading, onClick = onContinueWithPhone)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            )

            Spacer(modifier = Modifier.height(dimens.screenPaddingBottom))
        }
    }
}
