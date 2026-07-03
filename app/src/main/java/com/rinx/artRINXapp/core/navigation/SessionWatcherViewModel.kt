package com.rinx.artRINXapp.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.auth.LocalDataCleaner
import com.rinx.artRINXapp.core.auth.SessionEventBus
import com.rinx.artRINXapp.core.network.TokenRefreshCoordinator
import com.rinx.artRINXapp.feature.auth.data.local.SessionDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Bridges the app-scoped [SessionEventBus] to the navigation layer. [AppNavGraph] collects
 * [forceLogout] and, on each signal, calls [onForcedLogout] (wipes any remaining local data) and
 * navigates to the login screen. Also re-validates the session on app foreground so a device that was
 * signed out elsewhere (Settings → Sign out of all devices) discovers it promptly.
 */
@HiltViewModel
class SessionWatcherViewModel @Inject constructor(
    sessionEventBus: SessionEventBus,
    private val localDataCleaner: LocalDataCleaner,
    private val coordinator: TokenRefreshCoordinator,
    private val sessionDataSource: SessionDataSource,
) : ViewModel() {

    val forceLogout: SharedFlow<Unit> = sessionEventBus.forceLogout

    /**
     * Live session validity for the push/deep-link gate in [AppNavGraph]. The composition's start
     * destination is fixed for its lifetime, so it can't detect a mid-session forced logout (token
     * expired / refresh reused); the gate reads this at tap time instead. False once [SessionDataSource.clearSession] has run.
     */
    fun isLoggedIn(): Boolean = sessionDataSource.isSessionValid()

    /** Full local wipe (caches + offline queue + FCM token); the session tokens are already cleared. */
    fun onForcedLogout() {
        viewModelScope.launch { localDataCleaner.clearAll() }
    }

    /**
     * On app foreground, proactively refresh the token (handout §"refresh on app foreground"). If our
     * refresh token was revoked by a `sign-out-all` on another device, `/refresh` returns 401 and the
     * coordinator wipes the session + emits [forceLogout] (collected above) → routed to auth. A success
     * just rotates the token (no logout), and a network error returns false WITHOUT signalling, so
     * offline-foreground never wrongly logs out. Only runs when signed in.
     */
    fun onAppForegrounded() {
        if (sessionDataSource.getRefreshToken().isNullOrBlank()) return
        viewModelScope.launch { coordinator.refresh() }
    }
}
