package com.example.artrinx.feature.onboarding.domain.model

data class OnboardingPage(
    val label: String,
    val headline: String,
    val imageRes: List<Int>,
    val flipLayout: Boolean = false,
    val aspectRatios: List<Float> = emptyList(),
)
