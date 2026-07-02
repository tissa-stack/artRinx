package com.rinx.artRINXapp.feature.onboarding.presentation

import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

/**
 * Onboarding motion — spring specs mapped 1:1 from the iOS onboarding
 * (SwiftUI `response` / `dampingFraction`), matching iOS exactly.
 *
 *  - SwiftUI dampingFraction → Compose dampingRatio
 *  - Compose stiffness ≈ (2π / response)²
 *
 * | element            | iOS response / damping | dampingRatio | stiffness |
 * |--------------------|------------------------|--------------|-----------|
 * | text slide-in      | 0.5 / 0.8              | 0.8          | 160       |
 * | progress bar scale | 0.3 / 0.6              | 0.6          | 440       |
 * | page advance       | 0.6 / 0.8              | 0.8          | 110       |
 */
object OnboardingAnim {

    // Text (title + subtitle) slide-up + fade-in.
    fun textFloatSpec() = spring<Float>(dampingRatio = 0.8f, stiffness = 160f)

    fun textOffsetSpec() = spring<IntOffset>(dampingRatio = 0.8f, stiffness = 160f)

    // Progress-bar active-pill scale + colour.
    fun pillFloatSpec() = spring<Float>(dampingRatio = 0.6f, stiffness = 440f)

    fun pillColorSpec() = spring<Color>(dampingRatio = 0.6f, stiffness = 440f)

    // Page advance — also drives the image-collage tile morph.
    fun pageFloatSpec() = spring<Float>(dampingRatio = 0.8f, stiffness = 110f)

    fun pageDpSpec() = spring<Dp>(dampingRatio = 0.8f, stiffness = 110f)
}
