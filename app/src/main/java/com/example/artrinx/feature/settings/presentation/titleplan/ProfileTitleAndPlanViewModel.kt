package com.example.artrinx.feature.settings.presentation.titleplan

import androidx.lifecycle.ViewModel
import com.example.artrinx.feature.settings.domain.model.MockSettingsData
import com.example.artrinx.feature.settings.domain.model.PlanOption
import com.example.artrinx.feature.settings.domain.model.ProfileTitleOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class ProfileTitleAndPlanUiState(
    val title: ProfileTitleOption,
    val plan: PlanOption,
    val nextBillingDate: String,
)

@HiltViewModel
class ProfileTitleAndPlanViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(
        ProfileTitleAndPlanUiState(
            title = MockSettingsData.profileTitles.first { it.id == MockSettingsData.currentProfileTitleId },
            plan = MockSettingsData.plans.first { it.id == MockSettingsData.currentPlanId },
            nextBillingDate = MockSettingsData.nextBillingDate,
        ),
    )
    val state: StateFlow<ProfileTitleAndPlanUiState> = _state.asStateFlow()
}