package com.example.artrinx.feature.auth.data.remote

import com.example.artrinx.feature.auth.data.remote.dto.OtpRequest
import com.example.artrinx.feature.auth.data.remote.dto.OtpVerifyRequest
import com.example.artrinx.feature.auth.data.remote.dto.OtpVerifyResponse
import com.example.artrinx.feature.auth.data.remote.dto.RefreshTokenRequest
import com.example.artrinx.feature.auth.data.remote.dto.WaitlistRequest
import com.example.artrinx.feature.auth.data.remote.dto.WaitlistResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
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
}
