package com.rinx.artRINXapp.feature.settings.presentation.addphone

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.core.util.LegalLinks
import com.rinx.artRINXapp.core.util.appendLegalLink
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.components.PhoneNumberField
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.components.WaitlistCheckbox
import com.rinx.artRINXapp.feature.settings.presentation.components.SettingsOtpContent

@Composable
fun AddPhoneScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: AddPhoneViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val d = LocalDimens.current
    val context = LocalContext.current

    LaunchedEffect(uiState.done) {
        if (uiState.done) {
            Toast.makeText(context, "Phone added", Toast.LENGTH_SHORT).show()
            onDone()
        }
    }
    LaunchedEffect(uiState.codeResent) {
        if (uiState.codeResent) {
            Toast.makeText(context, "Code sent", Toast.LENGTH_SHORT).show()
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
        if (uiState.step == AddPhoneStep.OTP) {
            SettingsOtpContent(
                contact = uiState.newPhoneE164,
                otp = uiState.otp,
                isSubmitting = uiState.isSubmitting,
                cooldownSeconds = uiState.resendCooldownSeconds,
                canResend = uiState.canResend,
                errorMessage = uiState.errorMessage,
                onOtpChange = viewModel::onOtpChange,
                onVerify = viewModel::onVerify,
                onResend = viewModel::onResend,
                onCancel = onBack,
            )
            return@Column
        }

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
                text = "Add phone",
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = d.screenPaddingHorizontal),
        ) {
            Spacer(Modifier.height(Spacing.lg))

            Text(
                text = "Add phone number",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "We'll send a one-time code to verify your number.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = "Enter your phone number",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xs))
            PhoneNumberField(
                rawPhone = uiState.rawPhone,
                onPhoneChange = viewModel::onRawPhoneChange,
                selectedCountry = uiState.selectedCountry,
                onCountryChange = viewModel::onCountryChange,
                countries = uiState.availableCountries,
                searchable = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Spacing.lg))

            // ── Consents ─────────────────────────────────────────────────
            val linkStyle = SpanStyle(color = BrandPrimary, textDecoration = TextDecoration.Underline)
            WaitlistCheckbox(
                checked = uiState.acceptedTerms,
                onCheckedChange = viewModel::onAcceptTermsChange,
                text = buildAnnotatedString {
                    append("I accept the ")
                    appendLegalLink("Terms and Conditions", LegalLinks.TERMS_OF_USE, linkStyle)
                    append(" & ")
                    appendLegalLink("Privacy Policy", LegalLinks.PRIVACY_POLICY, linkStyle)
                    append(".")
                },
            )
            WaitlistCheckbox(
                checked = uiState.sms2faConsent,
                onCheckedChange = viewModel::onSms2faChange,
                text = AnnotatedString(
                    "I consent to receive 2FA notifications and verification codes via SMS from artRinx at the number provided.",
                ),
            )
            WaitlistCheckbox(
                checked = uiState.accountNotificationSms,
                onCheckedChange = viewModel::onAccountNotificationChange,
                text = AnnotatedString("I consent to receive account notifications from artRinx!"),
            )

            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "For all SMS communications above: Message frequency varies. Msg & data rates may " +
                    "apply. Reply HELP for help and STOP to opt-out at any time.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

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

            Spacer(Modifier.height(Spacing.xl))

            Button(
                onClick = viewModel::onSendCode,
                enabled = uiState.canSendCode,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(d.authButtonHeight),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    disabledContainerColor = InactiveButton,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                ),
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Spacing.xl),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = Spacing.xs / 2,
                    )
                } else {
                    Text("Send code", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}
