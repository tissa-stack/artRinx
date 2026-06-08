package com.rinx.artRINXapp.feature.auth.domain.usecase

import com.rinx.artRINXapp.core.push.PushTokenManager
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpVerifyResponse
import com.rinx.artRINXapp.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

class SaveSessionUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val pushTokenManager: PushTokenManager,
) {
    suspend operator fun invoke(response: OtpVerifyResponse) {
        repository.saveSession(response)
        // Now that a session exists, bind this device's FCM token to the user (API §11).
        pushTokenManager.registerCurrentToken()
    }
}
