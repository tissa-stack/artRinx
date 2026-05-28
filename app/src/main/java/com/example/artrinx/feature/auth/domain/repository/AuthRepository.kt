package com.example.artrinx.feature.auth.domain.repository

import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.auth.data.remote.dto.WaitlistRequest
import com.example.artrinx.feature.auth.data.remote.dto.WaitlistResponse

interface AuthRepository {
    suspend fun verifyInviteCode(code: String): ApiResult<String>
    suspend fun joinWaitlist(request: WaitlistRequest): ApiResult<WaitlistResponse>
}
