package com.rinx.artRINXapp.core.tour

import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
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
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    val state: StateFlow<TourState> = manager.state
    val bounds get() = manager.bounds

    fun startIfFirstTime() = manager.startIfFirstTime()
    fun restart() = manager.restart()
    fun next() = manager.next()
    fun back() = manager.back()
    fun report(target: TourTarget, rect: Rect) = manager.report(target, rect)

    /** Dismiss the post-tour Plans finale (used when it's skipped for a non-artist). */
    fun markPlansShown() = manager.markPlansShown()

    /**
     * True when the current user is an Artist, so the post-tutorial plans finale should show.
     * Uses the authoritative profile role (profile_type_name), matching PostTutorialPlansViewModel:
     * seed from the warm SWR cache, fall back to a network fetch if it's cold.
     */
    suspend fun isArtistUser(): Boolean {
        val cached = profileRepository.cachedProfileData()?.role
        val role = if (!cached.isNullOrBlank()) {
            cached
        } else {
            (profileRepository.getProfileData() as? ApiResult.Success)?.data?.role.orEmpty()
        }
        return role.contains("artist", ignoreCase = true)
    }
}
