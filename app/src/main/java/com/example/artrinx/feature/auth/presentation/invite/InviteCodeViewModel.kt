package com.example.artrinx.feature.auth.presentation.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.auth.domain.usecase.VerifyInviteCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InviteCodeViewModel @Inject constructor(
    private val verifyInviteCode: VerifyInviteCodeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteCodeUiState())
    val uiState: StateFlow<InviteCodeUiState> = _uiState.asStateFlow()

    fun onCodeChange(code: String) {
        _uiState.update { it.copy(inviteCode = code, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onContinue() {
        val code = _uiState.value.inviteCode.trim()
        if (code.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // ── TESTING BYPASS ────────────────────────────────────────────────
            // Accept "123456" without hitting the API.
            if (code == "123456") {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                return@launch
            }
            // ─────────────────────────────────────────────────────────────────

            /*
            when (val result = verifyInviteCode(code)) {
                is ApiResult.Success ->
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                is ApiResult.Error.Network ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "No internet connection. Please try again.") }
                is ApiResult.Error.Validation ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is ApiResult.Error.Blocked ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is ApiResult.Error.NotFound ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is ApiResult.Error.Conflict ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is ApiResult.Error.RateLimited ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is ApiResult.Error.Server ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Something went wrong. Please try again later.") }
                is ApiResult.Error.Unknown ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "An unexpected error occurred.") }
            }
            */

            _uiState.update { it.copy(isLoading = false, errorMessage = "Invalid invite code. Use 123456 for testing.") }
        }
    }
}
