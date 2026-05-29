package com.example.artrinx.core.network

import com.example.artrinx.feature.auth.data.local.SessionDataSource
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthTokenInterceptor @Inject constructor(
    private val sessionDataSource: SessionDataSource,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionDataSource.getAccessToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
