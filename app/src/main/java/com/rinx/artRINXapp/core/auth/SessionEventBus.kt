package com.rinx.artRINXapp.core.auth

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped one-shot signal that the session is definitively dead and the user must be returned
 * to the login screen. Emitted from the single authoritative wipe point ([TokenRefreshCoordinator]
 * when `/refresh` returns 401), NEVER on transient/network errors — so observing it can safely force
 * a sign-out without risking unwanted logouts.
 *
 * The navigation layer ([AppNavGraph] via SessionWatcherViewModel) observes [forceLogout] and routes
 * to [com.rinx.artRINXapp.core.navigation.NavRoutes.AUTH] with a cleared back stack.
 */
@Singleton
class SessionEventBus @Inject constructor() {
    // Buffer one + drop-oldest so a burst of concurrent failures collapses to a single logout, and an
    // emit that lands before the collector is ready (e.g. during startup) is not lost.
    private val _forceLogout = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val forceLogout: SharedFlow<Unit> = _forceLogout.asSharedFlow()

    fun signalSessionExpired() {
        _forceLogout.tryEmit(Unit)
    }
}
