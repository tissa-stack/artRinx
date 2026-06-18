package com.rinx.artRINXapp.feature.auth.domain

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Carries the just-authenticated Google account's name + photo from the sign-in moment to the
 * profile-completion wizard, for brand-new users only.
 *
 * Read-once and in-memory by design: it is set on the Google new-user branch, [consume]d once when
 * the wizard ViewModel initialises, and never persisted. Anything the user actually commits lives in
 * the ProfileDraftDataSource, so this never needs to survive process death — and being ephemeral, it
 * can never leak Google data into an unrelated email/OTP signup. Cleared on logout via LocalDataCleaner.
 */
@Singleton
class GooglePrefillHolder @Inject constructor() {

    data class Data(
        val fullName: String?,
        val givenName: String?,
        val familyName: String?,
        val photoUrl: String?,
    )

    @Volatile
    private var pending: Data? = null

    fun set(data: Data) {
        pending = data
    }

    /** Returns the pending data and clears it, so it is delivered exactly once. */
    fun consume(): Data? {
        val current = pending
        pending = null
        return current
    }

    fun clear() {
        pending = null
    }

    companion object {
        /** Cache filename for a downloaded Google avatar (single fixed name → each prefill overwrites). */
        const val AVATAR_CACHE_FILENAME = "google_avatar_prefill.jpg"
    }
}
