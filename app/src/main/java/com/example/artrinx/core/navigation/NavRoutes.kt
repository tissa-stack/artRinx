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
    const val ART_DETAIL = "art_detail/{postId}"
    const val CURATION_DETAIL = "curation_detail/{curationId}"

    fun signup(inviteCode: String) = "signup?inviteCode=${Uri.encode(inviteCode)}"
    fun artDetail(postId: String) = "art_detail/${Uri.encode(postId)}"
    fun curationDetail(curationId: String) = "curation_detail/${Uri.encode(curationId)}"
}

data class OtpArgs(
    val mode: String,
    val contactType: String,
    val contactValue: String,
    val inviteCode: String = "",
)
