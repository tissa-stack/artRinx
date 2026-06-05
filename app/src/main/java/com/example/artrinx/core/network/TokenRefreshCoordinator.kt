package com.example.artrinx.core.network

import com.example.artrinx.feature.auth.data.local.SessionDataSource
import com.example.artrinx.feature.auth.data.remote.dto.RefreshTokenRequest
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
        if (now - lastAttemptAtMs < ATTEMPT_WINDOW_MS) {
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
                    session.saveSession(body)
                    true
                }
                response.code() == 401 -> {
                    // refresh_invalid / refresh_reused → the session is dead.
                    session.clearSession()
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
        /** Coalesce window: a burst of refreshes within this collapses to one round-trip. */
        const val ATTEMPT_WINDOW_MS = 5_000L
    }
}
