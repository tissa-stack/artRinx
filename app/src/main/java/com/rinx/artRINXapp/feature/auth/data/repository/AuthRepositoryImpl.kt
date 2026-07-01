package com.rinx.artRINXapp.feature.auth.data.repository

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.auth.data.local.SessionDataSource
import com.rinx.artRINXapp.feature.auth.data.remote.AuthApiService
import com.rinx.artRINXapp.feature.auth.data.remote.dto.ApiErrorResponse
import com.rinx.artRINXapp.feature.auth.data.remote.dto.ApiMessageResponse
import com.rinx.artRINXapp.feature.auth.data.remote.dto.ContactConfirmAddRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.ContactConfirmChangeRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.ContactStartAddRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.ContactStartChangeRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.GoogleSignInRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.NativeAuthErrorResponse
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpVerifyRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpVerifyResponse
import com.rinx.artRINXapp.feature.auth.data.remote.dto.RefreshTokenRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.WaitlistRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.WaitlistResponse
import com.rinx.artRINXapp.feature.auth.domain.model.InviteCodeType
import com.rinx.artRINXapp.feature.auth.domain.model.InviteVerification
import com.rinx.artRINXapp.feature.auth.domain.repository.AuthRepository
import com.rinx.artRINXapp.core.auth.LocalDataCleaner
import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import java.io.IOException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: AuthApiService,
    private val sessionDataSource: SessionDataSource,
    private val localDataCleaner: LocalDataCleaner,
) : AuthRepository {

    private val gson = Gson()

    // ── Invite / Waitlist ────────────────────────────────────────────────────

    override suspend fun verifyInviteCode(code: String): ApiResult<InviteVerification> {
        return try {
            val response = apiService.verifyInvite(code)
            val body = response.body()
            if (response.isSuccessful && body?.success != false) {
                val data = body?.data
                ApiResult.Success(
                    InviteVerification(
                        code = code,
                        codeType = InviteCodeType.fromWire(data?.codeType),
                        remainingInvites = data?.remainingInvites,
                    ),
                )
            } else if (response.isSuccessful) {
                // HTTP 200 but body-level failure (success=false), e.g. invite limit exhausted.
                ApiResult.Error.Validation(body?.message ?: DEFAULT_ERROR)
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

    override suspend fun signInWithGoogle(idToken: String, inviteCode: String?): ApiResult<OtpVerifyResponse> {
        return try {
            val response = apiService.signInWithGoogle(GoogleSignInRequest(idToken, inviteCode))
            if (response.isSuccessful) {
                response.body()?.let { ApiResult.Success(it) }
                    ?: ApiResult.Error.Unknown(RuntimeException("Empty Google sign-in response"))
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

    override suspend fun saveSession(response: OtpVerifyResponse) {
        sessionDataSource.saveSession(response)
        // New sign-in → drop any previous user's in-memory caches (keeps the just-saved session),
        // covering reactive sign-outs that only cleared the session.
        localDataCleaner.clearCaches()
    }

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
        localDataCleaner.clearAll()
    }

    override suspend fun signOutEverywhere() {
        // Best-effort server-side revoke of ALL refresh tokens; always wipe locally afterwards.
        try {
            apiService.signOutEverywhere()
        } catch (_: Exception) {
            // Ignore network/server failure — local wipe below still happens.
        }
        localDataCleaner.clearAll()
    }

    // ── Change email (contact change, §1.8) ───────────────────────────────────

    override fun getEmail(): String? = sessionDataSource.getEmail()

    override suspend fun startChangeEmail(newEmail: String): ApiResult<Unit> {
        return try {
            val response = apiService.contactStartChange(ContactStartChangeRequest(newValue = newEmail))
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                nativeErrorResult(response.code(), response.errorBody()?.string(), response)
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun confirmChangeEmail(newEmail: String, code: String): ApiResult<Unit> {
        return try {
            val response = apiService.contactConfirmChange(
                ContactConfirmChangeRequest(newValue = newEmail, code = code),
            )
            if (response.isSuccessful) {
                adoptSessionIfEnvelope(response.body()?.string())
                sessionDataSource.saveEmail(newEmail)
                ApiResult.Success(Unit)
            } else {
                nativeErrorResult(response.code(), response.errorBody()?.string(), response)
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun startAddEmail(newEmail: String): ApiResult<Unit> {
        return try {
            val response = apiService.contactStartAdd(ContactStartAddRequest(value = newEmail))
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                nativeErrorResult(response.code(), response.errorBody()?.string(), response)
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun confirmAddEmail(newEmail: String, code: String): ApiResult<Unit> {
        return try {
            val response = apiService.contactConfirmAdd(
                ContactConfirmAddRequest(value = newEmail, code = code),
            )
            if (response.isSuccessful) {
                adoptSessionIfEnvelope(response.body()?.string())
                sessionDataSource.saveEmail(newEmail)
                ApiResult.Success(Unit)
            } else {
                nativeErrorResult(response.code(), response.errorBody()?.string(), response)
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    // ── Change phone (contact change, §1.8) ───────────────────────────────────

    override fun getPhone(): String? = sessionDataSource.getPhone()

    override suspend fun startChangePhone(newPhone: String): ApiResult<Unit> {
        return try {
            val response = apiService.contactStartChange(
                ContactStartChangeRequest(kind = "phone", newValue = newPhone),
            )
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                nativeErrorResult(response.code(), response.errorBody()?.string(), response)
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun confirmChangePhone(newPhone: String, code: String): ApiResult<Unit> {
        return try {
            val response = apiService.contactConfirmChange(
                ContactConfirmChangeRequest(kind = "phone", newValue = newPhone, code = code),
            )
            if (response.isSuccessful) {
                adoptSessionIfEnvelope(response.body()?.string())
                sessionDataSource.savePhone(newPhone)
                ApiResult.Success(Unit)
            } else {
                nativeErrorResult(response.code(), response.errorBody()?.string(), response)
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun startAddPhone(newPhone: String): ApiResult<Unit> {
        return try {
            val response = apiService.contactStartAdd(ContactStartAddRequest(kind = "phone", value = newPhone))
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                nativeErrorResult(response.code(), response.errorBody()?.string(), response)
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun confirmAddPhone(newPhone: String, code: String): ApiResult<Unit> {
        return try {
            val response = apiService.contactConfirmAdd(
                ContactConfirmAddRequest(kind = "phone", value = newPhone, code = code),
            )
            if (response.isSuccessful) {
                adoptSessionIfEnvelope(response.body()?.string())
                sessionDataSource.savePhone(newPhone)
                ApiResult.Success(Unit)
            } else {
                nativeErrorResult(response.code(), response.errorBody()?.string(), response)
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    /**
     * Contact confirm endpoints may return a fresh auth envelope (the server revokes other devices'
     * refresh tokens) or an empty success. If an envelope is present, adopt the new token pair so
     * THIS device stays signed in; otherwise leave the session untouched.
     */
    private suspend fun adoptSessionIfEnvelope(raw: String?) {
        if (raw.isNullOrBlank()) return
        runCatching {
            val envelope = gson.fromJson(raw, OtpVerifyResponse::class.java)
            if (envelope != null && envelope.accessToken.isNotBlank() && envelope.refreshToken.isNotBlank()) {
                // Adopt only the rotated token pair; each caller persists the new email/phone
                // separately. Using saveTokens (not saveSession) avoids the envelope's user fields
                // clobbering the persisted profile_completed/role.
                sessionDataSource.saveTokens(envelope)
            }
        }
    }

    override suspend fun deleteAccount(): ApiResult<Unit> {
        return try {
            val response = apiService.deleteMe()
            if (response.isSuccessful) {
                localDataCleaner.clearAll() // sign out + wipe all local data after the server accepts
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

    @VisibleForTesting
    internal fun nativeErrorResult(
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

    @VisibleForTesting
    internal fun parseNativeError(body: String?, fallback: String = DEFAULT_NATIVE_ERROR): String {
        if (body == null) return fallback
        return try {
            val code = gson.fromJson(body, NativeAuthErrorResponse::class.java)?.detail?.code
            codeToMessage(code) ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    @VisibleForTesting
    internal fun codeToMessage(code: String?): String? = when (code) {
        "invalid_otp" -> "The code is invalid or has expired."
        "expired_otp", "otp_expired" -> "Your code has expired. Please request a new one."
        "invalid_invite_code", "invite_invalid" -> "This invite code is not valid."
        "invalid_referral_code", "referral_invalid" -> "This referral code is not valid."
        "invite_or_referral_required" -> "An invite or referral code is required to sign up."
        "invite_required" -> "An invite code is required to create an account. Please sign up with your invite code."
        "account_disabled", "identity_disabled" -> "Your account has been disabled. Please contact support."
        "otp_locked" -> "Too many attempts. Please wait before trying again."
        "rate_limited" -> "Too many attempts. Please wait a moment and try again."
        "user_not_found", "no_account_found" -> "No account found. Try creating an account instead."
        "user_exists", "already_registered" -> "An account already exists. Try signing in instead."
        // Security: the refresh token was invalid or reused — the session is dead; the coordinator
        // wipes it on the 401 from /refresh. Surface a sign-in prompt if a code path reaches here.
        "refresh_invalid", "refresh_reused", "session_expired" ->
            "Your session has expired. Please sign in again."
        "not_allowed_on_this_surface" -> "This action isn't available in the app."
        // Contact change (email/phone)
        "email_required_before_phone_change" -> "Please add an email before changing your phone."
        "contact_in_use", "phone_in_use", "email_in_use" -> "That contact is already in use by another account."
        "invalid_contact" -> "Enter a valid phone number or email."
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
