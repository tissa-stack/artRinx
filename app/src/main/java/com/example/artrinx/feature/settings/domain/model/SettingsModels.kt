package com.example.artrinx.feature.settings.domain.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

// ── Edit profile ──────────────────────────────────────────────────────────────

@Immutable
data class EditProfileData(
    val username: String,
    val fullName: String,
    val bio: String,
    val shopLink: String,
    val displayName: String,
    val gender: String,
    val age: String,
    val country: String,
    val state: String,
    val city: String,
)

// ── Profile title & plan ────────────────────────────────────────────────────

@Immutable
data class ProfileTitleOption(
    val id: Int,
    val name: String,
    val description: String,
    /** Italic footnote shown when selected, e.g. "Artist", "Gallery", "title-free". Null = none. */
    val badge: String? = null,
)

@Immutable
data class PlanOption(
    val id: String,
    val name: String,
    val price: String,            // "Free" or "$4.99/ month"
    val features: List<String>,
)

// ── Privacy / blocked accounts ────────────────────────────────────────────────

@Immutable
data class BlockedAccount(
    val id: String,
    val name: String,
    val role: String,
    @param:DrawableRes val avatarRes: Int? = null,
)

// ── Invite friends ────────────────────────────────────────────────────────────

@Immutable
data class Invitee(
    val id: String,
    val name: String,
    val handle: String,
    val date: String,
    @param:DrawableRes val avatarRes: Int? = null,
)

// ── Mock data ─────────────────────────────────────────────────────────────────

object MockSettingsData {

    val profileTitles = listOf(
        ProfileTitleOption(
            id = 1, name = "Artist",
            description = "Promote your work, create connections and be inspired.",
            badge = "\"Artist\"",
        ),
        ProfileTitleOption(
            id = 2, name = "Collector",
            description = "Connect with new artists, grow and refine your collection.",
        ),
        ProfileTitleOption(
            id = 3, name = "Art Curious",
            description = "View art, share art and be inspired, no strings attached!",
            badge = "title-free",
        ),
        ProfileTitleOption(
            id = 4, name = "Gallery - Enterprise",
            description = "Attract collectors, exhibit art, and promote artists.",
            badge = "\"Gallery\"",
        ),
    )

    val basicPlan = PlanOption(
        id = "basic", name = "BASIC", price = "Free",
        features = listOf(
            "Upload 10 artworks",
            "Send messages (5 invites/ month)",
            "Browse and like art and curations",
            "Follow and share profiles",
        ),
    )

    val premiumPlan = PlanOption(
        id = "premium", name = "PREMIUM", price = "$4.99/ month",
        features = listOf(
            "Upload unlimited artworks",
            "Send messages (25 invites/ month)",
            "Browse and like art & curations",
            "Follow and share profiles",
            "Add links to profile and art",
        ),
    )

    val plans = listOf(basicPlan, premiumPlan)

    /** Current preferences shown on the Profile-title-and-plan summary screen. */
    const val currentProfileTitleId = 4          // Gallery - Enterprise
    const val currentPlanId = "premium"
    const val nextBillingDate = "15th May 2025"

    val editProfile = EditProfileData(
        username    = "Vinay Pabba",
        fullName    = "Vinay Pabba",
        bio         = "Art Lover",
        shopLink    = "www.artsmart.com",
        displayName = "Vinay Pabba",
        gender      = "Male",
        age         = "18-25",
        country     = "India",
        state       = "Telangana",
        city        = "Khammam",
    )

    val blockedAccounts = List(6) { i ->
        BlockedAccount(id = "b$i", name = "Hayley AG", role = "Art Curios", avatarRes = null)
    }

    const val inviteCode = "A34-055-2J6"
    const val invitesPerMonth = 25

    val invitees = listOf(
        Invitee("i1", "Hayley AG",    "hayleyag",  "12/02/25"),
        Invitee("i2", "Shelby Cumin", "scumin2",   "11/01/25"),
        Invitee("i3", "Joe Shmoe",    "shmoeawy",  "8/06/25"),
        Invitee("i4", "Diana Tipler", "tiptee",    "7/04/25"),
    )
}