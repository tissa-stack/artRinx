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
    private var lastRefreshAtMs = 0L

    /** @return true if a usable (freshly refreshed or still-valid) access token is available. */
    suspend fun refresh(): Boolean = mutex.withLock {
        // Coalesce: if another caller just refreshed and the token is still valid, reuse it.
        if (System.currentTimeMillis() - lastRefreshAtMs < RECENCY_WINDOW_MS &&
            !session.isAccessTokenExpired()
        ) {
            return@withLock true
        }

        val refreshToken = session.getRefreshToken() ?: return@withLock false

        try {
            val response = refreshApi.refresh(RefreshTokenRequest(refreshToken))
            val body = response.body()
            when {
                response.isSuccessful && body != null -> {
                    session.saveSession(body)
                    lastRefreshAtMs = System.currentTimeMillis()
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
            // Network/other error — keep the session; the caller can retry later.
            false
        }
    }

    private companion object {
        const val RECENCY_WINDOW_MS = 5_000L
    }
}
