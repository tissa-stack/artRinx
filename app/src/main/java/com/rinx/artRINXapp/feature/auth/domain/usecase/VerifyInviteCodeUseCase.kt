package com.rinx.artRINXapp.feature.auth.domain.usecase

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.auth.domain.model.InviteVerification
import com.rinx.artRINXapp.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyInviteCodeUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(code: String): ApiResult<InviteVerification> =
        repository.verifyInviteCode(code)
}
