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
    /** False for phone-only accounts → the account row reads "Add email" instead of "Change email". */
    val hasEmail: Boolean = true,
    /** True when the account has a phone → show the "Change phone number" row (hidden for email-only). */
    val hasPhone: Boolean = true,
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
            appVersion = readAppVersion(),
        ),
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    /** Re-read email/phone presence — call on resume so the rows reflect changes made elsewhere
     *  (e.g. "Add email" flips to "Change email"; the new number shows after a phone change). */
    fun refreshContactState() {
        _state.update {
            it.copy(
                hasEmail = !authRepository.getEmail().isNullOrBlank(),
                hasPhone = !authRepository.getPhone().isNullOrBlank(),
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
}
