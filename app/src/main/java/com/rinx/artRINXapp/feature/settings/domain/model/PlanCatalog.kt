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

/** Per-card call-to-action on the plan screen (handout §Plan card CTA behavior). */
enum class PlanCardCta { NONE, CURRENT_PLAN, UPGRADE, MANAGED_ON_WEB }

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

    /**
     * Currently unused — Gallery is hidden in-app for now, so every plan surface role-gates via
     * [availablePlans]. Kept so re-enabling the full three-plan paywall later is a one-liner.
     */
    val all: List<PlanOption> = listOf(basic, artistPro, gallery)

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

    /** CTA for a given plan card (handout matrix): current → pill, Artist-Free's Pro → Upgrade,
     *  Gallery → "Managed on artrinx.com", else none. */
    fun ctaFor(planId: String, currentPlanId: String, role: String, isPaid: Boolean): PlanCardCta = when {
        planId == gallery.id -> PlanCardCta.MANAGED_ON_WEB
        planId == currentPlanId -> PlanCardCta.CURRENT_PLAN
        planId == artistPro.id && !isPaid && role.contains("artist", ignoreCase = true) -> PlanCardCta.UPGRADE
        else -> PlanCardCta.NONE
    }
}
