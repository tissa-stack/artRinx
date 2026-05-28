package com.example.artrinx.feature.onboarding.domain.usecase

import com.example.artrinx.R
import com.example.artrinx.feature.onboarding.domain.model.CardSize
import com.example.artrinx.feature.onboarding.domain.model.OnboardingPage

class GetOnboardingPagesUseCase {

    operator fun invoke(): List<OnboardingPage> = listOf(
        // ── Page 1 ─────────────────────────────────────────────────────────
        // 2+3 layout, tall portrait at LEFT-BOTTOM (slot 2)
        //
        //  ┌──────────┬──────────┐
        //  │   M      │    LH    │   slot 0 (M),  slot 1 (LH)
        //  ├──────────┤──────────┤
        //  │          │    S     │   slot 2 (LV), slot 3 (S)
        //  │   LV     ├──────────┤
        //  │          │    M     │                slot 4 (M)
        //  └──────────┴──────────┘
        OnboardingPage(
            label = "Welcome",
            headline = "Discover art from around the world.",
            cardSizes = listOf(
                CardSize.SHORT,            // [0] L-top   — art_heaven         (SHORT)
                CardSize.LONG_HORIZONTAL,  // [1] R-top   — street_poster      (LONG)
                CardSize.TALL_PORTRAIT,    // [2] L-bot   — fluid_purple       (TALL)
                CardSize.MEDIUM,           // [3] R-mid   — paint_brushes
                CardSize.MEDIUM,           // [4] R-bot   — cosmic_swirl
            ),
            imageRes = listOf(
                R.drawable.art_heaven,
                R.drawable.art_sample_street_poster,
                R.drawable.art_sample_fluid_purple,
                R.drawable.art_brush_paint,
                R.drawable.art_sample_cosmic_swirl,
            ),
        ),
        // ── Page 2 ─────────────────────────────────────────────────────────
        // 3+2 layout (flipped), tall portrait at RIGHT-BOTTOM (slot 3)
        //
        //  ┌──────────┬──────────┐
        //  │   LH     │    M     │   slot 0 (LH), slot 1 (M)
        //  ├──────────┼──────────┤
        //  │   M      │          │   slot 2 (M)
        //  ├──────────┤   LV     │                slot 3 (LV)
        //  │   S      │          │   slot 4 (S)
        //  └──────────┴──────────┘
        OnboardingPage(
            label = "Explore",
            headline = "Find new art to add to your collection.",
            flipLayout = true,
            cardSizes = listOf(
                CardSize.LONG_HORIZONTAL,  // [0] L-top   — artists_studio (LONG)
                CardSize.SHORT,            // [1] R-top   — typewriter     (SHORT)
                CardSize.MEDIUM,           // [2] L-mid   — neon_corridor
                CardSize.TALL_PORTRAIT,    // [3] R-bot   — pink_glitter   (TALL)
                CardSize.MEDIUM,           // [4] L-bot   — chrysler
            ),
            imageRes = listOf(
                R.drawable.art_sample_artists_studio,
                R.drawable.art_sample_typewriter,
                R.drawable.art_sample_neon_corridor,
                R.drawable.art_sample_pink_glitter,
                R.drawable.art_sample_chrysler_building,
            ),
        ),
        // ── Page 3 ─────────────────────────────────────────────────────────
        // 2+3 layout, tall portrait at LEFT-TOP (slot 0)
        //
        //  ┌──────────┬──────────┐
        //  │          │    M     │   slot 0 (LV), slot 1 (M)
        //  │   LV     ├──────────┤
        //  │          │    S     │                slot 3 (S)
        //  ├──────────┼──────────┤
        //  │   M      │    LH    │   slot 2 (M),  slot 4 (LH)
        //  └──────────┴──────────┘
        OnboardingPage(
            label = "Let's Get Started",
            headline = "Connect with artists and support their journeys!",
            cardSizes = listOf(
                CardSize.TALL_PORTRAIT,    // [0] L-top (TALL)
                CardSize.MEDIUM,           // [1] R-top
                CardSize.MEDIUM,           // [2] L-bot
                CardSize.MEDIUM,           // [3] R-mid (flatter than SHORT → shorter LV)
                CardSize.LONG_HORIZONTAL,  // [4] R-bot
            ),
            imageRes = listOf(
                R.drawable.art_sample_painted_hands,
                R.drawable.art_sample_artist_outdoors,
                R.drawable.art_sample_paint_brushes,
                R.drawable.art_and_artist,
                R.drawable.art_sample_street_artist,
            ),
        ),
    )
}
