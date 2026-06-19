package com.rinx.artRINXapp.feature.auth.presentation.waitlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.auth.data.remote.dto.WaitlistRequest
import com.rinx.artRINXapp.feature.auth.domain.model.ProfileType
import com.rinx.artRINXapp.feature.auth.domain.usecase.JoinWaitlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WaitlistViewModel @Inject constructor(
    private val joinWaitlist: JoinWaitlistUseCase,
    private val countryCodeProvider: CountryCodeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WaitlistUiState())
    val uiState: StateFlow<WaitlistUiState> = _uiState.asStateFlow()

    init {
        loadCountryCodes()
    }

    /**
     * Replace the bundled fallback dial-code list with the full master catalog (loaded via
     * [CountryCodeProvider], which falls back to the bundled list when offline). The current
     * selection is re-pointed at the matching catalog entry (same ISO2) so the dial code is stable.
     */
    private fun loadCountryCodes() {
        viewModelScope.launch {
            val countries = countryCodeProvider.load()
            _uiState.update { state ->
                val selected = countries.firstOrNull { it.code == state.selectedCountry.code }
                    ?: countries.firstOrNull { it.code == CountryCodes.default.code }
                    ?: countries.firstOrNull()
                    ?: state.selectedCountry
                state.copy(availableCountries = countries, selectedCountry = selected)
            }
        }
    }

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, errorMessage = null) }

    fun onPhoneChange(value: String) =
        _uiState.update { it.copy(rawPhone = value, errorMessage = null) }

    fun onCountryChange(value: CountryCode) =
        _uiState.update { it.copy(selectedCountry = value) }

    fun onFirstNameChange(value: String) =
        _uiState.update { it.copy(firstName = value, errorMessage = null) }

    fun onProfileTypeChange(value: ProfileType) =
        _uiState.update { it.copy(profileType = value, errorMessage = null) }

    fun onInstagramHandleChange(value: String) =
        _uiState.update { it.copy(instagramHandle = value) }

    fun onReferralCodeChange(value: String) =
        _uiState.update { it.copy(referralCode = value) }

    fun onAcceptedTermsChange(value: Boolean) =
        _uiState.update { it.copy(acceptedTerms = value, errorMessage = null) }

    fun onSmsOptInChange(value: Boolean) =
        _uiState.update { it.copy(smsOptIn = value) }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onJoin() {
        val state = _uiState.value
        if (!state.isJoinEnabled) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val request = WaitlistRequest(
                email = state.email.trim(),
                phoneNumber = state.selectedCountry.dialCode + state.rawPhone.trim(),
                firstName = state.firstName.trim(),
                profileTypeId = state.profileType!!.id,
                instagramHandle = state.instagramHandle.trim().ifBlank { null },
                acceptedTerms = state.acceptedTerms,
                smsNotificationsOptIn = state.smsOptIn,
                deviceType = "android",
            )
            when (val result = joinWaitlist(request)) {
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
        }
    }
}
