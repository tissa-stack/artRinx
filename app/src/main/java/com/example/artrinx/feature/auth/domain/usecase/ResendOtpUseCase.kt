package com.example.artrinx.feature.auth.domain.usecase

import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.auth.data.remote.dto.OtpRequest
import com.example.artrinx.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class ResendOtpUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(request: OtpRequest): ApiResult<Unit> =
        repository.resendOtp(request)
}
