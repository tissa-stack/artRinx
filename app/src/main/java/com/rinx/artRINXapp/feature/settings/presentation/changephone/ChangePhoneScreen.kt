package com.rinx.artRINXapp.feature.settings.presentation.changephone

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.core.ui.AppToast
import com.rinx.artRINXapp.feature.auth.presentation.otp.components.OtpBoxRow
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.CountryCode
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.components.PhoneNumberField

@Composable
fun ChangePhoneScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: ChangePhoneViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val d = LocalDimens.current
    val context = LocalContext.current

    LaunchedEffect(uiState.done) {
        if (uiState.done) {
            AppToast.show("Phone updated")
            onDone()
        }
    }
    LaunchedEffect(uiState.codeResent) {
        if (uiState.codeResent) {
            AppToast.show("Code sent")
            viewModel.onCodeResentShown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // ── Header ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.xs, end = Spacing.md, top = Spacing.xs, bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = "Change phone number",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(Spacing.huge))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = d.screenPaddingHorizontal),
        ) {
            Spacer(Modifier.height(Spacing.xl))

            when (uiState.step) {
                ChangePhoneStep.PHONE -> PhoneStep(
                    currentPhone = uiState.currentPhone,
                    rawPhone = uiState.rawPhone,
                    selectedCountry = uiState.selectedCountry,
                    isSubmitting = uiState.isSubmitting,
                    onRawPhoneChange = viewModel::onRawPhoneChange,
                    onCountryChange = viewModel::onCountryChange,
                    onSendCode = viewModel::onSendCode,
                    buttonHeight = d.authButtonHeight,
                )
                ChangePhoneStep.OTP -> OtpStep(
                    newPhone = uiState.newPhoneE164,
                    otp = uiState.otp,
                    isSubmitting = uiState.isSubmitting,
                    cooldownSeconds = uiState.resendCooldownSeconds,
                    canResend = uiState.canResend,
                    onOtpChange = viewModel::onOtpChange,
                    onVerify = viewModel::onVerify,
                    onResend = viewModel::onResend,
                    buttonHeight = d.authButtonHeight,
                )
            }

            AnimatedVisibility(visible = uiState.errorMessage != null) {
                Column {
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        text = uiState.errorMessage ?: "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneStep(
    currentPhone: String,
    rawPhone: String,
    selectedCountry: CountryCode,
    isSubmitting: Boolean,
    onRawPhoneChange: (String) -> Unit,
    onCountryChange: (CountryCode) -> Unit,
    onSendCode: () -> Unit,
    buttonHeight: androidx.compose.ui.unit.Dp,
) {
    if (currentPhone.isNotBlank()) {
        Text(
            text = "Current number",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = currentPhone,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.xl))
    }

    Text(
        text = "New number",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.xs))
    PhoneNumberField(
        rawPhone = rawPhone,
        onPhoneChange = onRawPhoneChange,
        selectedCountry = selectedCountry,
        onCountryChange = onCountryChange,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(Spacing.xl))

    Button(
        onClick = onSendCode,
        enabled = rawPhone.isNotBlank() && !isSubmitting,
        modifier = Modifier
            .fillMaxWidth()
            .height(buttonHeight),
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
            Text("Send code", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun OtpStep(
    newPhone: String,
    otp: String,
    isSubmitting: Boolean,
    cooldownSeconds: Int,
    canResend: Boolean,
    onOtpChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    buttonHeight: androidx.compose.ui.unit.Dp,
) {
    Text(
        text = "Enter the code sent to $newPhone",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(Spacing.xl))

    OtpBoxRow(otp = otp, onOtpChange = onOtpChange, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(Spacing.lg))

    // ── Resend countdown (this timer gates Resend, not code expiry) ─────
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Resend in: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatCountdown(cooldownSeconds),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
    Spacer(Modifier.height(Spacing.lg))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        OutlinedButton(
            onClick = onResend,
            enabled = canResend && !isSubmitting,
            modifier = Modifier
                .weight(1f)
                .height(buttonHeight),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = BrandPrimary,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            ),
            border = BorderStroke(
                Spacing.xs / 4,
                if (canResend && !isSubmitting) BrandPrimary else MaterialTheme.colorScheme.outline,
            ),
        ) {
            Text("Resend code", style = MaterialTheme.typography.labelLarge)
        }

        Button(
            onClick = onVerify,
            enabled = otp.length == 6 && !isSubmitting,
            modifier = Modifier
                .weight(1f)
                .height(buttonHeight),
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
}

/** Formats remaining seconds as MM:SS. */
private fun formatCountdown(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}
