package com.example.artrinx.feature.auth.domain.usecase

import com.example.artrinx.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class ClearSessionUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke() = repository.clearSession()
}
