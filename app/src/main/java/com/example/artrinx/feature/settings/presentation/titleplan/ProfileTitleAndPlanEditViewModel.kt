package com.example.artrinx.feature.settings.presentation.titleplan

import androidx.lifecycle.ViewModel
import com.example.artrinx.feature.settings.domain.model.MockSettingsData
import com.example.artrinx.feature.settings.domain.model.PlanOption
import com.example.artrinx.feature.settings.domain.model.ProfileTitleOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class TitlePlanEditUiState(
    val step: Int = 0,                       // 0 = title, 1 = plan
    val titles: List<ProfileTitleOption> = emptyList(),
    val plans: List<PlanOption> = emptyList(),
    val selectedTitleId: Int? = null,
    val selectedPlanId: String? = null,
) {
    val canAdvanceTitle: Boolean get() = selectedTitleId != null
    val canSavePlan: Boolean get() = selectedPlanId != null
}

@HiltViewModel
class ProfileTitleAndPlanEditViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(
        TitlePlanEditUiState(
            titles = MockSettingsData.profileTitles,
            plans = MockSettingsData.plans,
            selectedTitleId = MockSettingsData.currentProfileTitleId,
            selectedPlanId = MockSettingsData.currentPlanId,
        ),
    )
    val state: StateFlow<TitlePlanEditUiState> = _state.asStateFlow()

    /** One-time init of the starting step (title vs plan) from nav arg. */
    fun setInitialStep(step: Int) = _state.update {
        if (it.step == 0 && step in 0..1) it.copy(step = step) else it
    }

    fun onTitleSelected(id: Int) = _state.update { it.copy(selectedTitleId = id) }
    fun onPlanSelected(id: String) = _state.update { it.copy(selectedPlanId = id) }

    fun goToPlanStep() = _state.update { it.copy(step = 1) }
    fun goToTitleStep() = _state.update { it.copy(step = 0) }
}