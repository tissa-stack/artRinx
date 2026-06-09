package com.rinx.artRINXapp.feature.settings.domain.model

/**
 * Single source of truth for plan copy + limits (2026-06 pricing & limits revamp). Mirrors the iOS
 * `PlanType.swift` enum referenced in the handout. Keep all plan strings here so cards, the signup
 * step-5 screen, and the settings plan screen stay consistent.
 *
 * NOTE: prices are placeholders pending Play Console / backend confirmation (the handout redacts the
 * exact amounts). The Artist Pro intro-trial line is read dynamically from Play Billing, NOT here.
 */
enum class PlanType { BASIC, ARTIST_PRO, GALLERY }

object PlanCatalog {

    val basic = PlanOption(
        id = "basic",
        name = "Basic",
        price = "Free",
        features = listOf(
            "Browse and like art",
            "Follow and share profiles",
            "Upload 10 artworks",
            "Invite 5 friends per month",
            "Start up to 15 new chats per month",
            "Unlimited messages within active chats",
        ),
    )

    val artistPro = PlanOption(
        id = "artist_pro",
        name = "Artist Pro",
        price = "$4.99/mo",
        features = listOf(
            "Browse and like art",
            "Follow and share profiles",
            "Upload 99 artworks",
            "Profile link",
            "Shop art link",
            "Invite 5 friends per month",
            "Start up to 25 new chats per month",
            "Unlimited messages within active chats",
        ),
    )

    val gallery = PlanOption(
        id = "gallery",
        name = "Gallery",
        // Anti-steering: Gallery is web-billed; no in-app price/CTA.
        price = "Managed on artrinx.com",
        features = listOf(
            "Browse and like art",
            "Follow and share profiles",
            "Upload 99 artworks",
            "Profile link",
            "Shop art link",
            "Invite 25 friends per month",
            "Start up to 25 new chats per month",
            "Unlimited messages within active chats",
            "Create events (web only)",
            "City-targeted push notifications for events (web only)",
        ),
    )

    /** Plans shown for a picked signup role (Gallery is never offered in-app). */
    fun availablePlans(role: String): List<PlanOption> = when {
        role.contains("artist", ignoreCase = true) -> listOf(basic, artistPro)
        else -> listOf(basic) // Collector / Art Curious
    }

    /** The plan the user is currently on, given their role + paid state. */
    fun currentPlan(role: String, isPaid: Boolean): PlanOption = when {
        role.contains("gallery", ignoreCase = true) -> gallery
        isPaid -> artistPro
        else -> basic
    }
}
