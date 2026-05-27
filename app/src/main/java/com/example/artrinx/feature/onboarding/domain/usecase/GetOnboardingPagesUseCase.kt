package com.example.artrinx.feature.onboarding.domain.usecase

import com.example.artrinx.R
import com.example.artrinx.feature.onboarding.domain.model.OnboardingPage

class GetOnboardingPagesUseCase {

    operator fun invoke(): List<OnboardingPage> = listOf(
        OnboardingPage(
            label = "Welcome",
            headline = "Discover art from around the world.",
            imageRes = listOf(
                // 5 images → 2+3 grid: left=[0,2], right=[1,3,4]
                R.drawable.art_heaven,               // [0] LEFT top
                R.drawable.art_sample_street_poster, // [1] RIGHT top
                R.drawable.art_sample_fluid_purple,  // [2] LEFT bot
                R.drawable.art_sample_paint_brushes, // [3] RIGHT mid
                R.drawable.art_sample_cosmic_swirl,  // [4] RIGHT bot
            ),
        ),
        OnboardingPage(
            label = "Explore",
            headline = "Find new art to add to your collection.",
            imageRes = listOf(
                // 5 images → 2+3 grid: left=[0,2], right=[1,3,4]
                R.drawable.art_sample_artists_studio,  // [0] LEFT top
                R.drawable.art_sample_typewriter,      // [1] RIGHT top
                R.drawable.art_sample_neon_corridor,   // [2] LEFT bot
                R.drawable.art_sample_pink_glitter,    // [3] RIGHT mid
                R.drawable.art_sample_chrysler_building, // [4] RIGHT bot
            ),
        ),
        OnboardingPage(
            label = "Let's Get Started",
            headline = "Connect with artists and support their journeys!",
            imageRes = listOf(
                // 5 images → 2+3 grid: left=[0,2], right=[1,3,4]
                R.drawable.art_sample_painted_hands,   // [0] LEFT top
                R.drawable.art_sample_artist_outdoors, // [1] RIGHT top
                R.drawable.art_sample_street_artist,   // [2] LEFT bot
                R.drawable.art_and_artist,             // [3] RIGHT mid
                R.drawable.art_sample_paint_brushes,   // [4] RIGHT bot
            ),
        ),
    )
}
