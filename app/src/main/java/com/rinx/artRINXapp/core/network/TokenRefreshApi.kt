package com.rinx.artRINXapp.core.network

import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpVerifyResponse
import com.rinx.artRINXapp.feature.auth.data.remote.dto.RefreshTokenRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Minimal interface for the token-refresh call, served by a SEPARATE bare OkHttp client (no auth
 * interceptor/authenticator). Keeping it off the main client breaks the DI cycle
 * (mainClient → interceptor/authenticator → coordinator → refreshApi) and avoids interceptor
 * recursion when refreshing.
 */
interface TokenRefreshApi {
    @POST("api/auth/native/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): Response<OtpVerifyResponse>
}
