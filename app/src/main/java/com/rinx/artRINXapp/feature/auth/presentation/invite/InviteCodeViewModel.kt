package com.rinx.artRINXapp.feature.auth.presentation.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.navigation.DeepLinkRouter
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.auth.domain.model.InviteCodeType
import com.rinx.artRINXapp.feature.auth.domain.usecase.VerifyInviteCodeUseCase
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
    deepLinkRouter: DeepLinkRouter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteCodeUiState())
    val uiState: StateFlow<InviteCodeUiState> = _uiState.asStateFlow()

    init {
        // Deep links (rinxart://invite/<CODE>, https://artrinx.com/invite/<CODE>) pre-fill the field.
        deepLinkRouter.consumeInviteCode()?.let { code ->
            _uiState.update { it.copy(inviteCode = code) }
        }
    }

    fun onCodeChange(code: String) {
        _uiState.update { it.copy(inviteCode = code, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissGalleryWebModal() {
        _uiState.update { it.copy(showGalleryWebModal = false) }
    }

    fun onContinue() {
        val code = _uiState.value.inviteCode.trim()
        if (code.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = verifyInviteCode(code)) {
                is ApiResult.Success -> when (result.data.codeType) {
                    // Gallery (agent) accounts are completed on the web — never proceed to OTP.
                    InviteCodeType.AGENT ->
                        _uiState.update { it.copy(isLoading = false, showGalleryWebModal = true) }
                    // peer / admin / unknown (legacy) → continue to signup.
                    else ->
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
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
        }
    }
}
