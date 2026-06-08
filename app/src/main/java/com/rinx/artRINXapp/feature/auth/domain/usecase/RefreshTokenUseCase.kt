package com.rinx.artRINXapp.feature.auth.domain.usecase

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpVerifyResponse
import com.rinx.artRINXapp.feature.auth.data.remote.dto.RefreshTokenRequest
import com.rinx.artRINXapp.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class RefreshTokenUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(refreshToken: String): ApiResult<OtpVerifyResponse> =
        repository.refreshToken(RefreshTokenRequest(refreshToken))
}
