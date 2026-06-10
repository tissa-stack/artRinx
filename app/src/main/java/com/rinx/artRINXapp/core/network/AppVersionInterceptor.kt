package com.rinx.artRINXapp.core.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adds `X-App-Version` to every REST request (handout §20). Backend uses it for legacy-traffic
 * gating and crash tagging. Mirrors the same header the WebSocket upgrade sends.
 */
@Singleton
class AppVersionInterceptor @Inject constructor(
    @ApplicationContext private val context: Context,
) : Interceptor {

    private val version: String by lazy {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: FALLBACK
        } catch (_: Exception) {
            FALLBACK
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("X-App-Version", version)
            .build()
        return chain.proceed(request)
    }

    private companion object {
        const val FALLBACK = "1.0"
    }
}
