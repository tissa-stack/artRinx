package com.rinx.artRINXapp.feature.settings.presentation.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileUpdate
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhonePermissionsUiState(
    val isLoading: Boolean = true,
    val marketingSmsConsent: Boolean = false,
    val isSaving: Boolean = false,
    /** One-shot error for a failed toggle write; cleared via [consumeError]. */
    val error: String? = null,
)

@HiltViewModel
class PhonePermissionsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PhonePermissionsUiState())
    val state: StateFlow<PhonePermissionsUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            when (val res = profileRepository.getEditableProfile()) {
                is ApiResult.Success -> _state.update {
                    it.copy(isLoading = false, marketingSmsConsent = res.data.marketingSmsConsent)
                }
                is ApiResult.Error -> _state.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Marketing-SMS reads and writes the SAME field (`marketing_sms_consent`) — the V1.9 fix.
     * Optimistically flips the switch, persists via PUT /api/profile/update, reverts on failure.
     */
    fun setMarketingSmsConsent(enabled: Boolean) {
        val previous = _state.value.marketingSmsConsent
        if (previous == enabled || _state.value.isSaving) return
        _state.update { it.copy(marketingSmsConsent = enabled, isSaving = true, error = null) }
        viewModelScope.launch {
            when (profileRepository.updateProfile(ProfileUpdate(marketingSmsConsent = enabled), newPictureUri = null)) {
                is ApiResult.Success -> _state.update { it.copy(isSaving = false) }
                is ApiResult.Error -> _state.update {
                    it.copy(marketingSmsConsent = previous, isSaving = false, error = "Couldn't update — please try again.")
                }
            }
        }
    }

    fun consumeError() = _state.update { it.copy(error = null) }
}
