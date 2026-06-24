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

/**
 * [completed] = the first-launch tour is resolved and done (either finished/skipped now, or already
 * completed on a previous launch). It stays false until [startIfFirstTime] resolves, so callers can
 * defer follow-up prompts (e.g. notification permission) until AFTER the tour — without the
 * first-frame race where `!active` is briefly true before the tour starts.
 */
data class TourState(
    val active: Boolean = false,
    val step: Int = 0,
    val completed: Boolean = false,
    /** True from when the first-launch tour finishes until the post-tour Plans screen is dismissed.
     *  Drives the one-time "Plans → notification permission" finale; never set for returning users
     *  or a Settings-triggered re-run. */
    val plansPending: Boolean = false,
)

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

    /** True only while the genuine first-launch tour is running, so the post-tour Plans finale fires
     *  for new users but NOT after a Settings-triggered [restart]. */
    @Volatile private var firstLaunchRun = false

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
        firstLaunchRun = false
        _state.value = TourState()
        prefs.reset()
    }

    /** Starts the tour the first time Home is reached, unless it was already completed. */
    fun startIfFirstTime() {
        if (started) return
        started = true
        scope.launch {
            if (prefs.isCompleted()) {
                _state.value = TourState(active = false, completed = true)
            } else {
                firstLaunchRun = true
                _state.value = TourState(active = true, step = 0)
            }
        }
    }

    fun next() {
        val n = _state.value.step + 1
        if (n >= TourStep.ordered.size) finish() else _state.value = _state.value.copy(step = n)
    }

    fun back() {
        _state.value = _state.value.copy(step = (_state.value.step - 1).coerceAtLeast(0))
    }

    fun report(target: TourTarget, rect: Rect) {
        bounds[target] = rect
    }

    private fun finish() {
        // Show the post-tour Plans finale only after a genuine first-launch tour (not a Settings re-run).
        val showPlans = firstLaunchRun
        firstLaunchRun = false
        _state.value = _state.value.copy(active = false, completed = true, plansPending = showPlans)
        scope.launch { prefs.markCompleted() }
    }

    /** Dismiss the post-tour Plans finale (after the user taps "Continue"). */
    fun markPlansShown() {
        _state.value = _state.value.copy(plansPending = false)
    }

    /** Manually (re)start the tour on demand — e.g. Settings → "App tutorial". */
    fun restart() {
        firstLaunchRun = false
        _state.value = TourState(active = true, step = 0)
    }
}
