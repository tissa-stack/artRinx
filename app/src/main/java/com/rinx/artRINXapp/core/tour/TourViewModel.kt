package com.rinx.artRINXapp.core.tour

import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Thin delegate over the [TourManager] singleton. Obtained via `hiltViewModel()` in each place
 * that touches the tour (the host overlay, and screens that report element bounds) — every
 * instance shares the same underlying manager.
 */
@HiltViewModel
class TourViewModel @Inject constructor(
    private val manager: TourManager,
) : ViewModel() {
    val state: StateFlow<TourState> = manager.state
    val bounds get() = manager.bounds

    fun startIfFirstTime() = manager.startIfFirstTime()
    fun restart() = manager.restart()
    fun next() = manager.next()
    fun back() = manager.back()
    fun report(target: TourTarget, rect: Rect) = manager.report(target, rect)
}
