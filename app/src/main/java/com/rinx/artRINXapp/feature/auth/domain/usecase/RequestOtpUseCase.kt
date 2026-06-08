package com.rinx.artRINXapp.feature.auth.domain.usecase

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpRequest
import com.rinx.artRINXapp.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class RequestOtpUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(request: OtpRequest): ApiResult<Unit> =
        repository.requestOtp(request)
}
