package com.rinx.artRINXapp.feature.auth.domain.repository

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpVerifyRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpVerifyResponse
import com.rinx.artRINXapp.feature.auth.data.remote.dto.RefreshTokenRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.WaitlistRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.WaitlistResponse

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

    /** The user's current email (cached from the auth envelope), or null if unknown. */
    fun getEmail(): String?

    /** Begin an email change — OTP is sent to [newEmail] (§1.8). */
    suspend fun startChangeEmail(newEmail: String): ApiResult<Unit>

    /** Verify the OTP and apply the email change (§1.8); adopts the fresh token pair if returned. */
    suspend fun confirmChangeEmail(newEmail: String, code: String): ApiResult<Unit>

    /** Begin ADDING an email to an account that has none — OTP is sent to [newEmail] (§1.8). */
    suspend fun startAddEmail(newEmail: String): ApiResult<Unit>

    /** Verify the OTP and attach the new email (§1.8); adopts the fresh token pair if returned. */
    suspend fun confirmAddEmail(newEmail: String, code: String): ApiResult<Unit>
}
