package com.rinx.artRINXapp.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.auth.LocalDataCleaner
import com.rinx.artRINXapp.core.auth.SessionEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Bridges the app-scoped [SessionEventBus] to the navigation layer. [AppNavGraph] collects
 * [forceLogout] and, on each signal, calls [onForcedLogout] (wipes any remaining local data) and
 * navigates to the login screen.
 */
@HiltViewModel
class SessionWatcherViewModel @Inject constructor(
    sessionEventBus: SessionEventBus,
    private val localDataCleaner: LocalDataCleaner,
) : ViewModel() {

    val forceLogout: SharedFlow<Unit> = sessionEventBus.forceLogout

    /** Full local wipe (caches + offline queue + FCM token); the session tokens are already cleared. */
    fun onForcedLogout() {
        viewModelScope.launch { localDataCleaner.clearAll() }
    }
}
