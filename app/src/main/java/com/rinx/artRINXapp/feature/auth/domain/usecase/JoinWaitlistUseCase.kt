package com.rinx.artRINXapp.feature.auth.domain.usecase

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.auth.data.remote.dto.WaitlistRequest
import com.rinx.artRINXapp.feature.auth.data.remote.dto.WaitlistResponse
import com.rinx.artRINXapp.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class JoinWaitlistUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(request: WaitlistRequest): ApiResult<WaitlistResponse> =
        repository.joinWaitlist(request)
}
