package com.rinx.artRINXapp.feature.auth.presentation.waitlist

data class CountryCode(
    val flag: String,
    val code: String,
    val dialCode: String,
    /** Full country name — shown/searched in the searchable picker; empty for compact-only uses. */
    val name: String = "",
)

/**
 * Bundled fallback dial codes. The waitlist replaces these with the full master catalog
 * (GET /api/locations/master/countries) at runtime; the other phone screens and any offline state
 * fall back to this short list.
 */
object CountryCodes {
    val all: List<CountryCode> = listOf(
        CountryCode("🇮🇳", "IN", "+91", "India"),
        CountryCode("🇺🇸", "US", "+1", "United States"),
        CountryCode("🇬🇧", "GB", "+44", "United Kingdom"),
        CountryCode("🇦🇪", "AE", "+971", "United Arab Emirates"),
        CountryCode("🇦🇺", "AU", "+61", "Australia"),
        CountryCode("🇨🇦", "CA", "+1", "Canada"),
        CountryCode("🇸🇬", "SG", "+65", "Singapore"),
        CountryCode("🇩🇪", "DE", "+49", "Germany"),
        CountryCode("🇫🇷", "FR", "+33", "France"),
        CountryCode("🇸🇦", "SA", "+966", "Saudi Arabia"),
    )

    // Default to the United States everywhere a phone country isn't explicitly chosen.
    val default: CountryCode = all.first { it.code == "US" }
}
