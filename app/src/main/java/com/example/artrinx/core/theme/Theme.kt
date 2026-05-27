package com.example.artrinx.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCE9F8),
    onPrimaryContainer = Color(0xFF004E73),
    secondary = InactiveButton,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightPrimaryText,
    surface = LightSurface,
    onSurface = LightSecondaryText,
    surfaceVariant = LightCardSurface,
    onSurfaceVariant = LightInlineText,
    error = ErrorLight,
    onError = Color.White,
    outline = LightBorder,
    outlineVariant = LightFieldBackground,
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF003A56),
    onPrimaryContainer = Color(0xFF9DD5F5),
    secondary = InactiveButton,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = DarkPrimaryText,
    surface = DarkSurface,
    onSurface = DarkSecondaryText,
    surfaceVariant = DarkCardSurface,
    onSurfaceVariant = DarkInlineText,
    error = ErrorDark,
    onError = Color.White,
    outline = DarkBorder,
    outlineVariant = DarkFieldBackground,
)

@Composable
fun ArtRinxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val configuration = LocalConfiguration.current
    val dimens = ResponsiveDimens(
        screenWidthDp = configuration.screenWidthDp.toFloat(),
        screenHeightDp = configuration.screenHeightDp.toFloat(),
    )

    CompositionLocalProvider(LocalDimens provides dimens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ArtRinxTypography,
            content = content,
        )
    }
}
