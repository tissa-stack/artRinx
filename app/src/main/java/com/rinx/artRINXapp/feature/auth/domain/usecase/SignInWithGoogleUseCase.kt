package com.rinx.artRINXapp.feature.auth.domain.usecase

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpVerifyResponse
import com.rinx.artRINXapp.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

/** Exchanges a Google ID token for a native token pair (sign-in or sign-up). */
class SignInWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(idToken: String, inviteCode: String? = null): ApiResult<OtpVerifyResponse> =
        repository.signInWithGoogle(idToken, inviteCode)
}
