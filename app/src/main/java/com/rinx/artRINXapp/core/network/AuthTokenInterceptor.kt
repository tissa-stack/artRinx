package com.rinx.artRINXapp.core.network

import com.rinx.artRINXapp.feature.auth.data.local.SessionDataSource
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthTokenInterceptor @Inject constructor(
    private val sessionDataSource: SessionDataSource,
    private val coordinator: TokenRefreshCoordinator,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Auth/onboarding endpoints are unauthenticated — never attach a token or preflight-refresh
        // (refreshing here would be pointless and could recurse).
        if (isAuthPath(request.url.encodedPath)) {
            return chain.proceed(request)
        }

        // Preflight (API §13.6): if the access token is within 60s of expiry and we have a refresh
        // token, refresh proactively (single-flight) before sending, so the request goes out fresh.
        if (sessionDataSource.getRefreshToken() != null &&
            sessionDataSource.isAccessTokenExpiringSoon(EXPIRY_THRESHOLD_MS)
        ) {
            runBlocking { coordinator.refresh() }
        }

        val token = sessionDataSource.getAccessToken()
        val authed = if (token != null) {
            request.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            request
        }
        return chain.proceed(authed)
    }

    private fun isAuthPath(path: String): Boolean =
        // Login/onboarding native-auth endpoints are unauthenticated, EXCEPT the contact
        // add/change endpoints (§1.8), which are Bearer-authed and must carry the token.
        (path.contains("/api/auth/native") && !path.contains("/api/auth/native/contact")) ||
            path.contains("/api/verify-invite") ||
            path.contains("/api/waitlist")

    private companion object {
        const val EXPIRY_THRESHOLD_MS = 60_000L
    }
}
