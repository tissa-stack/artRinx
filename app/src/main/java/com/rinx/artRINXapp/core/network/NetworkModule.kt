package com.rinx.artRINXapp.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://apifargate.rinx.com/"
    private const val TIMEOUT_SECONDS = 30L
    private const val UPLOAD_WRITE_TIMEOUT_SECONDS = 120L

    // The refresh runs inside a blocking preflight on every authed call, so keep it short — a stuck
    // /refresh must fail fast rather than freeze the UI for the full 30s.
    private const val REFRESH_TIMEOUT_SECONDS = 12L

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

    // ── Main client: auth-token preflight interceptor + 401 refresh authenticator ──────────

    @Provides
    @Singleton
    fun provideOkHttpClient(
        logging: HttpLoggingInterceptor,
        authTokenInterceptor: AuthTokenInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authTokenInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(logging)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // ── Bare client for token refresh (no auth interceptor/authenticator) ──────────────────
    // Kept separate to break the DI cycle (main client → interceptor/authenticator → coordinator
    // → refreshApi) and to avoid interceptor recursion while refreshing.

    @Provides
    @Singleton
    @Named("refresh")
    fun provideRefreshOkHttpClient(
        logging: HttpLoggingInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(REFRESH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(REFRESH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(REFRESH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(REFRESH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

    @Provides
    @Singleton
    @Named("refresh")
    fun provideRefreshRetrofit(@Named("refresh") client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideTokenRefreshApi(@Named("refresh") retrofit: Retrofit): TokenRefreshApi =
        retrofit.create(TokenRefreshApi::class.java)

    // ── Bare client for the chat WebSocket (no auth interceptor — the Bearer is set as a header) ─
    // OkHttp does NOT send protocol pings by default, so configure pingInterval here (§12.5). The
    // main client is unsuitable: its auth interceptor/authenticator don't apply to the WS upgrade.

    @Provides
    @Singleton
    @Named("ws")
    fun provideWebSocketOkHttpClient(
        logging: HttpLoggingInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .pingInterval(25, TimeUnit.SECONDS)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    // ── Bare client for the signed-URL PUT (no auth interceptor/authenticator) ──────────────
    // The signed CDN URL IS the credential — attaching our Bearer token would both leak it and
    // trip the preflight refresh. Larger write timeout for big image uploads. Used directly
    // (not via Retrofit) so byte progress can be streamed; see UploadRepositoryImpl.

    @Provides
    @Singleton
    @Named("upload")
    fun provideUploadOkHttpClient(
        logging: HttpLoggingInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(UPLOAD_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
}
