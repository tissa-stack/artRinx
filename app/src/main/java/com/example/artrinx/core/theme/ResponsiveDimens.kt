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
}

/** Default matches the 360×800dp reference phone so previews look correct. */
val LocalDimens = compositionLocalOf { ResponsiveDimens(360f, 800f) }
