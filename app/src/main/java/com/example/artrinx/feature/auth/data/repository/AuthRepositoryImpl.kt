package com.example.artrinx.feature.auth.data.repository

import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.auth.data.local.SessionDataSource
import com.example.artrinx.feature.auth.data.remote.AuthApiService
import com.example.artrinx.feature.auth.data.remote.dto.ApiErrorResponse
import com.example.artrinx.feature.auth.data.remote.dto.ApiMessageResponse
import com.example.artrinx.feature.auth.data.remote.dto.NativeAuthErrorResponse
import com.example.artrinx.feature.auth.data.remote.dto.OtpRequest
import com.example.artrinx.feature.auth.data.remote.dto.OtpVerifyRequest
import com.example.artrinx.feature.auth.data.remote.dto.OtpVerifyResponse
import com.example.artrinx.feature.auth.data.remote.dto.RefreshTokenRequest
import com.example.artrinx.feature.auth.data.remote.dto.WaitlistRequest
import com.example.artrinx.feature.auth.data.remote.dto.WaitlistResponse
import com.example.artrinx.feature.auth.domain.repository.AuthRepository
import com.google.gson.Gson
import java.io.IOException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: AuthApiService,
    private val sessionDataSource: SessionDataSource,
) : AuthRepository {

    private val gson = Gson()

    // ── Invite / Waitlist ────────────────────────────────────────────────────

    override suspend fun verifyInviteCode(code: String): ApiResult<String> {
        return try {
            val response = apiService.verifyInvite(code)
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: "")
            } else {
                val rawError = response.errorBody()?.string()
                when (response.code()) {
                    422 -> ApiResult.Error.Validation(parseValidationMessage(rawError))
                    in 400..499 -> ApiResult.Error.Validation(parseApiMessage(rawError, DEFAULT_ERROR))
                    in 500..599 -> ApiResult.Error.Server(response.code())
                    else -> ApiResult.Error.Unknown(RuntimeException("HTTP ${response.code()}"))
                }
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun joinWaitlist(request: WaitlistRequest): ApiResult<WaitlistResponse> {
        return try {
            val response = apiService.joinWaitlist(request)
            if (response.isSuccessful) {
                response.body()?.let { ApiResult.Success(it) }
                    ?: ApiResult.Error.Unknown(RuntimeException("Empty response body"))
            } else {
                val rawError = response.errorBody()?.string()
                when (response.code()) {
                    422 -> ApiResult.Error.Validation(parseValidationMessage(rawError, DEFAULT_WAITLIST_ERROR))
                    in 400..499 -> ApiResult.Error.Validation(parseApiMessage(rawError, DEFAULT_WAITLIST_ERROR))
                    in 500..599 -> ApiResult.Error.Server(response.code())
                    else -> ApiResult.Error.Unknown(RuntimeException("HTTP ${response.code()}"))
                }
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    // ── Native OTP Auth ──────────────────────────────────────────────────────

    override suspend fun requestOtp(request: OtpRequest): ApiResult<Unit> {
        return try {
            val response = apiService.requestOtp(request)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                val rawError = response.errorBody()?.string()
                nativeErrorResult(response.code(), rawError, response)
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun verifyOtp(request: OtpVerifyRequest): ApiResult<OtpVerifyResponse> {
        return try {
            val response = apiService.verifyOtp(request)
            if (response.isSuccessful) {
                response.body()?.let { ApiResult.Success(it) }
                    ?: ApiResult.Error.Unknown(RuntimeException("Empty OTP verify response"))
            } else {
                val rawError = response.errorBody()?.string()
                nativeErrorResult(response.code(), rawError, response)
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun resendOtp(request: OtpRequest): ApiResult<Unit> {
        return try {
            val response = apiService.resendOtp(request)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                val rawError = response.errorBody()?.string()
                nativeErrorResult(response.code(), rawError, response)
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun refreshToken(request: RefreshTokenRequest): ApiResult<OtpVerifyResponse> {
        return try {
            val response = apiService.refreshToken(request)
            if (response.isSuccessful) {
                response.body()?.let { ApiResult.Success(it) }
                    ?: ApiResult.Error.Unknown(RuntimeException("Empty refresh token response"))
            } else {
                val rawError = response.errorBody()?.string()
                nativeErrorResult(response.code(), rawError, response)
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    // ── Session ──────────────────────────────────────────────────────────────

    override suspend fun saveSession(response: OtpVerifyResponse) =
        sessionDataSource.saveSession(response)

    override fun getRefreshToken(): String? = sessionDataSource.getRefreshToken()

    override fun isSessionValid(): Boolean = sessionDataSource.isSessionValid()

    override fun isAccessTokenExpired(): Boolean = sessionDataSource.isAccessTokenExpired()

    override fun isProfileCompleted(): Boolean = sessionDataSource.isProfileCompleted()

    override suspend fun saveProfileCompleted(completed: Boolean) =
        sessionDataSource.saveProfileCompleted(completed)

    override suspend fun clearSession() = sessionDataSource.clearSession()

    override suspend fun logout() {
        // Best-effort server revoke; always wipe the local session regardless of the outcome (§1.6).
        val refreshToken = sessionDataSource.getRefreshToken()
        if (!refreshToken.isNullOrBlank()) {
            try {
                apiService.logout(RefreshTokenRequest(refreshToken))
            } catch (_: Exception) {
                // Ignore network/server failure — local wipe below still happens.
            }
        }
        sessionDataSource.clearSession()
    }

    override suspend fun deleteAccount(): ApiResult<Unit> {
        return try {
            val response = apiService.deleteMe()
            if (response.isSuccessful) {
                sessionDataSource.clearSession() // sign out only after the server accepts
                ApiResult.Success(Unit)
            } else {
                when (response.code()) {
                    in 400..499 -> ApiResult.Error.Validation("Couldn't delete your account. Please try again.")
                    in 500..599 -> ApiResult.Error.Server(response.code())
                    else -> ApiResult.Error.Unknown(RuntimeException("HTTP ${response.code()}"))
                }
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    // ── Error Parsing ────────────────────────────────────────────────────────

    private fun nativeErrorResult(
        code: Int,
        rawError: String?,
        response: retrofit2.Response<*>,
    ): ApiResult.Error {
        val message = parseNativeError(rawError)
        return when (code) {
            400 -> ApiResult.Error.Validation(message)
            403 -> ApiResult.Error.Blocked(message)
            404 -> ApiResult.Error.NotFound(message)
            409 -> ApiResult.Error.Conflict(message)
            422 -> ApiResult.Error.Validation(message)
            429 -> ApiResult.Error.RateLimited(message, parseRetryAfter(response))
            in 500..599 -> ApiResult.Error.Server(code)
            else -> ApiResult.Error.Unknown(RuntimeException("HTTP $code"))
        }
    }

    private fun parseNativeError(body: String?, fallback: String = DEFAULT_NATIVE_ERROR): String {
        if (body == null) return fallback
        return try {
            val code = gson.fromJson(body, NativeAuthErrorResponse::class.java)?.detail?.code
            codeToMessage(code) ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    private fun codeToMessage(code: String?): String? = when (code) {
        "invalid_otp" -> "The code is invalid or has expired."
        "expired_otp" -> "Your code has expired. Please request a new one."
        "invalid_invite_code" -> "This invite code is not valid."
        "invalid_referral_code" -> "This referral code is not valid."
        "account_disabled" -> "Your account has been disabled. Please contact support."
        "otp_locked" -> "Too many attempts. Please wait before trying again."
        "user_not_found" -> "No account found. Try creating an account instead."
        "user_exists" -> "An account already exists. Try signing in instead."
        else -> null
    }

    private fun parseRetryAfter(response: retrofit2.Response<*>): Int =
        response.headers()["Retry-After"]?.toIntOrNull() ?: 60

    private fun parseApiMessage(body: String?, fallback: String): String {
        if (body == null) return fallback
        return try {
            gson.fromJson(body, ApiMessageResponse::class.java)
                ?.message?.takeIf { it.isNotBlank() } ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    private fun parseValidationMessage(body: String?, fallback: String = DEFAULT_ERROR): String {
        if (body == null) return fallback
        return try {
            gson.fromJson(body, ApiErrorResponse::class.java)
                .detail.firstOrNull()?.msg ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    private companion object {
        const val DEFAULT_ERROR = "Invalid invitation code"
        const val DEFAULT_WAITLIST_ERROR = "Failed to join waitlist"
        const val DEFAULT_NATIVE_ERROR = "Something went wrong. Please try again."
    }
}
