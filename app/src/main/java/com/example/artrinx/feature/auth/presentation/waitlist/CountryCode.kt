package com.example.artrinx.feature.auth.presentation.waitlist

data class CountryCode(
    val flag: String,
    val code: String,
    val dialCode: String,
)

object CountryCodes {
    val all: List<CountryCode> = listOf(
        CountryCode("🇮🇳", "IN", "+91"),
        CountryCode("🇺🇸", "US", "+1"),
        CountryCode("🇬🇧", "GB", "+44"),
        CountryCode("🇦🇪", "AE", "+971"),
        CountryCode("🇦🇺", "AU", "+61"),
        CountryCode("🇨🇦", "CA", "+1"),
        CountryCode("🇸🇬", "SG", "+65"),
        CountryCode("🇩🇪", "DE", "+49"),
        CountryCode("🇫🇷", "FR", "+33"),
        CountryCode("🇸🇦", "SA", "+966"),
    )

    val default: CountryCode = all.first()
}
