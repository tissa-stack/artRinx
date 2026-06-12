package com.rinx.artRINXapp.core.push

import android.content.Context
import android.provider.Settings
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.rinx.artRINXapp.core.di.ApplicationScope
import com.rinx.artRINXapp.core.push.dto.FcmTokenRequest
import com.rinx.artRINXapp.feature.auth.data.local.SessionDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers the device's FCM token with the backend (API §11). Fire-and-forget on the app scope;
 * only runs when a session exists (the endpoint is authed). Call on app launch, on token refresh,
 * and right after a fresh sign-in.
 */
@Singleton
class PushTokenManager @Inject constructor(
    private val pushApiService: PushApiService,
    private val session: SessionDataSource,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) {
    /** Stable per-install device identifier (Settings.Secure.ANDROID_ID). */
    @Suppress("HardwareIds")
    private val deviceId: String? by lazy {
        runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()
    }

    /** Fetch the current FCM token and register it (no-op if signed out). */
    fun registerCurrentToken() {
        if (!session.isSessionValid()) return
        scope.launch(Dispatchers.IO) {
            val token = try {
                Tasks.await(FirebaseMessaging.getInstance().token)
            } catch (_: Exception) {
                null
            }
            if (!token.isNullOrBlank()) post(token)
        }
    }

    /** Register a token delivered by [RinxMessagingService.onNewToken]. */
    fun onTokenRefreshed(token: String) {
        if (token.isBlank() || !session.isSessionValid()) return
        scope.launch(Dispatchers.IO) { post(token) }
    }

    /**
     * Invalidate this device's FCM token on logout so it stops being a valid push target. Fire-and-
     * forget; deliberately NOT gated on a valid session (logout clears it first). The backend has no
     * delete endpoint yet, so this relies on FCM reporting the old token as unregistered when the
     * server next tries to deliver — add a server DELETE call here once that endpoint exists.
     */
    fun deleteToken() {
        scope.launch(Dispatchers.IO) {
            runCatching { Tasks.await(FirebaseMessaging.getInstance().deleteToken()) }
        }
    }

    private suspend fun post(token: String) {
        try {
            pushApiService.registerToken(FcmTokenRequest(token = token, deviceId = deviceId))
        } catch (_: Exception) {
            // Best-effort; the token re-registers on next launch / refresh.
        }
    }
}
