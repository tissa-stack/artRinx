package com.example.artrinx.feature.onboarding.domain.usecase

import com.example.artrinx.R
import com.example.artrinx.feature.onboarding.domain.model.OnboardingPage

class GetOnboardingPagesUseCase {

    operator fun invoke(): List<OnboardingPage> = listOf(
        OnboardingPage(
            label = "Welcome",
            headline = "Discover art from around the world.",
            // 2+3 layout: left=[0,2], right=[1,3,4]
            // Left has mixed ratios: top landscape, bottom tall portrait
            aspectRatios = listOf(1.5f, 1.35f, 0.63f, 1.35f, 1.35f),
            imageRes = listOf(
                R.drawable.art_heaven,               // [0] LEFT top  (landscape)
                R.drawable.art_sample_street_poster, // [1] RIGHT top (landscape)
                R.drawable.art_sample_fluid_purple,  // [2] LEFT bot  (tall portrait)
                R.drawable.art_sample_paint_brushes, // [3] RIGHT mid (landscape)
                R.drawable.art_sample_cosmic_swirl,  // [4] RIGHT bot (landscape)
            ),
        ),
        OnboardingPage(
            label = "Explore",
            headline = "Find new art to add to your collection.",
            flipLayout = true,
            // 3+2 layout: left=[0,2,4], right=[1,3]
            // Right has mixed ratios: top landscape, bottom tall portrait
            aspectRatios = listOf(1.35f, 1.5f, 1.35f, 0.63f, 1.35f),
            imageRes = listOf(
                R.drawable.art_sample_artists_studio,    // [0] LEFT top  (landscape)
                R.drawable.art_sample_typewriter,        // [1] RIGHT top (landscape)
                R.drawable.art_sample_neon_corridor,     // [2] LEFT mid  (landscape)
                R.drawable.art_sample_pink_glitter,      // [3] RIGHT bot (tall portrait)
                R.drawable.art_sample_chrysler_building, // [4] LEFT bot  (landscape)
            ),
        ),
        OnboardingPage(
            label = "Let's Get Started",
            headline = "Connect with artists and support their journeys!",
            // 2+3 layout: left=[0,2], right=[1,3,4]
            // Left has mixed ratios: top tall portrait, bottom landscape
            aspectRatios = listOf(0.68f, 1.5f, 1.35f, 1.35f, 1.35f),
            imageRes = listOf(
                R.drawable.art_sample_painted_hands,   // [0] LEFT top  (tall portrait)
                R.drawable.art_sample_artist_outdoors, // [1] RIGHT top (landscape)
                R.drawable.art_sample_paint_brushes,   // [2] LEFT bot  (landscape)
                R.drawable.art_and_artist,             // [3] RIGHT mid (landscape)
                R.drawable.art_sample_street_artist,   // [4] RIGHT bot (landscape)
            ),
        ),
    )
}