package com.rinx.artRINXapp.feature.auth.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpVerifyResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionDataSource @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "artrinx_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    suspend fun saveSession(response: OtpVerifyResponse) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, response.accessToken)
            .putString(KEY_REFRESH_TOKEN, response.refreshToken)
            .putLong(KEY_ACCESS_EXPIRY, System.currentTimeMillis() + response.accessExpiresIn * 1000L)
            .putLong(KEY_REFRESH_EXPIRY, System.currentTimeMillis() + response.refreshExpiresIn * 1000L)
            .putString(KEY_USER_ROLE, response.user.role)
            .putString(KEY_EMAIL, response.user.email)
            .putString(KEY_PHONE, response.user.phone)
            // Gate Home vs Profile-Completion on whether a profile EXISTS (existing users → Home).
            // profile_completed is computed strictly and is false even for usable profiles.
            .putBoolean(KEY_PROFILE_COMPLETED, response.user.profileExists)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun isAccessTokenExpired(): Boolean {
        val expiry = prefs.getLong(KEY_ACCESS_EXPIRY, 0L)
        return expiry == 0L || System.currentTimeMillis() >= expiry
    }

    /** True when the access token is already expired or will expire within [thresholdMs] (preflight). */
    fun isAccessTokenExpiringSoon(thresholdMs: Long): Boolean {
        val expiry = prefs.getLong(KEY_ACCESS_EXPIRY, 0L)
        return expiry == 0L || System.currentTimeMillis() >= expiry - thresholdMs
    }

    fun isSessionValid(): Boolean = getAccessToken() != null

    fun isProfileCompleted(): Boolean = prefs.getBoolean(KEY_PROFILE_COMPLETED, false)

    /** The role chosen at onboarding: artist | collector | curious | gallery | agent | admin. */
    fun getUserRole(): String? = prefs.getString(KEY_USER_ROLE, null)

    /** The user's current email, cached from the auth envelope (login / email-change confirm). */
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    /** The user's current phone (E.164), cached from the auth envelope; null if signed up via email. */
    fun getPhone(): String? = prefs.getString(KEY_PHONE, null)

    /** Persist the phone after a successful phone-change confirm (covers an empty-body server reply). */
    suspend fun savePhone(phone: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_PHONE, phone).apply()
    }

    /** Persist the email after a successful add/change confirm (covers an empty-body server reply). */
    suspend fun saveEmail(email: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_EMAIL, email).apply()
    }

    /** Keep the cached role in sync after the user changes their profile title. */
    suspend fun saveUserRole(role: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_USER_ROLE, role).apply()
    }

    suspend fun saveProfileCompleted(completed: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(KEY_PROFILE_COMPLETED, completed).apply()
    }

    suspend fun clearSession() = withContext(Dispatchers.IO) {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_ACCESS_EXPIRY)
            .remove(KEY_REFRESH_EXPIRY)
            .remove(KEY_USER_ROLE)
            .remove(KEY_EMAIL)
            .remove(KEY_PHONE)
            .remove(KEY_PROFILE_COMPLETED)
            .apply()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_ACCESS_EXPIRY = "access_token_expiry_epoch"
        const val KEY_REFRESH_EXPIRY = "refresh_token_expiry_epoch"
        const val KEY_USER_ROLE = "user_role"
        const val KEY_EMAIL = "user_email"
        const val KEY_PHONE = "user_phone"
        const val KEY_PROFILE_COMPLETED = "profile_completed"
    }
}
