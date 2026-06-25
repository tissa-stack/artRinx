package com.rinx.artRINXapp.core.network

import com.rinx.artRINXapp.feature.auth.data.local.SessionDataSource
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recovers from the backend's **403 `{"detail":"Not authenticated"}`** on an authenticated request.
 *
 * The backend returns 403 (not 401) for missing/expired auth, so OkHttp's [TokenAuthenticator] —
 * which only fires on 401 — never gets a chance. This interceptor extends the same single-flight
 * refresh-and-retry to that 403 case: it refreshes once via the coordinator and retries with the
 * fresh token.
 *
 * It NEVER signs the user out on its own. The only authoritative sign-out is the coordinator wiping
 * the session when `/refresh` returns 401 (which emits the force-logout signal). So a transient
 * failure here just surfaces the 403 once and recovers on a later attempt.
 *
 * Crucially, it keys on the response **body** `{"detail":"Not authenticated"}`, so action-denied 403s
 * (mutual block / invite limit / blocked-user / not-a-participant — API §, PR #66/#68) are untouched.
 */
@Singleton
class SessionInvalidationInterceptor @Inject constructor(
    private val session: SessionDataSource,
    private val coordinator: TokenRefreshCoordinator,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (isAuthPath(request.url.encodedPath)) return chain.proceed(request)

        val response = chain.proceed(request)
        if (response.code != 403) return response

        // peekBody copies without consuming the stream the downstream parser needs.
        val body = runCatching { response.peekBody(PEEK_BYTES).string() }.getOrNull().orEmpty()
        if (!body.contains(NOT_AUTHENTICATED, ignoreCase = true)) return response

        // Treat like a 401: refresh once. If it fails authoritatively, the coordinator already wiped
        // the session and signalled force-logout — we just return the 403.
        val refreshed = runBlocking { coordinator.refresh() }
        val token = session.getAccessToken()
        if (!refreshed || token == null) return response

        response.close()
        val retried = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(retried)
    }

    // Mirror AuthTokenInterceptor: never touch the unauthenticated auth/onboarding endpoints, but DO
    // cover the Bearer-authed natives (contact add/change, sign-out-all) so a stale-token retry works.
    private fun isAuthPath(path: String): Boolean =
        (
            path.contains("/api/auth/native") &&
                !path.contains("/api/auth/native/contact") &&
                !path.contains("/api/auth/native/sign-out-all")
            ) ||
            path.contains("/api/verify-invite") ||
            path.contains("/api/waitlist")

    private companion object {
        const val NOT_AUTHENTICATED = "Not authenticated"
        const val PEEK_BYTES = 512L
    }
}
