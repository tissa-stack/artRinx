package com.rinx.artRINXapp.feature.auth.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Body for `POST api/auth/native/oauth/google` — exchanges a verified Google ID token for a token pair.
 * [inviteCode] is required by the backend when the token belongs to a NEW user (account creation);
 * it is omitted (Gson null-skip) for returning users signing in.
 */
data class GoogleSignInRequest(
    @SerializedName("id_token") val idToken: String,
    @SerializedName("invite_code") val inviteCode: String? = null,
)
