package com.rinx.artRINXapp.core.network

import com.rinx.artRINXapp.core.auth.SessionEventBus
import com.rinx.artRINXapp.feature.auth.data.local.SessionDataSource
import com.rinx.artRINXapp.feature.auth.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single-flight access-token refresh (API §1.5). Concurrent callers coalesce onto one round-trip:
 * the [Mutex] serializes them and a short recency cache lets followers reuse a just-refreshed token
 * instead of POSTing `/refresh` again — otherwise the backend's reuse-detection would invalidate the
 * whole token family and sign the user out.
 *
 * Wipes the session only when `/refresh` returns 401 (refresh_invalid / refresh_reused). Network
 * errors return `false` without wiping, so a transient outage doesn't sign the user out.
 */
@Singleton
class TokenRefreshCoordinator @Inject constructor(
    private val session: SessionDataSource,
    private val refreshApi: TokenRefreshApi,
    private val sessionEventBus: SessionEventBus,
) {
    private val mutex = Mutex()

    @Volatile
    private var lastAttemptAtMs = 0L

    @Volatile
    private var lastSuccess = false

    /** @return true if a usable (freshly refreshed or still-valid) access token is available. */
    suspend fun refresh(): Boolean = mutex.withLock {
        val now = System.currentTimeMillis()
        // Coalesce a burst: reuse the most recent attempt's outcome (success OR failure) instead of
        // POSTing /refresh again. This collapses the foreground thundering-herd (every screen calls
        // this on resume once the 15-min token expires) to ONE round-trip, and — crucially — never
        // re-sends a just-attempted refresh token. Re-sending it after a slow/timed-out attempt
        // trips the backend's reuse-detection, which invalidates the whole token family and signs
        // the user out (the "infinite loading + 403 Not authenticated" after idle).
        // Cache a SUCCESS for ~30s (handout §Auth recency cache) so followers reuse the fresh token
        // and we never re-send the just-rotated refresh token. A FAILURE is cached only briefly so a
        // transient network error can retry soon rather than blocking auth for 30s.
        val window = if (lastSuccess) SUCCESS_WINDOW_MS else FAILURE_WINDOW_MS
        if (now - lastAttemptAtMs < window) {
            return@withLock lastSuccess
        }

        val refreshToken = session.getRefreshToken()
        if (refreshToken.isNullOrBlank()) {
            lastAttemptAtMs = now
            lastSuccess = false
            return@withLock false
        }

        val result = try {
            val response = refreshApi.refresh(RefreshTokenRequest(refreshToken))
            val body = response.body()
            when {
                response.isSuccessful && body != null -> {
                    // Persist ONLY the rotated tokens — never the envelope's user fields. A refresh
                    // envelope can omit/false-default user.profile_exists (or omit `user`), and
                    // saveSession would then flip the persisted profile_completed true→false, which
                    // bounced signed-in users to the profile-setup wizard on the next cold start.
                    session.saveTokens(body)
                    true
                }
                response.code() == 401 -> {
                    // refresh_invalid / refresh_reused → the session is dead. This is the SINGLE
                    // authoritative sign-out point: wipe the session and signal the UI to route to
                    // login. Network/timeout errors fall to the catch below and do NOT signal, so a
                    // transient outage never forces an unwanted logout.
                    session.clearSession()
                    sessionEventBus.signalSessionExpired()
                    false
                }
                else -> false
            }
        } catch (e: Exception) {
            // Network/timeout — keep the session; a later attempt (after the window) can retry.
            false
        }
        lastAttemptAtMs = System.currentTimeMillis()
        lastSuccess = result
        result
    }

    private companion object {
        /** Recency cache for a SUCCESSFUL refresh (handout §Auth ~30s). */
        const val SUCCESS_WINDOW_MS = 30_000L
        /** Short coalesce window for a FAILED attempt so transient errors retry quickly. */
        const val FAILURE_WINDOW_MS = 5_000L
    }
}
