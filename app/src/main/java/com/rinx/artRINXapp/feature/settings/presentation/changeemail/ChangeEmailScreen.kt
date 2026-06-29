package com.rinx.artRINXapp.feature.settings.presentation.changeemail

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.settings.presentation.components.SettingsOtpContent

@Composable
fun ChangeEmailScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: ChangeEmailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val d = LocalDimens.current
    val context = LocalContext.current

    LaunchedEffect(uiState.done) {
        if (uiState.done) {
            val msg = if (uiState.isAdding) "Email added" else "Email updated"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
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
        if (uiState.step == ChangeEmailStep.OTP) {
            // Centered code screen (logo + Cancel/Verify) — no top bar.
            SettingsOtpContent(
                contact = uiState.newEmail,
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
                text = if (uiState.isAdding) "Add email" else "Change email",
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

            EmailStep(
                currentEmail = uiState.currentEmail,
                newEmail = uiState.newEmail,
                confirmEmail = uiState.confirmEmail,
                isSubmitting = uiState.isSubmitting,
                onNewEmailChange = viewModel::onNewEmailChange,
                onConfirmEmailChange = viewModel::onConfirmEmailChange,
                onSendCode = viewModel::onSendCode,
                buttonHeight = d.authButtonHeight,
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
        }
    }
}

@Composable
private fun EmailStep(
    currentEmail: String,
    newEmail: String,
    confirmEmail: String,
    isSubmitting: Boolean,
    onNewEmailChange: (String) -> Unit,
    onConfirmEmailChange: (String) -> Unit,
    onSendCode: () -> Unit,
    buttonHeight: androidx.compose.ui.unit.Dp,
) {
    // Open the keyboard on the "New email" field as soon as the screen appears, ready to type.
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        delay(150)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    if (currentEmail.isNotBlank()) {
        Text(
            text = "Current email",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = currentEmail,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.xl))
    }

    Text(
        text = "New email",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.xs))
    OutlinedTextField(
        value = newEmail,
        onValueChange = onNewEmailChange,
        singleLine = true,
        placeholder = { Text("you@example.com") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        shape = RoundedCornerShape(Spacing.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandPrimary,
            cursorColor = BrandPrimary,
        ),
    )
    Spacer(Modifier.height(Spacing.md))

    // Re-enter to confirm — must match before "Send code" is allowed (parity with iOS).
    Text(
        text = "Confirm new email",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.xs))
    OutlinedTextField(
        value = confirmEmail,
        onValueChange = onConfirmEmailChange,
        singleLine = true,
        placeholder = { Text("you@example.com") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BrandPrimary,
            cursorColor = BrandPrimary,
        ),
    )
    Spacer(Modifier.height(Spacing.xl))

    Button(
        onClick = onSendCode,
        enabled = newEmail.isNotBlank() && confirmEmail.isNotBlank() && !isSubmitting,
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

