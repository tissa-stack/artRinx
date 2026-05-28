package com.example.artrinx.feature.auth.domain.usecase

import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.auth.data.remote.dto.WaitlistRequest
import com.example.artrinx.feature.auth.data.remote.dto.WaitlistResponse
import com.example.artrinx.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class JoinWaitlistUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(request: WaitlistRequest): ApiResult<WaitlistResponse> =
        repository.joinWaitlist(request)
}
