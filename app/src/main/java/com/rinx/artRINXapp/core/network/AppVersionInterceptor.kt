package com.rinx.artRINXapp.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adds `X-App-Platform` + `X-App-Version` to every REST request (handout §20). Backend uses them
 * for per-platform feature gating (e.g. bidirectional blocking) and crash tagging. Mirrors the same
 * headers the WebSocket upgrade sends — both source them from [AppVersionProvider] so they never drift.
 */
@Singleton
class AppVersionInterceptor @Inject constructor(
    private val appVersionProvider: AppVersionProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("X-App-Platform", appVersionProvider.platform)
            .header("X-App-Version", appVersionProvider.versionHeader)
            .build()
        return chain.proceed(request)
    }
}
