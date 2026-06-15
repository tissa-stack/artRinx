package com.rinx.artRINXapp.feature.profile.domain.model

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
    val dob: String = "", // ISO YYYY-MM-DD
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

/**
 * Invite counters from /profile. The 2026-06 revamp splits two distinct caps:
 * [remainingInvites] = peer-share codes you give friends (5/5/25, Invite Friends screen);
 * [remainingChatInvites] = new chats you can start this month (15/25/25, chat surfaces).
 */
data class InviteInfo(
    val code: String?,
    val remainingInvites: Int?,
    val remainingChatInvites: Int? = null,
    /** Monthly cap for new chats (message requests), when the server provides the nested counter. */
    val chatInvitesMonthlyCap: Int? = null,
    /** Monthly cap for peer-share invites (e.g. 5) — used to show "used/cap" on the invite screen. */
    val invitesMonthlyCap: Int? = null,
)

/**
 * Upload-eligibility snapshot for the Create screen's role×plan gate (handout Upload).
 * [maxUploads] is the server value when present, else the tier fallback.
 */
data class UploadQuota(
    val isPaid: Boolean,
    val role: String,
    val artworkCount: Int,
    val maxUploads: Int,
) {
    val isGallery: Boolean get() = role.contains("gallery", ignoreCase = true)
    val limitReached: Boolean get() = artworkCount >= maxUploads
}

/** Another user's public profile, for the Other-Profile screen. */
data class PublicProfile(
    val userId: Int,
    val handle: String,
    val displayName: String,
    val role: String,
    val bio: String,
    val website: String,
    val avatarUrl: String?,
    val artCount: Int,
    val curationCount: Int,
    val followerCount: Int,
    val followingCount: Int,
    val isFollowing: Boolean,
    val iBlocked: Boolean,
    val theyBlocked: Boolean,
    val canMessage: Boolean,
    val blockReason: String? = null,
    val chatroomId: String?,
)

/** A follower or followed user, for the Followers/Following list. */
data class FollowUser(
    val userId: Int,
    val name: String,
    val handle: String, // "@username"
    val avatarUrl: String?,
    val isFollowing: Boolean,
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
    val dob: String, // ISO YYYY-MM-DD (empty when the user has no DOB on record)
    val country: String,
    val state: String,
    val city: String,
    val profilePictureUrl: String?,
    val fullNameEditCount: Int = 0,
    val marketingSmsConsent: Boolean = false,
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
    val dob: String? = null, // ISO YYYY-MM-DD; sent as the `dob` multipart field
    val country: String? = null,
    val state: String? = null,
    val city: String? = null,
    val profileTypeId: Int? = null, // sent as profile_type_id (the profile title/role)
    val marketingSmsConsent: Boolean? = null, // sent as marketing_sms_consent
) {
    val hasAnyField: Boolean
        get() = listOf(username, fullName, displayName, bio, dob, country, state, city)
            .any { it != null } || profileTypeId != null || marketingSmsConsent != null
}
