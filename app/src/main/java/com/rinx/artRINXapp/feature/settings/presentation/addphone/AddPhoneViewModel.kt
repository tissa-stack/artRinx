package com.rinx.artRINXapp.feature.settings.presentation.addphone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.phone.PhoneNumberValidator
import com.rinx.artRINXapp.core.phone.PhoneValidation
import com.rinx.artRINXapp.feature.auth.domain.repository.AuthRepository
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.CountryCode
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.CountryCodeProvider
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.CountryCodes
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileUpdate
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AddPhoneStep { PHONE, OTP }

data class AddPhoneUiState(
    val step: AddPhoneStep = AddPhoneStep.PHONE,
    val selectedCountry: CountryCode = CountryCodes.default,
    val availableCountries: List<CountryCode> = CountryCodes.all,
    val rawPhone: String = "",
    /** libphonenumber length/validity of [rawPhone] for [selectedCountry]. */
    val phoneValidation: PhoneValidation = PhoneValidation.EMPTY,
    /** Max digits typeable for [selectedCountry] (caps the phone input). */
    val phoneMaxDigits: Int = 15,
    val otp: String = "",
    // Consents (parity with iOS Add-phone). T&C is a required gate; the two SMS consents are persisted.
    val acceptedTerms: Boolean = false,
    val sms2faConsent: Boolean = false,
    val accountNotificationSms: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val codeResent: Boolean = false,
    val done: Boolean = false,
    val resendCooldownSeconds: Int = 0,
) {
    val canResend: Boolean get() = resendCooldownSeconds == 0
    val newPhoneE164: String get() = selectedCountry.dialCode + rawPhone
    val canSendCode: Boolean get() = phoneValidation == PhoneValidation.OK && acceptedTerms && !isSubmitting
}

/**
 * Add a FIRST phone to a phone-less account. Two steps: send an OTP to the new number
 * (`startAddPhone`), then verify it (`confirmAddPhone`). On success the SMS consents are persisted via
 * the profile update. Mirrors [com.rinx.artRINXapp.feature.settings.presentation.changephone.ChangePhoneViewModel].
 */
@HiltViewModel
class AddPhoneViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val countryCodeProvider: CountryCodeProvider,
    private val phoneNumberValidator: PhoneNumberValidator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddPhoneUiState())
    val uiState: StateFlow<AddPhoneUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    init {
        loadCountryCodes()
    }

    private fun loadCountryCodes() {
        viewModelScope.launch {
            val countries = countryCodeProvider.load()
            _uiState.update { state ->
                val selected = countries.firstOrNull { it.code == state.selectedCountry.code }
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

    fun onCountryChange(country: CountryCode) =
        _uiState.update {
            it.copy(
                selectedCountry = country,
                phoneValidation = phoneNumberValidator.validate(country, it.rawPhone),
                phoneMaxDigits = phoneNumberValidator.maxNationalDigits(country),
                errorMessage = null,
            )
        }

    fun onRawPhoneChange(value: String) =
        _uiState.update {
            val digits = value.filter { c -> c.isDigit() }.take(15)
            it.copy(
                rawPhone = digits,
                phoneValidation = phoneNumberValidator.validate(it.selectedCountry, digits),
                errorMessage = null,
            )
        }

    fun onAcceptTermsChange(v: Boolean) = _uiState.update { it.copy(acceptedTerms = v) }
    fun onSms2faChange(v: Boolean) = _uiState.update { it.copy(sms2faConsent = v) }
    fun onAccountNotificationChange(v: Boolean) = _uiState.update { it.copy(accountNotificationSms = v) }

    fun onSendCode() {
        val state = _uiState.value
        if (!state.canSendCode) {
            return _uiState.update { it.copy(errorMessage = "Enter a valid number and accept the terms.") }
        }
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            when (val res = authRepository.startAddPhone(state.newPhoneE164)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, step = AddPhoneStep.OTP, otp = "") }
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

    fun onVerify() {
        val state = _uiState.value
        if (state.otp.length < OTP_LENGTH || state.isSubmitting) return
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            when (val res = authRepository.confirmAddPhone(state.newPhoneE164, state.otp)) {
                is ApiResult.Success -> {
                    // Phone attached → persist the SMS consents (best-effort; don't block on it).
                    profileRepository.updateProfile(
                        ProfileUpdate(
                            sms2faConsent = state.sms2faConsent,
                            accountNotificationSms = state.accountNotificationSms,
                        ),
                        newPictureUri = null,
                    )
                    _uiState.update { it.copy(isSubmitting = false, done = true) }
                }
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
            when (val res = authRepository.startAddPhone(state.newPhoneE164)) {
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
