package com.rinx.artRINXapp.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retries transient network failures on idempotent **GET**s so a one-off slow/cold backend response (e.g.
 * an ALB/Fargate cold start) self-heals instead of surfacing as an error. Conservative by design:
 * - only GETs are retried — never POST/PUT/DELETE (no risk of duplicate mutations);
 * - only on network **exceptions**, never on a 5xx response body (no hammering a struggling backend);
 * - a slow [SocketTimeoutException] is retried just once (the wait is expensive), while fast connection
 *   failures ([SocketException]/[java.net.ConnectException]) get a couple of cheap retries;
 * - the main client's `callTimeout` bounds the total wall-clock across all attempts.
 *
 * Installed on the main client only (not the refresh/upload/ws clients).
 */
@Singleton
class RetryInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // Only retry safe/idempotent reads; everything else passes through untouched.
        if (!request.method.equals("GET", ignoreCase = true)) {
            return chain.proceed(request)
        }

        var attempt = 0
        while (true) {
            try {
                return chain.proceed(request)
            } catch (e: IOException) {
                val maxRetries = when (e) {
                    is SocketTimeoutException -> TIMEOUT_RETRIES
                    is SocketException -> CONNECTION_RETRIES // covers ConnectException too
                    else -> 0 // e.g. UnknownHostException (offline/DNS) — surface immediately
                }
                if (attempt >= maxRetries) throw e
                attempt++
                try {
                    Thread.sleep(attempt * BACKOFF_MS)
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw e
                }
            }
        }
    }

    private companion object {
        const val TIMEOUT_RETRIES = 1
        const val CONNECTION_RETRIES = 2
        const val BACKOFF_MS = 300L
    }
}
