package com.rinx.artRINXapp.core.navigation

import android.net.Uri

object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val AUTH = "auth"
    const val WAITLIST = "waitlist"
    const val LOGIN = "login"
    const val LOGIN_ENTRY = "login_entry?contactType={contactType}"
    const val SIGNUP = "signup?inviteCode={inviteCode}"
    const val OTP = "otp?mode={mode}&contactType={contactType}&contactValue={contactValue}&inviteCode={inviteCode}"
    const val PROFILE_COMPLETION = "profile_completion"
    /** Informational plans screen shown once, right after the first-launch app tutorial. */
    const val POST_TUTORIAL_PLANS = "post_tutorial_plans"
    const val HOME          = "home"
    const val SEARCH        = "search"
    const val CREATE        = "create"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE       = "profile"

    // Settings flow
    const val SETTINGS               = "settings"
    const val EDIT_PROFILE           = "edit_profile"
    const val CHANGE_EMAIL           = "change_email"
    const val CHANGE_PHONE           = "change_phone"
    const val PROFILE_TITLE_PLAN     = "profile_title_plan"
    const val PROFILE_TITLE_PLAN_EDIT = "profile_title_plan_edit?step={step}"
    const val BLOCKED_ACCOUNTS       = "blocked_accounts"
    const val BLOCKED_ARTWORKS       = "blocked_artworks"
    const val INVITE_FRIENDS         = "invite_friends"
    const val PHONE_PERMISSIONS      = "phone_permissions"

    fun profileTitlePlanEdit(step: Int = 0) = "profile_title_plan_edit?step=$step"

    // Messages / chat
    const val CHAT        = "chat/{userId}?source={source}"
    const val NEW_MESSAGE = "new_message"
    const val CHAT_MENU   = "chat_menu/{userId}"

    fun chat(userId: String, source: String = NOTIFICATIONS) =
        "chat/${Uri.encode(userId)}?source=${Uri.encode(source)}"
    fun chatMenu(userId: String) = "chat_menu/${Uri.encode(userId)}"

    // Upload / create flows
    const val NEW_ART             = "new_art/{imageUri}"
    const val ART_PREVIEW         = "art_preview"
    const val ARTIST_SEARCH       = "artist_search"
    const val ADD_TAGS            = "add_tags"
    const val NEW_CURATION        = "new_curation"
    const val ADD_ART_TO_CURATION = "add_art_to_curation"

    /** Sentinel imageUri for opening New Art in EDIT mode (no new image is picked). */
    const val NEW_ART_EDIT_SENTINEL = "edit"

    fun newArt(imageUri: String) = "new_art/${Uri.encode(imageUri)}"
    /** Open New Art in edit mode (image comes from the artwork being edited, via EditTargetStore). */
    fun newArtForEdit() = "new_art/$NEW_ART_EDIT_SENTINEL"

    // source arg carries the originating bottom-tab so detail screens keep the correct tab highlighted.
    const val ART_DETAIL      = "art_detail/{postId}?source={source}"
    const val CURATION_DETAIL = "curation_detail/{curationId}?source={source}"

    fun signup(inviteCode: String) = "signup?inviteCode=${Uri.encode(inviteCode)}"

    /** Login entry screen locked to a contact type — "EMAIL" or "PHONE" (ContactType.name). */
    fun loginEntry(contactType: String) = "login_entry?contactType=$contactType"

    fun artDetail(postId: String, source: String = HOME) =
        "art_detail/${Uri.encode(postId)}?source=${Uri.encode(source)}"

    fun curationDetail(curationId: String, source: String = HOME) =
        "curation_detail/${Uri.encode(curationId)}?source=${Uri.encode(source)}"

    // Another user's profile. source carries the originating tab for back/highlight behavior.
    const val USER_PROFILE = "user_profile/{userId}?source={source}"

    fun userProfile(userId: String, source: String = HOME) =
        "user_profile/${Uri.encode(userId)}?source=${Uri.encode(source)}"

    // "Art by <artist>" — credited-artist arts. artistId is blank when the artist has no RINX profile.
    const val ART_BY_ARTIST = "art_by_artist/{artistName}?artistId={artistId}&source={source}"

    fun artByArtist(artistName: String, artistId: Int?, source: String = HOME) =
        "art_by_artist/${Uri.encode(artistName)}" +
            "?artistId=${artistId?.toString().orEmpty()}&source=${Uri.encode(source)}"

    // Current user's followers / following list. tab = "followers" | "following".
    const val FOLLOW_LIST = "follow_list?tab={tab}"

    fun followList(tab: String) = "follow_list?tab=${Uri.encode(tab)}"
}

data class OtpArgs(
    val mode: String,
    val contactType: String,
    val contactValue: String,
    val inviteCode: String = "",
)
