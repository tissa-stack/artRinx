package com.rinx.artRINXapp.core.network

import com.rinx.artRINXapp.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Dispatcher
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

    // Hard ceiling on total wall-clock for a main-client call, INCLUDING RetryInterceptor's retries +
    // backoff, so a flaky/cold backend can never stack the 30s per-attempt timeout into a multi-minute hang.
    private const val CALL_TIMEOUT_SECONDS = 45L

    // Raise OkHttp's default per-host cap (5) so the ~5–7 concurrent calls fired on app resume aren't
    // queued behind each other while the backend is warming up.
    private const val MAX_REQUESTS_PER_HOST = 10

    // The refresh runs inside a blocking preflight on every authed call, so keep it short — a stuck
    // /refresh must fail fast rather than freeze the UI for the full 30s.
    private const val REFRESH_TIMEOUT_SECONDS = 12L

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            // Full bodies only in debug builds — request/response bodies and the refresh client carry
            // OTP codes, access/refresh tokens and the Bearer header, which must never reach release
            // logcat. Also redact the auth header regardless of level as defense-in-depth.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }

    // ── Main client: auth-token preflight interceptor + 401 refresh authenticator ──────────

    @Provides
    @Singleton
    fun provideOkHttpClient(
        logging: HttpLoggingInterceptor,
        retryInterceptor: RetryInterceptor,
        appVersionInterceptor: AppVersionInterceptor,
        authTokenInterceptor: AuthTokenInterceptor,
        sessionInvalidationInterceptor: SessionInvalidationInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient =
        OkHttpClient.Builder()
            // Outermost: retry transient timeouts/connection failures on GETs before anything else runs.
            .addInterceptor(retryInterceptor)
            .addInterceptor(appVersionInterceptor)
            .addInterceptor(authTokenInterceptor)
            // After authToken (so the token is already attached) — recovers the backend's 403
            // "Not authenticated" via a refresh+retry, complementing the 401-only authenticator.
            .addInterceptor(sessionInvalidationInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(logging)
            .dispatcher(Dispatcher().apply { maxRequestsPerHost = MAX_REQUESTS_PER_HOST })
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
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
        appVersionInterceptor: AppVersionInterceptor,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(appVersionInterceptor)
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
