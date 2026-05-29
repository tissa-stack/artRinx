package com.example.artrinx.feature.auth.domain.usecase

import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.auth.data.remote.dto.OtpVerifyRequest
import com.example.artrinx.feature.auth.data.remote.dto.OtpVerifyResponse
import com.example.artrinx.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(request: OtpVerifyRequest): ApiResult<OtpVerifyResponse> =
        repository.verifyOtp(request)
}
