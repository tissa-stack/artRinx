package com.example.artrinx.feature.auth.presentation.otp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.auth.data.remote.dto.ConsentsDto
import com.example.artrinx.feature.auth.data.remote.dto.OtpRequest
import com.example.artrinx.feature.auth.data.remote.dto.OtpVerifyRequest
import com.example.artrinx.feature.auth.domain.model.AuthMode
import com.example.artrinx.feature.auth.domain.model.ContactType
import com.example.artrinx.feature.auth.domain.usecase.ResendOtpUseCase
import com.example.artrinx.feature.auth.domain.usecase.SaveSessionUseCase
import com.example.artrinx.feature.auth.domain.usecase.VerifyOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OtpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val verifyOtp: VerifyOtpUseCase,
    private val resendOtp: ResendOtpUseCase,
    private val saveSession: SaveSessionUseCase,
) : ViewModel() {

    val mode: String = savedStateHandle.get<String>("mode") ?: "signin"
    val contactType: String = savedStateHandle.get<String>("contactType") ?: ContactType.EMAIL.name
    val contactValue: String = savedStateHandle.get<String>("contactValue") ?: ""
    val inviteCode: String = savedStateHandle.get<String>("inviteCode") ?: ""

    private val _uiState = MutableStateFlow(OtpUiState())
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    init {
        startCooldown(60)
    }

    fun onOtpChange(value: String) {
        val filtered = value.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(otp = filtered, errorMessage = null) }
        if (filtered.length == 6) onVerify()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onVerify() {
        val state = _uiState.value
        if (!state.isContinueEnabled) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val isEmail = contactType == ContactType.EMAIL.name
            val isSignup = mode == AuthMode.SIGNUP.apiValue
            val request = OtpVerifyRequest(
                code = state.otp,
                mode = mode,
                email = if (isEmail) contactValue else null,
                phone = if (!isEmail) contactValue else null,
                // invite_code + consents are signup-only (omitted for signin by Gson null-skip)
                inviteCode = if (isSignup) inviteCode.ifBlank { null } else null,
                consents = if (isSignup) ConsentsDto(
                    acceptedTerms = true,
                    sms2faConsent = true,
                    accountNotificationSms = true,
                    marketingSmsConsent = false,
                ) else null,
            )
            when (val result = verifyOtp(request)) {
                is ApiResult.Success -> {
                    saveSession(result.data)
                    // Existing users (a profile already exists) go straight Home; only brand-new
                    // signups without a profile go to the completion flow. profile_completed is
                    // computed strictly by the backend and is false even for usable profiles.
                    if (result.data.user.profileExists) {
                        _uiState.update { it.copy(isLoading = false, navigateToHome = true) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, navigateToProfileCompletion = true) }
                    }
                }
                is ApiResult.Error.Blocked ->
                    _uiState.update { it.copy(isLoading = false, isBlocked = true) }
                is ApiResult.Error.Validation ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message, otp = "") }
                is ApiResult.Error.RateLimited ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message, otp = "") }
                is ApiResult.Error.NotFound ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message, otp = "") }
                is ApiResult.Error.Conflict ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message, otp = "") }
                is ApiResult.Error.Network ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "No internet connection. Please try again.") }
                is ApiResult.Error.Server ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Something went wrong. Please try again later.") }
                is ApiResult.Error.Unknown ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "An unexpected error occurred.") }
            }
        }
    }

    fun onResend() {
        if (!_uiState.value.canResend) return
        viewModelScope.launch {
            _uiState.update { it.copy(isResending = true, errorMessage = null) }
            val isEmail = contactType == ContactType.EMAIL.name
            val request = OtpRequest(
                mode = mode,
                email = if (isEmail) contactValue else null,
                phone = if (!isEmail) contactValue else null,
                inviteCode = inviteCode.ifBlank { null },
            )
            when (val result = resendOtp(request)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isResending = false, codeResent = true) }
                    startCooldown(60)
                }
                is ApiResult.Error.RateLimited -> {
                    _uiState.update { it.copy(isResending = false, errorMessage = result.message) }
                    startCooldown(result.retryAfterSeconds)
                }
                is ApiResult.Error.Validation ->
                    _uiState.update { it.copy(isResending = false, errorMessage = result.message) }
                is ApiResult.Error.Blocked ->
                    _uiState.update { it.copy(isResending = false, errorMessage = result.message) }
                is ApiResult.Error.Network ->
                    _uiState.update { it.copy(isResending = false, errorMessage = "No internet connection. Please try again.") }
                is ApiResult.Error.Server ->
                    _uiState.update { it.copy(isResending = false, errorMessage = "Something went wrong. Please try again later.") }
                else ->
                    _uiState.update { it.copy(isResending = false, errorMessage = "Failed to resend code. Please try again.") }
            }
        }
    }

    fun onCodeResentShown() = _uiState.update { it.copy(codeResent = false) }

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
}
