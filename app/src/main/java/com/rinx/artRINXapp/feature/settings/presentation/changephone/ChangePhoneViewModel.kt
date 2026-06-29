package com.rinx.artRINXapp.feature.settings.presentation.changephone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.phone.PhoneNumberValidator
import com.rinx.artRINXapp.core.phone.PhoneValidation
import com.rinx.artRINXapp.feature.auth.domain.repository.AuthRepository
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.CountryCode
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.CountryCodeProvider
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.CountryCodes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ChangePhoneStep { PHONE, OTP }

data class ChangePhoneUiState(
    val step: ChangePhoneStep = ChangePhoneStep.PHONE,
    val currentPhone: String = "",
    val selectedCountry: CountryCode = CountryCodes.default,
    /** Dial-code options; bundled fallback list, replaced by the master catalog once it loads. */
    val availableCountries: List<CountryCode> = CountryCodes.all,
    val rawPhone: String = "",
    /** libphonenumber length/validity of [rawPhone] for [selectedCountry]. */
    val phoneValidation: PhoneValidation = PhoneValidation.EMPTY,
    /** Max digits typeable for [selectedCountry] (caps the phone input). */
    val phoneMaxDigits: Int = 15,
    val otp: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val codeResent: Boolean = false,
    val done: Boolean = false,
    /** Seconds left before the code expires / Resend re-enables (0 = can resend). */
    val resendCooldownSeconds: Int = 0,
) {
    val canResend: Boolean get() = resendCooldownSeconds == 0

    /** The new number in E.164 (dial code + entered digits), e.g. "+9198…". */
    val newPhoneE164: String get() = selectedCountry.dialCode + rawPhone
}

/**
 * Change the account's phone number (§1.8). Two steps: send an OTP to the new number
 * (`startChangePhone`), then verify it (`confirmChangePhone`). Mirrors [ChangeEmailViewModel].
 * The screen is only reachable when the account already has a phone, so this is change-only.
 */
@HiltViewModel
class ChangePhoneViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val countryCodeProvider: CountryCodeProvider,
    private val phoneNumberValidator: PhoneNumberValidator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChangePhoneUiState(currentPhone = authRepository.getPhone().orEmpty()),
    )
    val uiState: StateFlow<ChangePhoneUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

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

    /** Step 1 → request an OTP to the new number, then advance to the code step. */
    fun onSendCode() {
        val state = _uiState.value
        val newPhone = state.newPhoneE164
        when {
            state.phoneValidation != PhoneValidation.OK ->
                return _uiState.update { it.copy(errorMessage = "Enter a valid phone number.") }
            newPhone == state.currentPhone ->
                return _uiState.update { it.copy(errorMessage = "That's already your number.") }
        }
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            when (val res = authRepository.startChangePhone(newPhone)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, step = ChangePhoneStep.OTP, otp = "") }
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

    /** Step 2 → verify the OTP; on success the phone is changed (repo adopts any fresh tokens). */
    fun onVerify() {
        val state = _uiState.value
        if (state.otp.length < OTP_LENGTH || state.isSubmitting) return
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            when (val res = authRepository.confirmChangePhone(state.newPhoneE164, state.otp)) {
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
            when (val res = authRepository.startChangePhone(state.newPhoneE164)) {
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
