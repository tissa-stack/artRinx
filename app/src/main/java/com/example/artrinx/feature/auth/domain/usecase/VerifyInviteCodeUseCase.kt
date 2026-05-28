package com.example.artrinx.feature.auth.domain.usecase

import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyInviteCodeUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(code: String): ApiResult<String> =
        repository.verifyInviteCode(code)
}
