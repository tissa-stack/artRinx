package com.example.artrinx.feature.auth.domain.usecase

import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.auth.data.remote.dto.OtpVerifyResponse
import com.example.artrinx.feature.auth.data.remote.dto.RefreshTokenRequest
import com.example.artrinx.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class RefreshTokenUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(refreshToken: String): ApiResult<OtpVerifyResponse> =
        repository.refreshToken(RefreshTokenRequest(refreshToken))
}
