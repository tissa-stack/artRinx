package com.rinx.artRINXapp.feature.auth.presentation.signup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.navigation.OtpArgs
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpRequest
import com.rinx.artRINXapp.feature.auth.domain.model.AuthMode
import com.rinx.artRINXapp.feature.auth.domain.model.ContactType
import com.rinx.artRINXapp.feature.auth.domain.usecase.RequestOtpUseCase
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.CountryCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val requestOtp: RequestOtpUseCase,
) : ViewModel() {

    private val inviteCode: String = savedStateHandle.get<String>("inviteCode") ?: ""

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    fun onContactTypeToggle() {
        _uiState.update {
            val next = if (it.contactType == ContactType.EMAIL) ContactType.PHONE else ContactType.EMAIL
            it.copy(contactType = next, errorMessage = null)
        }
    }

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, errorMessage = null) }

    fun onPhoneChange(value: String) =
        _uiState.update { it.copy(rawPhone = value, errorMessage = null) }

    fun onCountryChange(value: CountryCode) =
        _uiState.update { it.copy(selectedCountry = value) }

    fun onTermsChange(value: Boolean) =
        _uiState.update { it.copy(acceptedTerms = value, errorMessage = null) }

    fun onOtpNavigated() =
        _uiState.update { it.copy(navigateToOtp = null) }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onContinue() {
        val state = _uiState.value
        if (!state.isContinueEnabled) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val contactValue = when (state.contactType) {
                ContactType.EMAIL -> state.email.trim()
                ContactType.PHONE -> state.selectedCountry.dialCode + state.rawPhone.trim()
            }
            val request = OtpRequest(
                mode = AuthMode.SIGNUP.apiValue,
                email = if (state.contactType == ContactType.EMAIL) state.email.trim() else null,
                phone = if (state.contactType == ContactType.PHONE) contactValue else null,
                inviteCode = inviteCode.ifBlank { null },
            )
            when (val result = requestOtp(request)) {
                is ApiResult.Success -> {
                    val args = OtpArgs(
                        mode = AuthMode.SIGNUP.apiValue,
                        contactType = state.contactType.name,
                        contactValue = contactValue,
                        inviteCode = inviteCode,
                    )
                    _uiState.update { it.copy(isLoading = false, navigateToOtp = args) }
                }
                is ApiResult.Error.Conflict ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is ApiResult.Error.NotFound ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is ApiResult.Error.Blocked ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is ApiResult.Error.RateLimited ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is ApiResult.Error.Validation ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                is ApiResult.Error.Network ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "No internet connection. Please try again.") }
                is ApiResult.Error.Server ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Something went wrong. Please try again later.") }
                is ApiResult.Error.Unknown ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = "An unexpected error occurred.") }
            }
        }
    }
}
