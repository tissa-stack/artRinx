package com.rinx.artRINXapp.feature.auth.presentation.invite

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.auth.presentation.invite.components.InviteCodeTextField

@Composable
fun InviteCodeScreen(
    onNavigateToSignup: (inviteCode: String) -> Unit,
    onJoinWaitlist: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: InviteCodeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val dimens = LocalDimens.current
    val isDark = isSystemInDarkTheme()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.clearError()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateToSignup(uiState.inviteCode)
            // Consume the one-shot so returning here doesn't auto-bounce back to signup, and so a
            // later Continue can re-trigger navigation (isSuccess must change false→true again).
            viewModel.onSignupNavigated()
        }
    }

    // Agent (Gallery) codes: Gallery signups complete on the web — no in-app OTP path.
    // Anti-steering safe: "Open Website" carries no purchase verb.
    if (uiState.showGalleryWebModal) {
        AlertDialog(
            onDismissRequest = viewModel::dismissGalleryWebModal,
            title = { Text("Complete on web") },
            text = {
                Text("Gallery signups are completed on the web. Visit artrinx.com/gallery.")
            },
            confirmButton = {
                TextButton(onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "https://artrinx.com/gallery".toUri()),
                        )
                    }
                    viewModel.dismissGalleryWebModal()
                }) { Text("Open artrinx.com/gallery") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissGalleryWebModal) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
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
                        if (isDark) R.drawable.artrinx_logo_dark_theme else R.drawable.artrinx_logo_light_theme,
                    ),
                    contentDescription = "artRINX logo",
                    modifier = Modifier
                        .height(dimens.logoHeight),
                    contentScale = ContentScale.Fit,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxxl))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.screenPaddingHorizontal),
            ) {
                Text(
                    text = "Hi, welcome to artRINX!",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Input your invite below to sign up",
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

                Spacer(modifier = Modifier.height(Spacing.xxxl))

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
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = continueColor,
                        disabledContainerColor = InactiveButton,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Spacing.xl),
                            color = MaterialTheme.colorScheme.onPrimary,
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

                // Fixed, comfortable gap below Join Waitlist — the block stays right here under the
                // buttons (top-aligned content), it does NOT stretch to the bottom of the screen.
                Spacer(modifier = Modifier.height(Spacing.xxxl))

                Column(
                    modifier = Modifier.fillMaxWidth(),
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

            Spacer(modifier = Modifier.height(dimens.screenPaddingBottom))
        }
    }
}
