package com.rinx.artRINXapp.feature.auth.domain.usecase

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpVerifyRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpVerifyResponse
import com.rinx.artRINXapp.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(request: OtpVerifyRequest): ApiResult<OtpVerifyResponse> =
        repository.verifyOtp(request)
}
