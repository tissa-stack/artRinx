package com.example.artrinx.core.navigation

import android.net.Uri

object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val AUTH = "auth"
    const val WAITLIST = "waitlist"
    const val LOGIN = "login"
    const val SIGNUP = "signup?inviteCode={inviteCode}"
    const val OTP = "otp?mode={mode}&contactType={contactType}&contactValue={contactValue}&inviteCode={inviteCode}"
    const val PROFILE_COMPLETION = "profile_completion"
    const val HOME = "home"
    const val SEARCH = "search"
    const val CREATE  = "create"
    const val PROFILE = "profile"

    // Upload / create flows
    const val NEW_ART             = "new_art/{imageUri}"
    const val ART_PREVIEW         = "art_preview"
    const val ARTIST_SEARCH       = "artist_search"
    const val ADD_TAGS            = "add_tags"
    const val NEW_CURATION        = "new_curation"
    const val ADD_ART_TO_CURATION = "add_art_to_curation"

    fun newArt(imageUri: String) = "new_art/${Uri.encode(imageUri)}"

    // source arg carries the originating bottom-tab so detail screens keep the correct tab highlighted.
    const val ART_DETAIL      = "art_detail/{postId}?source={source}"
    const val CURATION_DETAIL = "curation_detail/{curationId}?source={source}"

    fun signup(inviteCode: String) = "signup?inviteCode=${Uri.encode(inviteCode)}"

    fun artDetail(postId: String, source: String = HOME) =
        "art_detail/${Uri.encode(postId)}?source=${Uri.encode(source)}"

    fun curationDetail(curationId: String, source: String = HOME) =
        "curation_detail/${Uri.encode(curationId)}?source=${Uri.encode(source)}"
}

data class OtpArgs(
    val mode: String,
    val contactType: String,
    val contactValue: String,
    val inviteCode: String = "",
)
