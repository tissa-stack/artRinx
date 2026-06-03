package com.example.artrinx.core.network

import com.example.artrinx.feature.auth.data.local.SessionDataSource
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recovers from a 401 on an authenticated request (API §13.6): refresh once via the single-flight
 * coordinator and retry the original request with the new bearer token. A data-endpoint 401 after a
 * successful refresh is the token-rotation race, not a real invalidation — so we retry rather than
 * sign out. Only `/refresh` itself failing wipes the session (handled in the coordinator).
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val session: SessionDataSource,
    private val coordinator: TokenRefreshCoordinator,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val url = response.request.url.encodedPath
        // Never try to re-auth the auth/refresh endpoints themselves.
        if (isAuthPath(url)) return null
        // Cap retries: give up if we've already retried once for this request.
        if (responseCount(response) >= 2) return null

        val refreshed = runBlocking { coordinator.refresh() }
        if (!refreshed) return null

        val token = session.getAccessToken() ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private fun isAuthPath(path: String): Boolean =
        path.contains("/api/auth/native") ||
            path.contains("/api/verify-invite") ||
            path.contains("/api/waitlist")
}
