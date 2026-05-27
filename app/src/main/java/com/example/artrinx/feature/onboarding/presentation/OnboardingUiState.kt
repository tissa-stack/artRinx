package com.example.artrinx.feature.onboarding.presentation

import com.example.artrinx.feature.onboarding.domain.model.OnboardingPage

data class OnboardingUiState(
    val pages: List<OnboardingPage> = emptyList(),
    val currentPage: Int = 0,
    val isComplete: Boolean = false,
)
