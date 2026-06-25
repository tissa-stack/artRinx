package com.rinx.artRINXapp.feature.settings.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.feature.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoggingOut: Boolean = false,
    val loggedOut: Boolean = false,
    val isDeleting: Boolean = false,
    val deleted: Boolean = false,
    val deleteError: String? = null,
    /** False for phone-only accounts → the account row reads "Add email" instead of "Change email". */
    val hasEmail: Boolean = true,
    /** True when the account has a phone → the row reads "Change Phone"; false → "Add phone". */
    val hasPhone: Boolean = true,
    /** Current contact values, shown as the row's trailing text (parity with iOS). */
    val currentEmail: String = "",
    val currentPhone: String = "",
    /** Displayed at the bottom of Settings as "App Version X". */
    val appVersion: String = "",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            hasEmail = !authRepository.getEmail().isNullOrBlank(),
            hasPhone = !authRepository.getPhone().isNullOrBlank(),
            currentEmail = authRepository.getEmail().orEmpty(),
            currentPhone = authRepository.getPhone().orEmpty(),
            appVersion = readAppVersion(),
        ),
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    /** Re-read email/phone presence — call on resume so the rows reflect changes made elsewhere
     *  (e.g. "Add email" flips to "Change email"; "Add phone" flips to "Change Phone"). */
    fun refreshContactState() {
        _state.update {
            it.copy(
                hasEmail = !authRepository.getEmail().isNullOrBlank(),
                hasPhone = !authRepository.getPhone().isNullOrBlank(),
                currentEmail = authRepository.getEmail().orEmpty(),
                currentPhone = authRepository.getPhone().orEmpty(),
            )
        }
    }

    private fun readAppVersion(): String = try {
        "V" + (context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0")
    } catch (_: Exception) {
        "V1.0"
    }

    fun logout() {
        if (_state.value.isLoggingOut) return
        _state.update { it.copy(isLoggingOut = true) }
        viewModelScope.launch {
            authRepository.logout()   // revokes server-side (best effort) + wipes local session
            _state.update { it.copy(isLoggingOut = false, loggedOut = true) }
        }
    }

    /** Settings → "Sign out of all devices": revoke every refresh token server-side, then wipe local. */
    fun signOutEverywhere() {
        if (_state.value.isLoggingOut) return
        _state.update { it.copy(isLoggingOut = true) }
        viewModelScope.launch {
            authRepository.signOutEverywhere() // best-effort server revoke + full local wipe (clearAll)
            _state.update { it.copy(isLoggingOut = false, loggedOut = true) }
        }
    }

    fun deleteAccount() {
        if (_state.value.isDeleting) return
        _state.update { it.copy(isDeleting = true, deleteError = null) }
        viewModelScope.launch {
            when (authRepository.deleteAccount()) {
                is com.rinx.artRINXapp.core.network.ApiResult.Success ->
                    _state.update { it.copy(isDeleting = false, deleted = true) }
                is com.rinx.artRINXapp.core.network.ApiResult.Error ->
                    _state.update {
                        it.copy(isDeleting = false, deleteError = "Couldn't delete your account. Please try again.")
                    }
            }
        }
    }

    fun onDeleteErrorShown() = _state.update { it.copy(deleteError = null) }
}
