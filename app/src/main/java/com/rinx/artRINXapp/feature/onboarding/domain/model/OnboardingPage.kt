package com.rinx.artRINXapp.feature.onboarding.domain.model

/**
 * Logical size of a tile inside the mosaic. Heights are derived from the live
 * column width at render-time so the layout stays pixel-perfect on any screen.
 *
 *  - [MEDIUM]           : standard landscape  — used twice per page
 *  - [LONG_HORIZONTAL]  : a taller / squarer landscape ("long horizontal")
 *  - [SHORT]            : a flatter, wider landscape
 *  - [TALL_PORTRAIT]    : the lone vertical tile; its exact height is computed
 *                         so that both columns of the grid end at the same y.
 */
enum class CardSize {
    MEDIUM,
    LONG_HORIZONTAL,
    SHORT,
    TALL_PORTRAIT,
}

/**
 * One onboarding page.
 *
 * The mosaic always renders 5 image slots in a 2-column grid.
 *
 *  flipLayout == false  →  left=[0,2], right=[1,3,4]
 *  flipLayout == true   →  left=[0,2,4], right=[1,3]
 *
 * @param cardSizes The 5 [CardSize] values, one per slot 0..4. Exactly one
 *                  slot must be [CardSize.TALL_PORTRAIT].
 * @param landscapeHeightScale Scales every non-tall tile height before the
 *        tall portrait is solved. Values below 1 shrink the grid and shorten
 *        the tall tile (useful when the tall column has fewer slots).
 */
data class OnboardingPage(
    val label: String,
    val headline: String,
    val imageRes: List<Int>,
    val flipLayout: Boolean = false,
    val cardSizes: List<CardSize>,
    val landscapeHeightScale: Float = 1f,
)
