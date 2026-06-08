package com.rinx.artRINXapp.feature.settings.presentation.changeemail

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ChangeEmailStep { EMAIL, OTP }

data class ChangeEmailUiState(
    val step: ChangeEmailStep = ChangeEmailStep.EMAIL,
    val currentEmail: String = "",
    val newEmail: String = "",
    val otp: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val codeResent: Boolean = false,
    val done: Boolean = false,
    /** True when the account has no email yet (phone signup) — we ADD rather than change. */
    val isAdding: Boolean = false,
    /** Seconds left before the code expires / Resend re-enables (0 = can resend). */
    val resendCooldownSeconds: Int = 0,
) {
    val canResend: Boolean get() = resendCooldownSeconds == 0
}

@HiltViewModel
class ChangeEmailViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    // No email on the account (phone signup) → ADD a first email; otherwise CHANGE the existing one.
    private val isAdding = authRepository.getEmail().isNullOrBlank()

    private val _uiState = MutableStateFlow(
        ChangeEmailUiState(
            currentEmail = authRepository.getEmail().orEmpty(),
            isAdding = isAdding,
        ),
    )
    val uiState: StateFlow<ChangeEmailUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    fun onNewEmailChange(value: String) =
        _uiState.update { it.copy(newEmail = value.trim(), errorMessage = null) }

    /** Step 1 → request an OTP to the new email, then advance to the code step. */
    fun onSendCode() {
        val state = _uiState.value
        val email = state.newEmail.trim()
        when {
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                return _uiState.update { it.copy(errorMessage = "Enter a valid email address.") }
            email.equals(state.currentEmail, ignoreCase = true) ->
                return _uiState.update { it.copy(errorMessage = "That's already your email.") }
        }
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val res = if (isAdding) authRepository.startAddEmail(email)
            else authRepository.startChangeEmail(email)
            when (res) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, step = ChangeEmailStep.OTP, otp = "") }
                    startCooldown(OTP_TTL_SECONDS)
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = res.toMessage())
                }
            }
        }
    }

    fun onOtpChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(OTP_LENGTH)
        _uiState.update { it.copy(otp = digits, errorMessage = null) }
        if (digits.length == OTP_LENGTH) onVerify()
    }

    /** Step 2 → verify the OTP; on success the email is changed (repo adopts any fresh tokens). */
    fun onVerify() {
        val state = _uiState.value
        if (state.otp.length < OTP_LENGTH || state.isSubmitting) return
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val res = if (isAdding) authRepository.confirmAddEmail(state.newEmail, state.otp)
            else authRepository.confirmChangeEmail(state.newEmail, state.otp)
            when (res) {
                is ApiResult.Success -> _uiState.update { it.copy(isSubmitting = false, done = true) }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isSubmitting = false, otp = "", errorMessage = res.toMessage())
                }
            }
        }
    }

    fun onResend() {
        val state = _uiState.value
        if (state.isSubmitting || !state.canResend) return
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val res = if (isAdding) authRepository.startAddEmail(state.newEmail)
            else authRepository.startChangeEmail(state.newEmail)
            when (res) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, codeResent = true, otp = "") }
                    startCooldown(OTP_TTL_SECONDS)
                }
                is ApiResult.Error.RateLimited -> {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = res.toMessage()) }
                    startCooldown(res.retryAfterSeconds)
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = res.toMessage())
                }
            }
        }
    }

    fun onCodeResentShown() = _uiState.update { it.copy(codeResent = false) }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    private fun startCooldown(seconds: Int) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            _uiState.update { it.copy(resendCooldownSeconds = seconds) }
            repeat(seconds) {
                delay(1_000)
                _uiState.update { it.copy(resendCooldownSeconds = maxOf(0, it.resendCooldownSeconds - 1)) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cooldownJob?.cancel()
    }

    private fun ApiResult.Error.toMessage(): String = when (this) {
        is ApiResult.Error.Validation -> message
        is ApiResult.Error.Blocked -> message
        is ApiResult.Error.NotFound -> message
        is ApiResult.Error.Conflict -> message
        is ApiResult.Error.RateLimited -> "Too many attempts. Try again in ${retryAfterSeconds}s."
        is ApiResult.Error.Network -> "No internet connection. Please try again."
        is ApiResult.Error.Server -> "Server error. Please try again."
        is ApiResult.Error.Unknown -> "Something went wrong. Please try again."
    }

    private companion object {
        const val OTP_LENGTH = 6
        const val OTP_TTL_SECONDS = 60
    }
}
