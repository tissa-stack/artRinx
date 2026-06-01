package com.example.artrinx

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artrinx.core.navigation.NavRoutes
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.auth.domain.repository.AuthRepository
import com.example.artrinx.feature.auth.domain.usecase.RefreshTokenUseCase
import com.example.artrinx.feature.auth.domain.usecase.SaveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

internal val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val authRepository: AuthRepository,
    private val refreshToken: RefreshTokenUseCase,
    private val saveSession: SaveSessionUseCase,
) : ViewModel() {

    // null = still resolving, String = resolved start destination
    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val hasSeenOnboarding = dataStore.data
                .map { prefs -> prefs[ONBOARDING_COMPLETE_KEY] ?: false }
                .first()

            if (!hasSeenOnboarding) {
                _startDestination.value = NavRoutes.ONBOARDING
                return@launch
            }

            if (!authRepository.isSessionValid()) {
                _startDestination.value = NavRoutes.AUTH
                return@launch
            }

            if (authRepository.isAccessTokenExpired()) {
                val storedRefreshToken = authRepository.getRefreshToken()
                if (storedRefreshToken != null) {
                    when (val result = refreshToken(storedRefreshToken)) {
                        is ApiResult.Success -> {
                            saveSession(result.data)
                            _startDestination.value = if (authRepository.isProfileCompleted())
                                NavRoutes.HOME else NavRoutes.PROFILE_COMPLETION
                        }
                        else -> {
                            authRepository.clearSession()
                            _startDestination.value = NavRoutes.AUTH
                        }
                    }
                } else {
                    authRepository.clearSession()
                    _startDestination.value = NavRoutes.AUTH
                }
            } else {
                _startDestination.value = if (authRepository.isProfileCompleted())
                    NavRoutes.HOME else NavRoutes.PROFILE_COMPLETION
            }
        }
    }
}
