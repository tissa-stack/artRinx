package com.example.artrinx.feature.profile.domain.model

data class ProfileType(
    val id: Int,
    val name: String,
)

/** The current logged-in user, used to attribute uploads to "myself". */
data class CurrentUser(
    val id: Int,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
)

data class Medium(
    val id: Int,
    val title: String,
    val pictureUrl: String,
)

data class ProfileDraft(
    val step: Int = 0,
    val groundRulesAccepted: Boolean = false,
    val profileTypeId: Int? = null,
    val fullName: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val age: String = "",
    val country: String = "",
    val state: String = "",
    val city: String = "",
    val mediumIds: Set<Int> = emptySet(),
)

/**
 * The current user's title + plan, for the "Profile title and plan" settings screen.
 * [profileTitle] is the raw `profile_title` from the API; [isPremium] reflects the active
 * subscription (premium only when plan is artist_pro and status is active/trialing); the plan
 * shown otherwise is the free basic plan they start on at onboarding.
 */
data class ProfilePlanSummary(
    val profileTitle: String,
    val isPremium: Boolean,
    /** Formatted next billing/renewal date, or "" when there's nothing to bill (free plan). */
    val nextBillingDate: String,
)

/** The current user's invite code + remaining monthly invites, for the Invite Friends screen. */
data class InviteInfo(
    val code: String?,
    val remainingInvites: Int?,
)

/** A user the current user has blocked (§3.8). */
data class BlockedUser(
    val userId: Int,
    val name: String,
    val role: String,
    val avatarUrl: String?,
)

/** A user who joined via the current user's invite code (§2.3). */
data class InvitedUser(
    val id: String,
    val name: String,
    val handle: String, // "@username"
    val joinedDate: String, // formatted, "" when absent
    val avatarUrl: String?,
)

/** The current user's profile in editable form, used to prefill the Edit Profile screen. */
data class EditableProfile(
    val username: String,
    val fullName: String,
    val displayName: String,
    val bio: String,
    val age: String, // range label (e.g. "18-25"), already mapped from the numeric wire value
    val country: String,
    val state: String,
    val city: String,
    val profilePictureUrl: String?,
    val fullNameEditCount: Int = 0,
)

/**
 * A partial profile update: a non-null field means the user changed it and it should be sent;
 * null means unchanged and is omitted from the multipart request (protects the backend's
 * restricted-field edit limits — username/full name/display name).
 */
data class ProfileUpdate(
    val username: String? = null,
    val fullName: String? = null,
    val displayName: String? = null,
    val bio: String? = null,
    val age: String? = null, // range label; repo converts to numeric via ageToNumeric
    val country: String? = null,
    val state: String? = null,
    val city: String? = null,
    val profileTypeId: Int? = null, // sent as profile_type_id (the profile title/role)
) {
    val hasAnyField: Boolean
        get() = listOf(username, fullName, displayName, bio, age, country, state, city)
            .any { it != null } || profileTypeId != null
}
