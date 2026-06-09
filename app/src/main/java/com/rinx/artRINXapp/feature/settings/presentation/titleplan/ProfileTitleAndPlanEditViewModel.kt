package com.rinx.artRINXapp.feature.settings.presentation.titleplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.auth.data.local.SessionDataSource
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileUpdate
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

data class TitlePlanEditUiState(
    val step: Int = 0,                       // 0 = title, 1 = plan
    val titles: List<ProfileTitleOption> = emptyList(),
    val plans: List<PlanOption> = emptyList(),
    val selectedTitleId: Int? = null,
    val selectedPlanId: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saved: Boolean = false,
) {
    val canAdvanceTitle: Boolean get() = selectedTitleId != null
    val canSavePlan: Boolean get() = selectedPlanId != null && !isSaving
}

@HiltViewModel
class ProfileTitleAndPlanEditViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val session: SessionDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(
        TitlePlanEditUiState(
            // Change-Role picker shows Artist / Collector / Art Curious only — Gallery is web-managed
            // and never selectable on mobile (handout §Change Role picker rule).
            titles = MockSettingsData.profileTitles.filterNot { it.name.contains("gallery", ignoreCase = true) },
            plans = MockSettingsData.plans,
        ),
    )
    val state: StateFlow<TitlePlanEditUiState> = _state.asStateFlow()

    /** The title the user currently has — used to detect a real change before calling the API. */
    private var originalTitleId: Int? = null

    init {
        load()
    }

    private fun load() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val role = session.getUserRole()
            // Default to the user's live title; fall back to the onboarding role if the fetch fails.
            val result = repository.getProfilePlanSummary()
            val current = when (result) {
                is ApiResult.Success -> currentTitleOption(role, result.data.profileTitle)
                is ApiResult.Error -> currentTitleOption(role, "")
            }
            val planId = when (result) {
                is ApiResult.Success -> if (result.data.isPremium) MockSettingsData.premiumPlan.id else MockSettingsData.basicPlan.id
                is ApiResult.Error -> MockSettingsData.basicPlan.id
            }
            originalTitleId = current.id.takeIf { it != 0 }
            _state.update {
                it.copy(
                    selectedTitleId = current.id.takeIf { id -> id != 0 } ?: it.selectedTitleId,
                    selectedPlanId = planId,
                    isLoading = false,
                )
            }
        }
    }

    /** One-time init of the starting step (title vs plan) from nav arg. */
    fun setInitialStep(step: Int) = _state.update {
        if (it.step == 0 && step in 0..1) it.copy(step = step) else it
    }

    fun onTitleSelected(id: Int) = _state.update { it.copy(selectedTitleId = id, saveError = null) }
    fun onPlanSelected(id: String) = _state.update { it.copy(selectedPlanId = id) }

    fun goToPlanStep() = _state.update { it.copy(step = 1) }
    fun goToTitleStep() = _state.update { it.copy(step = 0) }

    /**
     * Persist the changes. Only the profile title is backed by an API (`profile_type_id`); the plan
     * has no purchase endpoint yet, so a plan-only change just closes the screen. If the title is
     * unchanged we skip the network call entirely.
     */
    fun onSave() {
        val newTitleId = _state.value.selectedTitleId
        if (newTitleId == null || newTitleId == 0 || newTitleId == originalTitleId) {
            _state.update { it.copy(saved = true) }
            return
        }
        _state.update { it.copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            when (val result = repository.updateProfile(ProfileUpdate(profileTypeId = newTitleId), null)) {
                is ApiResult.Success -> {
                    // Keep the cached role in sync so other screens reflect the new title.
                    roleForTitleId(newTitleId)?.let { session.saveUserRole(it) }
                    originalTitleId = newTitleId
                    _state.update { it.copy(isSaving = false, saved = true) }
                }
                is ApiResult.Error -> _state.update {
                    it.copy(isSaving = false, saveError = result.toMessage())
                }
            }
        }
    }

    fun onSaveHandled() = _state.update { it.copy(saved = false, saveError = null) }
}

private fun ApiResult.Error.toMessage(): String = when (this) {
    is ApiResult.Error.Network -> "No connection. Please try again."
    is ApiResult.Error.Validation -> message
    is ApiResult.Error.Server -> "Something went wrong. Please try again."
    else -> "Couldn't save changes — please try again."
}
