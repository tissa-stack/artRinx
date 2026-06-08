package com.rinx.artRINXapp.feature.onboarding.presentation

import com.rinx.artRINXapp.feature.onboarding.domain.model.OnboardingPage

data class OnboardingUiState(
    val pages: List<OnboardingPage> = emptyList(),
    val currentPage: Int = 0,
    val isComplete: Boolean = false,
)
