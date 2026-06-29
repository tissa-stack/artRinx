package com.rinx.artRINXapp.feature.auth.presentation.signup

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.auth.google.GoogleAuthClient
import com.rinx.artRINXapp.core.auth.google.GoogleSignInResult
import com.rinx.artRINXapp.core.navigation.OtpArgs
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.phone.PhoneNumberValidator
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpVerifyResponse
import com.rinx.artRINXapp.feature.auth.domain.GooglePrefillHolder
import com.rinx.artRINXapp.feature.auth.domain.model.AuthMode
import com.rinx.artRINXapp.feature.auth.domain.model.ContactType
import com.rinx.artRINXapp.feature.auth.domain.usecase.RequestOtpUseCase
import com.rinx.artRINXapp.feature.auth.domain.usecase.SaveSessionUseCase
import com.rinx.artRINXapp.feature.auth.domain.usecase.SignInWithGoogleUseCase
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.CountryCode
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.CountryCodeProvider
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.CountryCodes
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
    private val googleAuthClient: GoogleAuthClient,
    private val signInWithGoogle: SignInWithGoogleUseCase,
    private val saveSession: SaveSessionUseCase,
    private val googlePrefillHolder: GooglePrefillHolder,
    private val countryCodeProvider: CountryCodeProvider,
    private val phoneNumberValidator: PhoneNumberValidator,
) : ViewModel() {

    private val inviteCode: String = savedStateHandle.get<String>("inviteCode") ?: ""

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    init {
        loadCountryCodes()
    }

    /** Populate the phone-code picker from the master catalog (falls back to the bundled list). */
    private fun loadCountryCodes() {
        viewModelScope.launch {
            val countries = countryCodeProvider.load()
            _uiState.update { state ->
                // Default the picker to the device's country (SIM/network/locale); fall back to US.
                val deviceCode = phoneNumberValidator.deviceRegion()
                val selected = countries.firstOrNull { it.code == deviceCode }
                    ?: countries.firstOrNull { it.code == CountryCodes.default.code }
                    ?: countries.firstOrNull()
                    ?: state.selectedCountry
                state.copy(
                    availableCountries = countries,
                    selectedCountry = selected,
                    phoneMaxDigits = phoneNumberValidator.maxNationalDigits(selected),
                )
            }
        }
    }

    fun onContactTypeToggle() {
        _uiState.update {
            val next = if (it.contactType == ContactType.EMAIL) ContactType.PHONE else ContactType.EMAIL
            it.copy(contactType = next, errorMessage = null)
        }
    }

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, errorMessage = null) }

    fun onPhoneChange(value: String) =
        _uiState.update {
            it.copy(
                rawPhone = value,
                phoneValidation = phoneNumberValidator.validate(it.selectedCountry, value),
                errorMessage = null,
            )
        }

    fun onCountryChange(value: CountryCode) =
        _uiState.update {
            it.copy(
                selectedCountry = value,
                phoneValidation = phoneNumberValidator.validate(value, it.rawPhone),
                phoneMaxDigits = phoneNumberValidator.maxNationalDigits(value),
            )
        }

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

    /** "Continue with Google": run the account chooser, exchange the ID token, then route. */
    fun onGoogleSignIn(activityContext: Context) {
        val state = _uiState.value
        if (state.isLoading || state.isGoogleLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGoogleLoading = true, errorMessage = null) }
            when (val result = googleAuthClient.getResult(activityContext)) {
                is GoogleSignInResult.Success -> handleGoogleToken(result)
                GoogleSignInResult.Cancelled ->
                    _uiState.update { it.copy(isGoogleLoading = false) }
                GoogleSignInResult.NoCredential ->
                    _uiState.update { it.copy(isGoogleLoading = false, errorMessage = "No Google account found on this device.") }
                GoogleSignInResult.PlayServicesUnavailable ->
                    _uiState.update { it.copy(isGoogleLoading = false, errorMessage = "Google sign-in isn't available on this device.") }
                is GoogleSignInResult.Failure ->
                    _uiState.update { it.copy(isGoogleLoading = false, errorMessage = "Couldn't sign in with Google. Please try again.") }
            }
        }
    }

    private suspend fun handleGoogleToken(google: GoogleSignInResult.Success) {
        // Signup is invite-gated: forward the verified invite code so the backend can create a new
        // account (it 400s `invite_required` without it). Existing users sign in and the code is ignored.
        when (val result = signInWithGoogle(google.idToken, inviteCode.ifBlank { null })) {
            is ApiResult.Success -> {
                saveSession(result.data)
                routeAfterGoogle(google, result.data)
            }
            is ApiResult.Error.Blocked ->
                _uiState.update { it.copy(isGoogleLoading = false, errorMessage = result.message) }
            is ApiResult.Error.Validation ->
                _uiState.update { it.copy(isGoogleLoading = false, errorMessage = result.message) }
            is ApiResult.Error.Conflict ->
                _uiState.update { it.copy(isGoogleLoading = false, errorMessage = result.message) }
            is ApiResult.Error.NotFound ->
                _uiState.update { it.copy(isGoogleLoading = false, errorMessage = result.message) }
            is ApiResult.Error.RateLimited ->
                _uiState.update { it.copy(isGoogleLoading = false, errorMessage = result.message) }
            is ApiResult.Error.Network ->
                _uiState.update { it.copy(isGoogleLoading = false, errorMessage = "No internet connection. Please try again.") }
            is ApiResult.Error.Server ->
                _uiState.update { it.copy(isGoogleLoading = false, errorMessage = "Something went wrong. Please try again later.") }
            is ApiResult.Error.Unknown ->
                _uiState.update { it.copy(isGoogleLoading = false, errorMessage = "An unexpected error occurred.") }
        }
    }

    private fun routeAfterGoogle(google: GoogleSignInResult.Success, response: OtpVerifyResponse) {
        if (response.user.profileExists) {
            // Existing user → straight Home. Ensure no stale prefill lingers for a later signup.
            googlePrefillHolder.clear()
            _uiState.update { it.copy(isGoogleLoading = false, navigateToHome = true) }
        } else {
            // Brand-new account → seed the profile-completion wizard with the Google name + photo.
            googlePrefillHolder.set(
                GooglePrefillHolder.Data(
                    fullName = google.displayName,
                    givenName = google.givenName,
                    familyName = google.familyName,
                    photoUrl = google.photoUrl,
                ),
            )
            _uiState.update { it.copy(isGoogleLoading = false, navigateToProfileCompletion = true) }
        }
    }
}
