package com.example.artrinx.feature.auth.domain.repository

import com.example.artrinx.core.network.ApiResult

interface AuthRepository {
    suspend fun verifyInviteCode(code: String): ApiResult<String>
}
