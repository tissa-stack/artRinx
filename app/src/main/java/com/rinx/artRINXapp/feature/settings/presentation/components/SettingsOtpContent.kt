package com.rinx.artRINXapp.feature.settings.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.auth.presentation.otp.components.OtpBoxRow

/**
 * Shared OTP step for the Settings contact flows (change email / change phone / add phone). Vertically
 * centered: artRinx logo → "Enter the code" → "Please enter the 6-digit code sent to <contact>" (the
 * contact in brand blue) → OTP cells → "Didn't receive the code?" → a resend countdown that flips to a
 * "Resend code" link → Cancel + Verify. Reuses [OtpBoxRow] and the auth logo.
 */
@Composable
fun SettingsOtpContent(
    contact: String,
    otp: String,
    isSubmitting: Boolean,
    cooldownSeconds: Int,
    canResend: Boolean,
    errorMessage: String?,
    onOtpChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = LocalDimens.current
    val isDark = isSystemInDarkTheme()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = d.screenPaddingHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(
                if (isDark) R.drawable.artrinx_logo_dark_theme else R.drawable.artrinx_logo_light_theme,
            ),
            contentDescription = "artRinx logo",
            modifier = Modifier.height(d.logoHeight),
            contentScale = ContentScale.Fit,
        )

        Spacer(Modifier.height(Spacing.xxxl))

        Text(
            text = "Enter the code",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = "Please enter the 6-digit code sent to",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = contact,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = BrandPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Spacing.xxl))

        OtpBoxRow(otp = otp, onOtpChange = onOtpChange, modifier = Modifier.fillMaxWidth())

        AnimatedVisibility(visible = errorMessage != null) {
            Column {
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(Spacing.xl))

        Text(
            text = "Didn't receive the code?",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xs))
        if (canResend && !isSubmitting) {
            Text(
                text = "Resend code",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = BrandPrimary,
                modifier = Modifier.clickable(onClick = onResend),
            )
        } else {
            Text(
                text = "Resend code in ${formatCountdown(cooldownSeconds)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Spacing.xxxl))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            OutlinedButton(
                onClick = onCancel,
                enabled = !isSubmitting,
                modifier = Modifier
                    .weight(1f)
                    .height(d.authButtonHeight),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                border = BorderStroke(Spacing.xs / 4, MaterialTheme.colorScheme.outline),
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = onVerify,
                enabled = otp.length == 6 && !isSubmitting,
                modifier = Modifier
                    .weight(1f)
                    .height(d.authButtonHeight),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    disabledContainerColor = InactiveButton,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                ),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Spacing.xl),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = Spacing.xs / 2,
                    )
                } else {
                    Text("Verify", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Spacer(Modifier.height(Spacing.xxl))
    }
}

/** Formats remaining seconds as MM:SS. */
private fun formatCountdown(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}
