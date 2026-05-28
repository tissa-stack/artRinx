package com.example.artrinx.feature.auth.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.artrinx.R
import com.example.artrinx.core.theme.ArtRinxTheme
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.InactiveButton
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.auth.presentation.components.InviteCodeTextField

@Composable
fun InviteCodeScreen(
    onNavigateToHome: () -> Unit,
    onJoinWaitlist: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: InviteCodeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val dimens = LocalDimens.current
    val isDark = isSystemInDarkTheme()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onNavigateToHome()
    }

    // Outer column: handles system bar insets + keyboard avoidance
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // ── SCROLLABLE SECTION ──────────────────────────────────────────────
        // weight(1f) ensures the scroll area never pushes the bottom link off screen
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Logo ─────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimens.logoPaddingHorizontal,
                        vertical = dimens.logoPaddingVertical,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(
                        if (isDark) R.drawable.ic_white_logo else R.drawable.ic_black_logo,
                    ),
                    contentDescription = "RiNX logo",
                    modifier = Modifier
                        .height(dimens.logoHeight)
                        .aspectRatio(4f),
                    contentScale = ContentScale.Fit,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxxl))

            // ── Form ─────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.screenPaddingHorizontal),
            ) {
                Text(
                    text = "Hi, welcome to RINX!",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Input your invite code below to get started.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(Spacing.xxl))

                InviteCodeTextField(
                    value = uiState.inviteCode,
                    onValueChange = viewModel::onCodeChange,
                    hasError = uiState.errorMessage != null,
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.onContinue()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Animated error message below the field
                AnimatedVisibility(visible = uiState.errorMessage != null) {
                    Column {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = Spacing.sm),
                        )
                    }
                }

                // Larger gap between field and action buttons
                Spacer(modifier = Modifier.height(Spacing.xxxl))

                // Continue button — smoothly animates between brand blue (active) and gray (inactive)
                val continueColor by animateColorAsState(
                    targetValue = if (uiState.isSubmitEnabled) BrandPrimary else InactiveButton,
                    animationSpec = tween(durationMillis = 200),
                    label = "continueButtonColor",
                )

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.onContinue()
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimens.authButtonHeight),
                    // Pill shape — 50% = fully rounded ends
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = continueColor,
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
                            text = "Continue",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Join Waitlist outlined pill button
                OutlinedButton(
                    onClick = onJoinWaitlist,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimens.authButtonHeight),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                ) {
                    Text(
                        text = "Join Waitlist",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            // Breathing room at the bottom of the scrollable area
            Spacer(modifier = Modifier.height(Spacing.xxxl))
        }

        // ── BOTTOM LINK — always visible, sits above the keyboard ───────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = dimens.screenPaddingBottom),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Already have an account?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Continue to login",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clickable(onClick = onNavigateToLogin)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            )
        }
    }
}

@Preview(name = "Invite Code — Empty", showBackground = true)
@Composable
private fun InviteCodeEmptyPreview() {
    ArtRinxTheme {
        InviteCodeScreen(onNavigateToHome = {}, onJoinWaitlist = {}, onNavigateToLogin = {})
    }
}

@Preview(name = "Invite Code — Dark Empty", showBackground = true)
@Composable
private fun InviteCodeDarkPreview() {
    ArtRinxTheme(darkTheme = true) {
        InviteCodeScreen(onNavigateToHome = {}, onJoinWaitlist = {}, onNavigateToLogin = {})
    }
}
