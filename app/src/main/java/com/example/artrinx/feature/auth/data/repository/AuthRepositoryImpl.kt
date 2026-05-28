package com.example.artrinx.feature.auth.data.repository

import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.auth.data.remote.AuthApiService
import com.example.artrinx.feature.auth.data.remote.dto.ApiErrorResponse
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

    private fun parseValidationMessage(body: String?): String {
        if (body == null) return DEFAULT_ERROR
        return try {
            gson.fromJson(body, ApiErrorResponse::class.java)
                .detail.firstOrNull()?.msg ?: DEFAULT_ERROR
        } catch (_: Exception) {
            DEFAULT_ERROR
        }
    }

    companion object {
        private const val DEFAULT_ERROR = "Invalid invitation code"
    }
}
