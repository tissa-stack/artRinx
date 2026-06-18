package com.rinx.artRINXapp.feature.auth.data.remote

import com.rinx.artRINXapp.feature.auth.data.remote.dto.ContactConfirmAddRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.ContactConfirmChangeRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.ContactStartAddRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.ContactStartChangeRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.GoogleSignInRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpVerifyRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpVerifyResponse
import com.rinx.artRINXapp.feature.auth.data.remote.dto.RefreshTokenRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.VerifyInviteResponse
import com.rinx.artRINXapp.feature.auth.data.remote.dto.WaitlistRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.WaitlistResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.PATCH
import retrofit2.http.POST

interface AuthApiService {

    @FormUrlEncoded
    @POST("api/verify-invite")
    suspend fun verifyInvite(
        @Field("invitation_code") invitationCode: String,
    ): Response<VerifyInviteResponse>

    @POST("api/waitlist")
    suspend fun joinWaitlist(
        @Body request: WaitlistRequest,
    ): Response<WaitlistResponse>

    @POST("api/auth/native/request-otp")
    suspend fun requestOtp(
        @Body request: OtpRequest,
    ): Response<ResponseBody>

    @POST("api/auth/native/verify-otp")
    suspend fun verifyOtp(
        @Body request: OtpVerifyRequest,
    ): Response<OtpVerifyResponse>

    @POST("api/auth/native/resend-otp")
    suspend fun resendOtp(
        @Body request: OtpRequest,
    ): Response<ResponseBody>

    /**
     * Continue with Google (mobile): exchange a verified Google ID token for a native token pair.
     * Returns the same envelope as verify-otp; the backend auto-creates the account for new users.
     */
    @POST("api/auth/native/oauth/google")
    suspend fun signInWithGoogle(
        @Body request: GoogleSignInRequest,
    ): Response<OtpVerifyResponse>

    @POST("api/auth/native/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest,
    ): Response<OtpVerifyResponse>

    @POST("api/auth/native/logout")
    suspend fun logout(
        @Body request: RefreshTokenRequest,
    ): Response<ResponseBody>

    /** Revoke ALL refresh tokens for the current user (Settings → Sign out everywhere). Bearer-authed. */
    @POST("api/auth/native/sign-out-everywhere")
    suspend fun signOutEverywhere(): Response<ResponseBody>

    /** Schedule deletion of the current account (§3.10). Bearer-authed; soft-deletes after a grace window. */
    @PATCH("api/user/delete-me")
    suspend fun deleteMe(): Response<ResponseBody>

    /** Begin changing the user's email — OTP is sent to the new address (§1.8). Bearer-authed. */
    @POST("api/auth/native/contact/start-change")
    suspend fun contactStartChange(@Body request: ContactStartChangeRequest): Response<ResponseBody>

    /**
     * Verify the OTP and swap the email (§1.8). Bearer-authed. The body is read as raw [ResponseBody]
     * because the server may return either a fresh auth envelope or an empty success — handled in
     * the repository.
     */
    @POST("api/auth/native/contact/confirm-change")
    suspend fun contactConfirmChange(@Body request: ContactConfirmChangeRequest): Response<ResponseBody>

    /** Begin adding an email to an account that has none (e.g. phone signup) — OTP to the new email (§1.8). */
    @POST("api/auth/native/contact/start-add")
    suspend fun contactStartAdd(@Body request: ContactStartAddRequest): Response<ResponseBody>

    /** Verify the OTP and attach the new email (§1.8). Body read as raw [ResponseBody] (envelope or empty). */
    @POST("api/auth/native/contact/confirm-add")
    suspend fun contactConfirmAdd(@Body request: ContactConfirmAddRequest): Response<ResponseBody>
}
