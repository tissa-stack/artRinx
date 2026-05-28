package com.example.artrinx.feature.auth.data.repository

import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.auth.data.remote.AuthApiService
import com.example.artrinx.feature.auth.data.remote.dto.ApiErrorResponse
import com.example.artrinx.feature.auth.data.remote.dto.WaitlistRequest
import com.example.artrinx.feature.auth.data.remote.dto.WaitlistResponse
import com.example.artrinx.feature.auth.domain.repository.AuthRepository
import com.google.gson.Gson
import java.io.IOException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val apiService: AuthApiService,
) : AuthRepository {

    private val gson = Gson()

    override suspend fun verifyInviteCode(code: String): ApiResult<String> {
        return try {
            val response = apiService.verifyInvite(code)
            if (response.isSuccessful) {
                ApiResult.Success(response.body() ?: "")
            } else {
                val rawError = response.errorBody()?.string()
                when (response.code()) {
                    422 -> ApiResult.Error.Validation(parseValidationMessage(rawError))
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

    private fun parseValidationMessage(body: String?, fallback: String = DEFAULT_ERROR): String {
        if (body == null) return fallback
        return try {
            gson.fromJson(body, ApiErrorResponse::class.java)
                .detail.firstOrNull()?.msg ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    companion object {
        private const val DEFAULT_ERROR = "Invalid invitation code"
        private const val DEFAULT_WAITLIST_ERROR = "Failed to join waitlist"
    }
}
