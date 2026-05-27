package com.example.artrinx.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.artrinx.R

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val poppinsFont = GoogleFont("Poppins")

val PoppinsFamily = androidx.compose.ui.text.font.FontFamily(
    Font(googleFont = poppinsFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = poppinsFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = poppinsFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = poppinsFont, fontProvider = googleFontProvider, weight = FontWeight.Bold),
)

val ArtRinxTypography = Typography(
    // Display Large — splash / hero
    displayLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = 0.sp,
    ),
    // H1 — main headings
    headlineLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    // H2 — section titles
    headlineMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    ),
    // Body Large — main content
    bodyLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    // Body Medium — secondary content
    bodyMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    // Label — buttons / inputs
    labelLarge = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    // Caption — metadata
    labelSmall = TextStyle(
        fontFamily = PoppinsFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
)
