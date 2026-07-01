package com.rinx.artRINXapp.core.ui

/**
 * Central registry of character caps for user-editable text fields. Client-side caps keep input at
 * or below the backend's limits so an over-long string never round-trips into a 422 (see the
 * username-check `string_too_long` case). Only [USERNAME] is a backend-confirmed value; the rest
 * mirror the app's existing caps or are conservative for previously-uncapped fields. The 422 error
 * parser (core/network/ApiErrors.kt) is the safety net if any cap ends up looser than the server.
 */
object TextLimits {
    const val USERNAME = 50            // confirmed: backend rejects > 50
    const val FULL_NAME = 50
    const val DISPLAY_NAME = 50
    const val BIO = 200
    const val CURATION_TITLE = 40
    const val CURATION_DESCRIPTION = 255
    const val ARTWORK_TITLE = 40
    const val ARTWORK_DESCRIPTION = 255
    const val ARTIST_NAME = 50         // guest-artist name (typed in the artist search field)
    const val LOCATION = 100           // country / state / city typed text
    const val SHOP_LINK = 500
    const val TAG = 30                 // per tag (the 10-tag count cap lives in UploadModels)
    const val SEARCH_QUERY = 100
    const val FEEDBACK = 1000
    const val MESSAGE = 1000           // chat composer + send-message sheet
    const val EMAIL = 100
    const val PERSON_NAME = 50         // waitlist first / last name
    const val INVITE_CODE = 16
}
