package com.rinx.artRINXapp.feature.auth.presentation.otp

data class OtpUiState(
    val otp: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isBlocked: Boolean = false,
    val resendCooldownSeconds: Int = 0,
    val isResending: Boolean = false,
    val codeResent: Boolean = false,
    val navigateToHome: Boolean = false,
    val navigateToProfileCompletion: Boolean = false,
)

val OtpUiState.isOtpComplete: Boolean get() = otp.length == 6
val OtpUiState.isContinueEnabled: Boolean get() = isOtpComplete && !isLoading
val OtpUiState.canResend: Boolean get() = resendCooldownSeconds == 0 && !isResending && !isLoading
