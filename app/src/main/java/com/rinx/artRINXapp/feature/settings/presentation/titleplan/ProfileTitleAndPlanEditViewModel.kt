package com.rinx.artRINXapp.feature.settings.presentation.titleplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.auth.data.local.SessionDataSource
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileUpdate
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import com.rinx.artRINXapp.feature.settings.domain.model.MockSettingsData
import com.rinx.artRINXapp.feature.settings.domain.model.PlanCatalog
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
    // For the plan-card CTA state machine.
    val role: String = "",
    val isPaid: Boolean = false,
    val currentPlanId: String = "basic",
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
            // Plans are resolved from PlanCatalog in load() once the role is known.
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
            val roleStr = role.orEmpty()
            // Default to the user's live title; fall back to the onboarding role if the fetch fails.
            val result = repository.getProfilePlanSummary()
            // Fetch the real roles (authoritative id + name); descriptions/badges come from local copy.
            val typesResult = repository.getProfileTypes()
            val current = when (result) {
                is ApiResult.Success -> currentTitleOption(role, result.data.profileTitle)
                is ApiResult.Error -> currentTitleOption(role, "")
            }
            val isPaid = (result as? ApiResult.Success)?.data?.isPremium ?: false
            val fetchedTypes = (typesResult as? ApiResult.Success)?.data
            val titles = if (!fetchedTypes.isNullOrEmpty()) {
                fetchedTypes
                    .filterNot { it.name.contains("gallery", ignoreCase = true) } // web-managed
                    .map { type ->
                        val copy = MockSettingsData.profileTitles.firstOrNull { it.id == type.id }
                        ProfileTitleOption(
                            id = type.id,
                            name = type.name,
                            description = copy?.description.orEmpty(),
                            badge = copy?.badge,
                        )
                    }
            } else {
                // Fallback so the screen is never empty if the fetch fails.
                MockSettingsData.profileTitles.filterNot { it.name.contains("gallery", ignoreCase = true) }
            }
            // Role-gate the plan list and hide Gallery in-app for now: Artist sees Basic + Artist
            // Pro, Collector / Art Curious see Basic only. Per-card CTA reflects the user's state.
            val currentPlanId = PlanCatalog.currentPlan(roleStr, isPaid).id
            originalTitleId = current.id.takeIf { it != 0 }
            _state.update {
                it.copy(
                    titles = titles,
                    selectedTitleId = current.id.takeIf { id -> id != 0 } ?: it.selectedTitleId,
                    plans = PlanCatalog.availablePlans(roleStr),
                    role = roleStr,
                    isPaid = isPaid,
                    currentPlanId = currentPlanId,
                    selectedPlanId = currentPlanId,
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
