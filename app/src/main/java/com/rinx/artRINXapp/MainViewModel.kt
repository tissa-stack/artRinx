package com.rinx.artRINXapp

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.navigation.NavRoutes
import com.rinx.artRINXapp.core.network.TokenRefreshCoordinator
import com.rinx.artRINXapp.feature.auth.domain.repository.AuthRepository
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
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
    private val tokenRefreshCoordinator: TokenRefreshCoordinator,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    // null = still resolving, String = resolved start destination
    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val dest = resolveStartDestination()
            _startDestination.value = dest
            // Authenticated → warm the current-user id cache so "is this my profile?" is instant the
            // first time the user taps an avatar (the profile route reads ProfileRepository's cache).
            if (dest == NavRoutes.HOME || dest == NavRoutes.PROFILE_COMPLETION) {
                runCatching { profileRepository.getMyProfile() }
            }
        }
    }

    private suspend fun resolveStartDestination(): String {
        // 1. Onboarding must be completed before anything else.
        val onboardingComplete = dataStore.data
            .map { it[ONBOARDING_COMPLETE_KEY] ?: false }
            .first()
        if (!onboardingComplete) return NavRoutes.ONBOARDING

        // 2. No stored refresh token → no session → start at auth.
        val refresh = authRepository.getRefreshToken()
        if (refresh.isNullOrBlank()) return NavRoutes.AUTH

        // 3. Have a session. If the access token has expired, refresh it through the single-flight
        //    coordinator (the same path used mid-session). The coordinator wipes the session on a
        //    hard refresh failure; if we can't get a valid token, fall back to auth.
        if (authRepository.isAccessTokenExpired()) {
            if (!tokenRefreshCoordinator.refresh()) return NavRoutes.AUTH
        }

        // 4. Valid session → route by profile completion.
        return if (authRepository.isProfileCompleted()) NavRoutes.HOME
        else NavRoutes.PROFILE_COMPLETION
    }
}
