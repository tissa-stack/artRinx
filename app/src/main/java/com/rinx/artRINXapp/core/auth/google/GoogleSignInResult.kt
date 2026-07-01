package com.rinx.artRINXapp.core.auth.google

/**
 * Outcome of a Credential Manager "Continue with Google" attempt.
 *
 * [Success] carries the Google ID token (exchanged with the backend) plus the account's display
 * details, which seed the profile-completion prefill for brand-new users. The remaining states map
 * 1:1 to the user-facing handling in the auth ViewModels.
 */
sealed interface GoogleSignInResult {
    data class Success(
        val idToken: String,
        val displayName: String?,
        val givenName: String?,
        val familyName: String?,
        val photoUrl: String?,
    ) : GoogleSignInResult

    /** User dismissed the account chooser — not an error, surface nothing. */
    data object Cancelled : GoogleSignInResult

    /** No Google account available / nothing to pick on this device. */
    data object NoCredential : GoogleSignInResult

    /** Credential Manager provider (Play services) missing, outdated, or unconfigured. */
    data object PlayServicesUnavailable : GoogleSignInResult

    /** The sign-in attempt failed because the device is offline / GMS hit a network error. */
    data object NetworkError : GoogleSignInResult

    /** Any other failure (parse error, empty token, unexpected credential type, etc.). */
    data class Failure(val cause: Throwable) : GoogleSignInResult
}
