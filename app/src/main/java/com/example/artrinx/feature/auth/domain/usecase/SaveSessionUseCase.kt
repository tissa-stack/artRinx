package com.example.artrinx.feature.auth.domain.usecase

import com.example.artrinx.feature.auth.data.remote.dto.OtpVerifyResponse
import com.example.artrinx.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class SaveSessionUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(response: OtpVerifyResponse) = repository.saveSession(response)
}
