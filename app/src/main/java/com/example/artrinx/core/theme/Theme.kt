package com.example.artrinx.core.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

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

/**
 * Indication that draws nothing — used to remove the tap ripple/highlight from `clickable`
 * surfaces app-wide. Paired with a null [LocalRippleConfiguration] for Material3 components
 * (Button, IconButton, Card, etc.), this removes every click indication across the app.
 */
private object NoIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        object : Modifier.Node() {}

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = -1
}

@OptIn(ExperimentalMaterial3Api::class)
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

    val typography = remember(dimens.fontScale) {
        buildResponsiveTypography(dimens.fontScale)
    }

    CompositionLocalProvider(
        LocalDimens provides dimens,
        LocalIndication provides NoIndication,
        LocalRippleConfiguration provides null,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

private fun buildResponsiveTypography(scale: Float): Typography {
    fun TextStyle.scaled() = copy(
        fontSize = (fontSize.value * scale).sp,
        lineHeight = (lineHeight.value * scale).sp,
    )
    return Typography(
        displayLarge   = ArtRinxTypography.displayLarge.scaled(),
        headlineLarge  = ArtRinxTypography.headlineLarge.scaled(),
        headlineMedium = ArtRinxTypography.headlineMedium.scaled(),
        headlineSmall  = ArtRinxTypography.headlineSmall.scaled(),
        bodyLarge      = ArtRinxTypography.bodyLarge.scaled(),
        bodyMedium     = ArtRinxTypography.bodyMedium.scaled(),
        bodySmall      = ArtRinxTypography.bodySmall.scaled(),
        labelLarge     = ArtRinxTypography.labelLarge.scaled(),
        labelMedium    = ArtRinxTypography.labelMedium.scaled(),
        labelSmall     = ArtRinxTypography.labelSmall.scaled(),
    )
}
