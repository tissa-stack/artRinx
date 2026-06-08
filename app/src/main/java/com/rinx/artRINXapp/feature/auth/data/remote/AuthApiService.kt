package com.rinx.artRINXapp.feature.auth.data.remote

import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpVerifyRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpVerifyResponse
import com.rinx.artRINXapp.feature.auth.data.remote.dto.RefreshTokenRequest
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
    ): Response<String>

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

    @POST("api/auth/native/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest,
    ): Response<OtpVerifyResponse>

    @POST("api/auth/native/logout")
    suspend fun logout(
        @Body request: RefreshTokenRequest,
    ): Response<ResponseBody>

    /** Schedule deletion of the current account (§3.10). Bearer-authed; soft-deletes after a grace window. */
    @PATCH("api/user/delete-me")
    suspend fun deleteMe(): Response<ResponseBody>
}
