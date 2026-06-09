package com.rinx.artRINXapp.core.tour

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Rect
import com.rinx.artRINXapp.core.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class TourState(val active: Boolean = false, val step: Int = 0)

/**
 * App-scoped state for the first-launch tour. Single source of truth so the tour survives the
 * Home→Search→Home navigation (each screen is recreated, but this isn't). The host overlay reads
 * [state]/[bounds]; screens report their highlightable element bounds via [report].
 */
@Singleton
class TourManager @Inject constructor(
    private val prefs: TourPreferences,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(TourState())
    val state: StateFlow<TourState> = _state.asStateFlow()

    /** Window bounds of each spotlightable element, reported by whichever screen owns it. */
    val bounds = mutableStateMapOf<TourTarget, Rect>()

    @Volatile private var started = false

    /**
     * Re-arms the first-launch tour for a freshly-created account. The completed flag is
     * device-global (not per-user), so without this a second account created on the same device
     * would never see the tour. Called right after a successful profile creation: it clears the
     * persisted flag and the in-process "already started" guard so the tour fires when Home is next
     * reached. Suspends until the flag is persisted, so the subsequent Home → [startIfFirstTime]
     * reliably reads the reset value.
     */
    suspend fun prepareForNewUser() {
        started = false
        _state.value = TourState()
        prefs.reset()
    }

    /** Starts the tour the first time Home is reached, unless it was already completed. */
    fun startIfFirstTime() {
        if (started) return
        started = true
        scope.launch {
            if (!prefs.isCompleted()) _state.value = TourState(active = true, step = 0)
        }
    }

    fun next() {
        val n = _state.value.step + 1
        if (n >= TourStep.ordered.size) finish() else _state.value = _state.value.copy(step = n)
    }

    fun back() {
        _state.value = _state.value.copy(step = (_state.value.step - 1).coerceAtLeast(0))
    }

    fun skip() = finish()

    fun report(target: TourTarget, rect: Rect) {
        bounds[target] = rect
    }

    private fun finish() {
        _state.value = _state.value.copy(active = false)
        scope.launch { prefs.markCompleted() }
    }
}
