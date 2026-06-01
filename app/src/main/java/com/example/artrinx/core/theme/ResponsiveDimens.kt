package com.example.artrinx.core.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * All layout-critical dimensions derived from the physical screen size (dp).
 * Fractions are calibrated to the reference 360×800dp phone; coerceIn guards
 * against extreme devices (fold-out tablets, tiny watches, etc.).
 *
 * Provided via [LocalDimens] inside [ArtRinxTheme] so any composable can read
 *   val d = LocalDimens.current
 * instead of writing raw dp literals.
 */
data class ResponsiveDimens(
    val screenWidthDp: Float,
    val screenHeightDp: Float,
) {
    private fun w(fraction: Float, min: Dp, max: Dp): Dp =
        (screenWidthDp * fraction).dp.coerceIn(min, max)

    private fun h(fraction: Float, min: Dp, max: Dp): Dp =
        (screenHeightDp * fraction).dp.coerceIn(min, max)

    // ── Logo ──────────────────────────────────────────────────────────────
    // Width-based so the horizontal logo stays proportional regardless of device height.
    // 360w × 0.156 ≈ 56dp  |  min 48dp so it's never tiny on small phones
    val logoHeight: Dp            = w(0.125f, 38.dp, 56.dp)
    val logoPaddingVertical: Dp   = h(0.030f, 16.dp, 32.dp)
    val logoPaddingHorizontal: Dp = w(0.067f, 16.dp, 32.dp)

    // ── Art Mosaic Grid ───────────────────────────────────────────────────
    // 360w × 0.017 ≈ 6dp gap  |  360w × 0.033 ≈ 12dp corner
    val gridCellGap: Dp        = w(0.028f, 6.dp, 14.dp)
    val gridCornerRadius: Dp   = w(0.033f, 8.dp, 18.dp)
    val gridPaddingHorizontal: Dp = w(0.044f, 12.dp, 24.dp)

    // ── Screen-level padding ──────────────────────────────────────────────
    // 360w × 0.067 ≈ 24dp  |  800h × 0.030 ≈ 24dp
    val screenPaddingHorizontal: Dp = w(0.067f, 16.dp, 32.dp)
    val screenPaddingBottom: Dp     = h(0.030f, 16.dp, 32.dp)

    // ── Page indicator pills ──────────────────────────────────────────────
    // 800h × 0.008 ≈ 6dp height  |  360w × 0.067 ≈ 24dp active
    val pillHeight: Dp         = h(0.008f, 4.dp, 8.dp)
    val pillCornerRadius: Dp   = pillHeight / 2f
    val pillActiveWidth: Dp    = w(0.067f, 18.dp, 36.dp)
    val pillInactiveWidth: Dp  = w(0.050f, 14.dp, 26.dp)
    val pillSpacing: Dp        = w(0.022f, 6.dp, 14.dp)

    // ── Progress-arc button ───────────────────────────────────────────────
    // 360w × 0.178 ≈ 64dp outer  |  360w × 0.144 ≈ 52dp inner
    val buttonOuterSize: Dp    = w(0.178f, 52.dp, 84.dp)
    val buttonInnerSize: Dp    = w(0.144f, 42.dp, 68.dp)
    val buttonArcStroke: Dp    = w(0.007f, 2.dp, 4.dp)
    val buttonIconSize: Dp     = w(0.056f, 16.dp, 28.dp)

    // ── Auth full-width buttons ───────────────────────────────────────────
    // 800h × 0.068 ≈ 54dp  |  standard touch target is 48dp min
    val authButtonHeight: Dp   = h(0.068f, 48.dp, 60.dp)

    // ── Text fields ───────────────────────────────────────────────────────
    // Height matches Material3 default (56dp) on small phones and scales up
    // on larger screens so inputs remain comfortable touch targets.
    val textFieldHeight: Dp    = h(0.075f, 56.dp, 76.dp)

    // ── Typography scale ──────────────────────────────────────────────────
    // Dimensionless multiplier: 1.0 at 360dp reference width. Applied to all
    // sp font sizes in ArtRinxTheme so text grows proportionally on large
    // screens and never looks tiny on tablets or large phones.
    val fontScale: Float       = (screenWidthDp / 360f).coerceIn(0.85f, 1.45f)

    // Home screen dimensions
    val tabPillHeight: Dp          = h(0.055f,  40.dp,  54.dp)   // ~44dp at 800h reference
    val bannerHeight: Dp           = h(0.330f, 230.dp, 380.dp)   // ~264dp — close to reference
    val artCardWidth: Dp           = w(0.620f, 200.dp, 320.dp)   // ~62% of screen — 1 full + 1 peeking
    val artCardHeight: Dp          = w(0.490f, 158.dp, 265.dp)   // portrait ratio matches reference
    val collectionCardWidth: Dp    = w(0.590f, 190.dp, 300.dp)   // ~59% of screen — 1 full + 1 peeking
    val collectionCardHeight: Dp   = w(0.360f, 116.dp, 190.dp)   // image section only (text row below)
    val feedImageHeight: Dp        = w(0.720f, 245.dp, 400.dp)   // ~259dp at 360w — slightly reduced
    val avatarSize: Dp             = w(0.089f,  32.dp,  52.dp)
    val avatarSizeLg: Dp           = w(0.111f,  40.dp,  64.dp)
    val bottomNavHeight: Dp        = h(0.072f,  52.dp,  72.dp)
    val indicatorDotActive: Dp     = w(0.022f,   8.dp,  14.dp)
    val indicatorDotMedium: Dp     = w(0.017f,   6.dp,  10.dp)
    val indicatorDotSmall: Dp      = w(0.011f,   4.dp,   8.dp)
    val cardCornerRadius: Dp       = w(0.033f,  10.dp,  20.dp)
}

/** Default matches the 360×800dp reference phone so previews look correct. */
val LocalDimens = compositionLocalOf { ResponsiveDimens(360f, 800f) }
