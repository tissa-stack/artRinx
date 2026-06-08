package com.rinx.artRINXapp.feature.settings.presentation.titleplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.util.ProfileRefreshBus
import com.rinx.artRINXapp.feature.auth.data.local.SessionDataSource
import com.rinx.artRINXapp.feature.auth.domain.repository.AuthRepository
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import com.rinx.artRINXapp.feature.settings.domain.model.MockSettingsData
import com.rinx.artRINXapp.feature.settings.domain.model.PlanOption
import com.rinx.artRINXapp.feature.settings.domain.model.ProfileTitleOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileTitleAndPlanUiState(
    val title: ProfileTitleOption? = null,
    val plan: PlanOption? = null,
    val nextBillingDate: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val isDeleting: Boolean = false,
    val deleted: Boolean = false,
    val deleteError: String? = null,
)

@HiltViewModel
class ProfileTitleAndPlanViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val session: SessionDataSource,
    private val profileRefreshBus: ProfileRefreshBus,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileTitleAndPlanUiState())
    val state: StateFlow<ProfileTitleAndPlanUiState> = _state.asStateFlow()

    init {
        load()
        observeRefreshes()
    }

    /** Reload after the title is changed in the edit screen (updateProfile signals the bus). */
    private fun observeRefreshes() {
        viewModelScope.launch {
            profileRefreshBus.events.collect { load() }
        }
    }

    private fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.getProfilePlanSummary()) {
                is ApiResult.Success -> {
                    val summary = result.data
                    _state.update {
                        it.copy(
                            title = currentTitleOption(session.getUserRole(), summary.profileTitle),
                            // No paid purchase flow yet → everyone starts on the free basic plan.
                            plan = if (summary.isPremium) MockSettingsData.premiumPlan else MockSettingsData.basicPlan,
                            nextBillingDate = summary.nextBillingDate,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
                is ApiResult.Error -> _state.update {
                    it.copy(isLoading = false, error = result.toMessage())
                }
            }
        }
    }

    fun onRetry() = load()

    fun deleteAccount() {
        if (_state.value.isDeleting) return
        _state.update { it.copy(isDeleting = true, deleteError = null) }
        viewModelScope.launch {
            when (val r = authRepository.deleteAccount()) {
                is ApiResult.Success -> _state.update { it.copy(isDeleting = false, deleted = true) }
                is ApiResult.Error -> _state.update {
                    it.copy(isDeleting = false, deleteError = r.toDeleteMessage())
                }
            }
        }
    }
}

private fun ApiResult.Error.toMessage(): String = when (this) {
    is ApiResult.Error.Network -> "No connection. Please try again."
    is ApiResult.Error.Server -> "Something went wrong. Please try again."
    else -> "Couldn't load your plan. Please try again."
}

private fun ApiResult.Error.toDeleteMessage(): String = when (this) {
    is ApiResult.Error.Network -> "No connection. Please try again."
    is ApiResult.Error.Validation -> message
    is ApiResult.Error.Server -> "Something went wrong. Please try again."
    else -> "Couldn't delete your account. Please try again."
}
