package com.example.artrinx.feature.auth.domain.repository

import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.auth.data.remote.dto.OtpRequest
import com.example.artrinx.feature.auth.data.remote.dto.OtpVerifyRequest
import com.example.artrinx.feature.auth.data.remote.dto.OtpVerifyResponse
import com.example.artrinx.feature.auth.data.remote.dto.RefreshTokenRequest
import com.example.artrinx.feature.auth.data.remote.dto.WaitlistRequest
import com.example.artrinx.feature.auth.data.remote.dto.WaitlistResponse

interface AuthRepository {
    suspend fun verifyInviteCode(code: String): ApiResult<String>
    suspend fun joinWaitlist(request: WaitlistRequest): ApiResult<WaitlistResponse>
    suspend fun requestOtp(request: OtpRequest): ApiResult<Unit>
    suspend fun verifyOtp(request: OtpVerifyRequest): ApiResult<OtpVerifyResponse>
    suspend fun resendOtp(request: OtpRequest): ApiResult<Unit>
    suspend fun refreshToken(request: RefreshTokenRequest): ApiResult<OtpVerifyResponse>
    suspend fun saveSession(response: OtpVerifyResponse)
    fun getRefreshToken(): String?
    fun isSessionValid(): Boolean
    fun isAccessTokenExpired(): Boolean
    fun isProfileCompleted(): Boolean
    suspend fun saveProfileCompleted(completed: Boolean)
    suspend fun clearSession()
    suspend fun logout()
    /** Delete the current account (§3.10); wipes the local session on success. */
    suspend fun deleteAccount(): ApiResult<Unit>
}
